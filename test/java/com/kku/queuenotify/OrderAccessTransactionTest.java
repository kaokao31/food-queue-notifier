package com.kku.queuenotify;

import com.kku.queuenotify.common.QueueToken;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.OrderAccessService;
import com.kku.queuenotify.service.impl.OrderAccessServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderAccessTransactionTest {
  @Configuration(proxyBeanMethods=false) @EnableTransactionManagement static class Transactions {}
  // Test-only transaction manager verifies propagation; no database or physical locking claim.
  static class FixtureManager extends AbstractPlatformTransactionManager {
    private final ThreadLocal<Boolean> active=ThreadLocal.withInitial(()->false);
    protected Object doGetTransaction(){return active.get();}
    protected boolean isExistingTransaction(Object transaction){return Boolean.TRUE.equals(transaction);}
    protected void doBegin(Object transaction,TransactionDefinition definition){active.set(true);}
    protected void doCommit(DefaultTransactionStatus status){}
    protected void doRollback(DefaultTransactionStatus status){}
    protected void doCleanupAfterCompletion(Object transaction){active.remove();}
  }
  private ApplicationContextRunner context(QueueRepository queues) {
    return new ApplicationContextRunner().withUserConfiguration(Transactions.class,OrderAccessServiceImpl.class,QueueToken.class)
        .withBean(QueueRepository.class,()->queues).withBean(PlatformTransactionManager.class,FixtureManager::new);
  }
  @Test void outsideTransactionIsRejectedBeforeRepositoryAccess() {
    var queues=mock(QueueRepository.class);
    context(queues).run(ctx->{
      assertNull(ctx.getStartupFailure());
      assertThrows(IllegalTransactionStateException.class,()->ctx.getBean(OrderAccessService.class).locked(8L,"fixture"));
      verifyNoInteractions(queues);
    });
  }
  @Test void accessJoinsCallerTransactionThroughQueueLookup() {
    var queues=mock(QueueRepository.class);var queue=new Queue();queue.setId(8L);queue.setTokenHash(new QueueToken().hash("fixture"));
    when(queues.lockById(8L)).thenReturn(Optional.of(queue));
    context(queues).run(ctx->{
      assertNull(ctx.getStartupFailure());var manager=(FixtureManager)ctx.getBean(PlatformTransactionManager.class);
      var template=new TransactionTemplate(manager);
      assertSame(queue,template.execute(status->{assertTrue(manager.active.get());return ctx.getBean(OrderAccessService.class).locked(8L,"fixture");}));
      assertFalse(manager.active.get());verify(queues).lockById(8L);
    });
  }
}
