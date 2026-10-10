package com.kku.queuenotify;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.service.NotificationDeliveryService;
import com.kku.queuenotify.service.impl.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReadyNotificationObserverTest {
  @TestConfiguration(proxyBeanMethods=false) @EnableTransactionManagement static class Transactions {
    @Bean PlatformTransactionManager manager(){return new QueueServiceTest.FixtureManager();}
    @Bean NotificationDeliveryService delivery(){return mock(NotificationDeliveryService.class);}
  }
  ApplicationContextRunner runner(){return new ApplicationContextRunner().withUserConfiguration(Transactions.class,ReadyNotificationObserver.class);}
  QueueStatusChangedEvent event(QueueStatus from,QueueStatus to){return new QueueStatusChangedEvent(UUID.randomUUID(),8L,from,to,LocalDateTime.of(2026,10,10,0,0));}
  @Test void readyIsDeliveredOnlyAfterCommit(){runner().run(ctx->{
    var delivery=ctx.getBean(NotificationDeliveryService.class);
    new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
      ctx.publishEvent(event(QueueStatus.PREPARING,QueueStatus.READY));verifyNoInteractions(delivery);
    });verify(delivery).deliverReady(8L);
  });}
  @Test void rolledBackTransitionAndAttachmentDoNotDeliver(){runner().run(ctx->{
    new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
      ctx.publishEvent(event(QueueStatus.PREPARING,QueueStatus.READY));
      ctx.publishEvent(new OrderSubscriptionAttachedEvent(UUID.randomUUID(),8L));tx.setRollbackOnly();
    });verifyNoInteractions(ctx.getBean(NotificationDeliveryService.class));
  });}
  @Test void eventsOutsideTransactionHaveNoFallback(){runner().run(ctx->{
    ctx.publishEvent(event(QueueStatus.PREPARING,QueueStatus.READY));
    ctx.publishEvent(new OrderSubscriptionAttachedEvent(UUID.randomUUID(),8L));
    verifyNoInteractions(ctx.getBean(NotificationDeliveryService.class));
  });}
  @Test void nonReadyTransitionsAreIgnoredAndAttachmentRequestsCatchup(){runner().run(ctx->{
    var delivery=ctx.getBean(NotificationDeliveryService.class);
    new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
      ctx.publishEvent(event(QueueStatus.WAITING,QueueStatus.PREPARING));
      ctx.publishEvent(event(QueueStatus.READY,QueueStatus.COMPLETED));
      ctx.publishEvent(event(QueueStatus.WAITING,QueueStatus.CANCELLED));verifyNoInteractions(delivery);
      ctx.publishEvent(new OrderSubscriptionAttachedEvent(UUID.randomUUID(),8L));
    });verify(delivery).deliverReady(8L);
  });}
  @Test void processingFailureCannotEscapeCommittedBusinessOperation(){runner().run(ctx->{
    var delivery=ctx.getBean(NotificationDeliveryService.class);
    doThrow(new IllegalStateException("test-only-secret-error")).when(delivery).deliverReady(8L);
    assertDoesNotThrow(()->new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->
        ctx.publishEvent(event(QueueStatus.PREPARING,QueueStatus.READY))));verify(delivery).deliverReady(8L);
  });}
}
