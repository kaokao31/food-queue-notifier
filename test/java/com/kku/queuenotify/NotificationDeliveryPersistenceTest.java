package com.kku.queuenotify;

import com.kku.queuenotify.service.NotificationDeliveryService;
import com.kku.queuenotify.service.NotificationStrategy;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real PostgreSQL transactions with test-only provider; no network or C event doubles. */
@TestPropertySource(properties="notification.mode=webpush")
class NotificationDeliveryPersistenceTest extends IntegrationTestSupport {
  @Autowired NotificationDeliveryService delivery;
  @Autowired PlatformTransactionManager transactions;
  @MockBean NotificationStrategy strategy;
  long ready() {
    long subscription=jdbc.queryForObject("INSERT INTO push_subscription(endpoint,endpoint_hash,p256dh,auth) VALUES (?,?,?,?) RETURNING id",Long.class,
        "https://fcm.googleapis.com/fcm/send/test-only",UUID.randomUUID().toString(),"fixture-public","fixture-auth");
    long id=jdbc.queryForObject("INSERT INTO orders(total_amount,created_at,updated_at,push_subscription_id) VALUES (10,now(),now(),?) RETURNING id",Long.class,subscription);
    jdbc.update("INSERT INTO queue(id,queue_number,queue_date,status) VALUES (?,1,'2026-10-10','READY')",id);return id;
  }
  @Test void concurrentCallsShareOneDurableClaimAndReleaseLockBeforeProvider() throws Exception {
    long id=ready();var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
    when(strategy.send(any(),any())).thenAnswer(invocation->{
      assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
      assertEquals("PENDING",jdbc.queryForObject("SELECT delivery_status FROM notification_log WHERE queue_id=?",String.class,id));
      entered.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));return 201;});
    var pool=Executors.newFixedThreadPool(2);
    try {
      Future<?> first=pool.submit(()->delivery.deliverReady(id));assertTrue(entered.await(10,TimeUnit.SECONDS));
      Future<?> second=pool.submit(()->delivery.deliverReady(id));second.get(5,TimeUnit.SECONDS);
      var tx=new TransactionTemplate(transactions);tx.executeWithoutResult(status->{
        jdbc.execute("SET LOCAL lock_timeout='2s'");assertEquals(1,jdbc.update("UPDATE queue SET status='COMPLETED' WHERE id=?",id));});
      release.countDown();first.get(10,TimeUnit.SECONDS);
      assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notification_log WHERE queue_id=?",Integer.class,id));
      assertEquals("ACCEPTED",jdbc.queryForObject("SELECT delivery_status FROM notification_log WHERE queue_id=?",String.class,id));
      verify(strategy,times(1)).send(any(),any());
    } finally {release.countDown();pool.shutdownNow();}
  }
  @Test void providerFailureIsDurableAndRepeatedCallDoesNotRetry() {
    long id=ready();when(strategy.send(any(),any())).thenThrow(new IllegalStateException("SECRET_KEY_ENDPOINT"));
    delivery.deliverReady(id);delivery.deliverReady(id);
    assertEquals("FAILED",jdbc.queryForObject("SELECT delivery_status FROM notification_log WHERE queue_id=?",String.class,id));
    assertFalse(jdbc.queryForObject("SELECT message FROM notification_log WHERE queue_id=?",String.class,id).contains("SECRET"));
    assertEquals("READY",jdbc.queryForObject("SELECT status FROM queue WHERE id=?",String.class,id));verify(strategy,times(1)).send(any(),any());
  }
  @Test void outerRollbackDoesNotUndoAnAlreadyCompletedDeliveryClaim() {
    long id=ready();when(strategy.send(any(),any())).thenAnswer(invocation->{assertFalse(TransactionSynchronizationManager.isActualTransactionActive());return 202;});
    new TransactionTemplate(transactions).executeWithoutResult(status->{delivery.deliverReady(id);status.setRollbackOnly();});
    assertEquals("ACCEPTED",jdbc.queryForObject("SELECT delivery_status FROM notification_log WHERE queue_id=?",String.class,id));
    delivery.deliverReady(id);verify(strategy,times(1)).send(any(),any());
  }
}
