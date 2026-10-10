package com.kku.queuenotify;

import com.kku.queuenotify.domain.entity.*;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.*;
import com.kku.queuenotify.service.*;
import com.kku.queuenotify.service.impl.NotificationDeliveryServiceImpl;
import java.time.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationDeliveryTest {
  @Configuration(proxyBeanMethods=false) @EnableTransactionManagement static class Transactions {}
  // Propagation fixture only: database locking/concurrency is checked by native PostgreSQL tests.
  static class FixtureManager extends AbstractPlatformTransactionManager {
    private final ThreadLocal<Boolean> active=ThreadLocal.withInitial(()->false);
    protected Object doGetTransaction(){return active.get();}
    protected boolean isExistingTransaction(Object transaction){return Boolean.TRUE.equals(transaction);}
    protected void doBegin(Object transaction,TransactionDefinition definition){active.set(true);}
    protected void doCommit(DefaultTransactionStatus status){}
    protected void doRollback(DefaultTransactionStatus status){}
    protected Object doSuspend(Object transaction){boolean previous=active.get();active.set(false);return previous;}
    protected void doResume(Object transaction,Object suspended){active.set((Boolean)suspended);}
    protected void doCleanupAfterCompletion(Object transaction){active.remove();}
  }
  final QueueRepository queues=mock(QueueRepository.class);
  final NotificationLogRepository logs=mock(NotificationLogRepository.class);
  final NotificationStrategy strategy=mock(NotificationStrategy.class);
  final FixtureManager manager=new FixtureManager();
  final AtomicReference<NotificationLog> stored=new AtomicReference<>();
  Queue queue;
  @BeforeEach void setup() {
    var order=new Order();order.setId(8L);var sub=new PushSubscription();
    sub.setEndpoint("https://fcm.googleapis.com/fcm/send/test-only");sub.setP256dh("test-public");sub.setAuth("test-auth");sub.setActive(true);order.setPushSubscription(sub);
    queue=new Queue();queue.setId(8L);queue.setQueueNumber(4);queue.setStatus(QueueStatus.READY);queue.setOrder(order);
    when(queues.lockById(8L)).thenAnswer(invocation->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());return Optional.of(queue);});
    when(logs.existsByQueueIdAndEventType(8L,"READY")).thenAnswer(invocation->stored.get()!=null);
    when(logs.saveAndFlush(any())).thenAnswer(invocation->{assertTrue(TransactionSynchronizationManager.isActualTransactionActive());NotificationLog log=invocation.getArgument(0);log.setId(42L);stored.set(log);return log;});
    when(logs.findById(42L)).thenAnswer(invocation->Optional.ofNullable(stored.get()));
  }
  ApplicationContextRunner context(String mode,boolean withStrategy) {
    var ctx=new ApplicationContextRunner().withUserConfiguration(Transactions.class,NotificationDeliveryServiceImpl.class)
        .withPropertyValues("notification.mode="+mode).withBean(QueueRepository.class,()->queues)
        .withBean(NotificationLogRepository.class,()->logs).withBean(PlatformTransactionManager.class,()->manager)
        .withBean(Clock.class,()->Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"),ZoneOffset.UTC));
    return withStrategy?ctx.withBean(NotificationStrategy.class,()->strategy):ctx;
  }
  @Test void acceptanceIsRecordedOnceAfterCommittedClaimWithoutCallerTransaction() {
    when(strategy.send(any(),any())).thenAnswer(invocation->{
      assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
      assertEquals("PENDING",stored.get().getDeliveryStatus());
      assertEquals("/queue/8",((com.kku.queuenotify.dto.response.PushPayload)invocation.getArgument(1)).url());
      assertEquals("อาหารคิว 4 พร้อมแล้วเชิญรับอาหารได้เลย",
          ((com.kku.queuenotify.dto.response.PushPayload)invocation.getArgument(1)).body());
      return 201;
    });
    context("webpush",true).run(ctx->{
      var service=ctx.getBean(NotificationDeliveryService.class);
      new TransactionTemplate(manager).executeWithoutResult(tx->{
        service.deliverReady(8L);assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      });
      service.deliverReady(8L);verify(strategy,times(1)).send(any(),any());
      assertEquals("ACCEPTED",stored.get().getDeliveryStatus());assertTrue(stored.get().isSuccess());
      assertEquals(201,stored.get().getHttpStatus());assertNotNull(stored.get().getSentAt());
      assertEquals(QueueStatus.READY,queue.getStatus());
    });
  }
  @Test void consolePreviewNeedsNoSubscriptionAndIsNotProviderSuccess() {
    queue.getOrder().setPushSubscription(null);when(strategy.send(isNull(),any())).thenReturn(0);
    context("console",true).run(ctx->{ctx.getBean(NotificationDeliveryService.class).deliverReady(8L);
      assertEquals("PREVIEW",stored.get().getDeliveryStatus());assertFalse(stored.get().isSuccess());
      assertNull(stored.get().getHttpStatus());assertNull(stored.get().getSentAt());
      assertEquals(com.kku.queuenotify.domain.enums.NotificationChannel.CONSOLE,stored.get().getChannel());});
  }
  @Test void nonReadyAndMissingQueueProduceNoClaimOrNetwork() {
    context("webpush",true).run(ctx->{var service=ctx.getBean(NotificationDeliveryService.class);
      for(var status:QueueStatus.values()) if(status!=QueueStatus.READY){queue.setStatus(status);service.deliverReady(8L);}
      when(queues.lockById(99L)).thenReturn(Optional.empty());service.deliverReady(99L);
      service.deliverReady(null);service.deliverReady(0L);verifyNoInteractions(strategy);assertNull(stored.get());});
  }
  @Test void missingOrInactiveSubscriptionDoesNotConsumeClaim() {
    context("webpush",true).run(ctx->{var service=ctx.getBean(NotificationDeliveryService.class);
      queue.getOrder().getPushSubscription().setActive(false);service.deliverReady(8L);
      queue.getOrder().setPushSubscription(null);service.deliverReady(8L);
      assertNull(stored.get());verifyNoInteractions(strategy);});
  }
  @Test void httpFailuresAreRecordedAndDoNotRetryOrChangeQueue() {
    for(int code:new int[]{400,404,410,429,500,0,999}){
      stored.set(null);when(strategy.send(any(),any())).thenReturn(code);
      context("webpush",true).run(ctx->{var service=ctx.getBean(NotificationDeliveryService.class);service.deliverReady(8L);service.deliverReady(8L);
        assertEquals("FAILED",stored.get().getDeliveryStatus());assertFalse(stored.get().isSuccess());assertNull(stored.get().getSentAt());
        assertEquals(code>=100&&code<=599?Integer.valueOf(code):null,stored.get().getHttpStatus());
        assertEquals(QueueStatus.READY,queue.getStatus());});
    }
    verify(strategy,times(7)).send(any(),any());
  }
  @Test void providerExceptionTextNeverReachesLog() {
    when(strategy.send(any(),any())).thenThrow(new IllegalStateException("SECRET_ENDPOINT_AUTH"));
    context("webpush",true).run(ctx->{ctx.getBean(NotificationDeliveryService.class).deliverReady(8L);
      assertEquals("FAILED",stored.get().getDeliveryStatus());assertFalse(stored.get().getMessage().contains("SECRET"));assertNull(stored.get().getHttpStatus());});
  }
  @Test void absentOrUnsupportedStrategyFailsBeforeClaim() {
    context("webpush",false).run(ctx->assertThrows(ApiException.class,()->ctx.getBean(NotificationDeliveryService.class).deliverReady(8L)));
    context("unknown",true).run(ctx->assertThrows(ApiException.class,()->ctx.getBean(NotificationDeliveryService.class).deliverReady(8L)));
    verifyNoInteractions(queues,logs,strategy);
  }
  @Test void failedClaimNeverCallsProvider() {
    doThrow(new org.springframework.dao.DataIntegrityViolationException("fixture claim failure")).when(logs).saveAndFlush(any());
    context("webpush",true).run(ctx->assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->ctx.getBean(NotificationDeliveryService.class).deliverReady(8L)));
    verifyNoInteractions(strategy);
  }
  @Test void preexistingPendingAcceptedOrFailedClaimBlocksAnyNewAttempt() {
    context("webpush",true).run(ctx->{for(String status:new String[]{"PENDING","ACCEPTED","FAILED","PREVIEW"}){
      var log=new NotificationLog();log.setDeliveryStatus(status);stored.set(log);ctx.getBean(NotificationDeliveryService.class).deliverReady(8L);}
      verifyNoInteractions(strategy);verify(logs,never()).saveAndFlush(any());});
  }
  @Test void providerUsesImmutableSubscriptionSnapshotEvenIfOrderDetaches() {
    when(strategy.send(any(),any())).thenAnswer(invocation->{queue.getOrder().setPushSubscription(null);
      var request=(com.kku.queuenotify.dto.request.PushSubscriptionRequest)invocation.getArgument(0);
      assertEquals("test-auth",request.keys().auth());assertFalse(TransactionSynchronizationManager.isActualTransactionActive());return 202;});
    context("webpush",true).run(ctx->{ctx.getBean(NotificationDeliveryService.class).deliverReady(8L);assertEquals("ACCEPTED",stored.get().getDeliveryStatus());});
  }
}
