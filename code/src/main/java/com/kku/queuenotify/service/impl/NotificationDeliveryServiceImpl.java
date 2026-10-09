package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.NotificationLog;
import com.kku.queuenotify.domain.enums.NotificationChannel;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.NotificationLogRepository;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.NotificationDeliveryService;
import com.kku.queuenotify.service.NotificationStrategy;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** One durable READY claim per order. No automatic retry or event wiring here. */
@Service
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {
  private final QueueRepository queues;
  private final NotificationLogRepository logs;
  private final ObjectProvider<NotificationStrategy> strategies;
  private final String mode;
  private final Clock clock;
  private final TransactionTemplate isolated;

  public NotificationDeliveryServiceImpl(QueueRepository queues, NotificationLogRepository logs,
      ObjectProvider<NotificationStrategy> strategies, @Value("${notification.mode:console}") String mode,
      Clock clock, PlatformTransactionManager transactions) {
    this.queues=queues; this.logs=logs; this.strategies=strategies; this.mode=mode; this.clock=clock;
    isolated=new TransactionTemplate(transactions);
    isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  // Suspend even a caller transaction: provider I/O must hold no database locks.
  @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
  public void deliverReady(Long queueId) {
    if(queueId==null || queueId<=0) return;
    NotificationStrategy strategy=strategies.getIfAvailable();
    if(strategy==null || !(mode.equals("console") || mode.equals("webpush"))) {
      throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Notification strategy is not available");
    }
    Claim claim=isolated.execute(status->claim(queueId));
    if(claim==null) return;
    Result result;
    try {
      int code=strategy.send(claim.subscription(),claim.payload());
      if(mode.equals("console") && code==0) result=new Result("PREVIEW",null,false,"Local preview; no provider request");
      else if(mode.equals("webpush") && code>=200 && code<300) result=new Result("ACCEPTED",code,true,"Push provider accepted the request; device display is unconfirmed");
      else result=new Result("FAILED",code>=100 && code<=599?code:null,false,"Notification provider did not accept the request");
    } catch(RuntimeException ex) {
      // Persist a constant error; never store provider URLs, keys, tokens or exception text.
      result=new Result("FAILED",null,false,"Notification attempt failed");
    }
    Result finalResult=result;
    isolated.executeWithoutResult(status->{
      var log=logs.findById(claim.logId()).orElseThrow(()->new IllegalStateException("Notification claim is missing"));
      if(!"PENDING".equals(log.getDeliveryStatus())) return;
      log.setDeliveryStatus(finalResult.status());log.setHttpStatus(finalResult.httpStatus());
      log.setSuccess(finalResult.success());log.setMessage(finalResult.message());
      log.setSentAt(finalResult.success()?now():null);logs.saveAndFlush(log);
    });
  }

  private Claim claim(Long queueId) {
    var queue=queues.lockById(queueId).orElse(null);
    if(queue==null || queue.getStatus()!=QueueStatus.READY || logs.existsByQueueIdAndEventType(queueId,"READY")) return null;
    var subscription=queue.getOrder().getPushSubscription();
    boolean push=mode.equals("webpush");
    if(push && (subscription==null || !subscription.isActive())) return null;
    PushSubscriptionRequest target=push?new PushSubscriptionRequest(subscription.getEndpoint(),
        new PushSubscriptionRequest.Keys(subscription.getP256dh(),subscription.getAuth())):null;
    NotificationLog log=new NotificationLog();log.setQueue(queue);log.setEventType("READY");
    log.setChannel(push?NotificationChannel.PUSH:NotificationChannel.CONSOLE);
    log.setDeliveryStatus("PENDING");log.setMessage("Notification attempt claimed");log.setSuccess(false);
    log.setAttemptedAt(now());logs.saveAndFlush(log);
    return new Claim(log.getId(),target,PushPayload.ready(queueId,"อาหารของคุณพร้อมรับแล้ว"));
  }

  private LocalDateTime now() { return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC); }
  private record Claim(Long logId,PushSubscriptionRequest subscription,PushPayload payload) {}
  private record Result(String status,Integer httpStatus,boolean success,String message) {}
}
