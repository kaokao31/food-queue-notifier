package com.kku.queuenotify;

import com.kku.queuenotify.service.impl.QueueContext;
import com.kku.queuenotify.service.impl.QueueStatusChangedEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

class QueueTransitionEventTest {
  @Configuration(proxyBeanMethods=false) @EnableTransactionManagement static class Transactions {
    @Bean PlatformTransactionManager manager(){return new OrderAccessTransactionTest.FixtureManager();}
    @Bean Observer observer(){return new Observer();}
  }
  // Test-only observer verifies Spring AFTER_COMMIT semantics; this is not T's Push listener.
  static class Observer {
    final List<QueueStatusChangedEvent> delivered=new ArrayList<>();
    @TransactionalEventListener void receive(QueueStatusChangedEvent event){delivered.add(event);}
  }
  @Test void observerRunsOnlyAfterSuccessfulCommit(){new ApplicationContextRunner().withUserConfiguration(Transactions.class).run(ctx->{
    var observer=ctx.getBean(Observer.class);var q=QueueContextTest.queue();
    new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
      new QueueContext(q,QueueContextTest.handler(q.getStatus()),ctx.getSourceApplicationContext(),QueueContextTest.CLOCK).next();
      assertTrue(observer.delivered.isEmpty());
    });assertEquals(1,observer.delivered.size());assertEquals(8L,observer.delivered.get(0).queueId());
  });}
  @Test void rollbackDoesNotInvokeAfterCommitObserver(){new ApplicationContextRunner().withUserConfiguration(Transactions.class).run(ctx->{
    var observer=ctx.getBean(Observer.class);var q=QueueContextTest.queue();
    new TransactionTemplate(ctx.getBean(PlatformTransactionManager.class)).executeWithoutResult(tx->{
      new QueueContext(q,QueueContextTest.handler(q.getStatus()),ctx.getSourceApplicationContext(),QueueContextTest.CLOCK).cancel();
      tx.setRollbackOnly();
    });assertTrue(observer.delivered.isEmpty());
  });}
}
