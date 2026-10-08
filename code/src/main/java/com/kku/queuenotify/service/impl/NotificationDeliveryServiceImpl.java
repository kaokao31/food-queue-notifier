package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.NotificationLog;
import com.kku.queuenotify.domain.enums.*;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;
import com.kku.queuenotify.repository.*;
import com.kku.queuenotify.service.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {
  private final QueueRepository queues;
  private final NotificationLogRepository logs;
  private final NotificationStrategy strategy;
  private final TransactionTemplate tx;

  public NotificationDeliveryServiceImpl(
      QueueRepository q,
      NotificationLogRepository l,
      NotificationStrategy s,
      org.springframework.transaction.PlatformTransactionManager tm) {
    queues = q;
    logs = l;
    strategy = s;
    tx = new TransactionTemplate(tm);
    tx.setPropagationBehavior(
        org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  private record Job(
      Long logId,
      Long queueId,
      Long subscriptionId,
      PushSubscriptionRequest subscription,
      PushPayload payload) {}

  public void deliverReady(Long id) {
    Job job =
        tx.execute(
            status -> {
              var q = queues.lockById(id).orElse(null);
              if (q == null || q.getStatus() != QueueStatus.READY) return null;
              var s = q.getOrder().getPushSubscription();
              if (s == null || !s.isActive()) return null;
              if (logs.existsByQueueIdAndEventType(id, "READY")) return null;
              var body =
                  "อาหารคิว "
                      + String.format("%03d", q.getQueueNumber())
                      + " วันที่ "
                      + q.getQueueDate()
                          .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                      + " พร้อมแล้ว เชิญรับอาหารได้เลย";
              var log =
                  NotificationLog.builder()
                      .queue(q)
                      .channel(NotificationChannel.PUSH)
                      .message(body)
                      .success(false)
                      .eventType("READY")
                      .deliveryStatus("PENDING")
                      .attemptedAt(LocalDateTime.now(ZoneOffset.UTC))
                      .build();
              logs.saveAndFlush(log);
              return new Job(
                  log.getId(),
                  id,
                  s.getId(),
                  new PushSubscriptionRequest(
                      s.getEndpoint(),
                      new PushSubscriptionRequest.Keys(s.getP256dh(), s.getAuth())),
                  new PushPayload(
                      "อาหารพร้อมแล้ว", body, "queue-" + id + "-ready", "/queue/" + id));
            });
    if (job == null) return;
    int code;
    try {
      code = strategy.send(job.subscription(), job.payload());
    } catch (Exception ex) {
      code = -1;
    }
    final int result = code;
    tx.executeWithoutResult(
        status -> {
          var q = queues.lockById(id).orElse(null);
          var log = logs.findById(job.logId()).orElseThrow();
          boolean accepted = result >= 200 && result < 300;
          log.setSuccess(accepted);
          log.setDeliveryStatus(result == 0 ? "PREVIEW" : accepted ? "ACCEPTED" : "FAILED");
          if (result > 0) log.setHttpStatus(result);
          if (accepted) log.setSentAt(LocalDateTime.now(ZoneOffset.UTC));
          if (q != null && (result == 404 || result == 410)) {
            var s = q.getOrder().getPushSubscription();
            if (s != null && s.getId().equals(job.subscriptionId())) s.setActive(false);
          }
        });
  }
}
