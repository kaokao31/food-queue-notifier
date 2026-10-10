package com.kku.queuenotify;

import com.kku.queuenotify.common.QueueToken;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.mapper.QueueMapper;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.*;
import com.kku.queuenotify.service.impl.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueueServiceTest {
  static final String OWNER="owner-service-fixture";
  @TestConfiguration(proxyBeanMethods=false) @EnableTransactionManagement static class Transactions {
    @Bean PlatformTransactionManager manager(){return new FixtureManager();}
    @Bean Recorder recorder(){return new Recorder();}
  }
  // Test-only transaction lifecycle; database rollback is covered by the persistence tests.
  static class FixtureManager extends org.springframework.transaction.support.AbstractPlatformTransactionManager {
    static class State implements org.springframework.transaction.support.SmartTransactionObject {
      boolean active, rollbackOnly;
      public boolean isRollbackOnly(){return rollbackOnly;}
      public void flush(){}
    }
    private final ThreadLocal<State> current=new ThreadLocal<>();
    protected Object doGetTransaction(){var state=current.get();return state==null?new State():state;}
    protected boolean isExistingTransaction(Object transaction){return ((State)transaction).active;}
    protected void doBegin(Object transaction,org.springframework.transaction.TransactionDefinition definition){
      var state=(State)transaction;state.active=true;current.set(state);
    }
    protected void doSetRollbackOnly(org.springframework.transaction.support.DefaultTransactionStatus status){
      ((State)status.getTransaction()).rollbackOnly=true;
    }
    protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status){}
    protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status){}
    protected void doCleanupAfterCompletion(Object transaction){current.remove();}
  }
  static class Recorder {
    final List<QueueStatusChangedEvent> events=new ArrayList<>();
    @TransactionalEventListener void receive(QueueStatusChangedEvent event){events.add(event);}
  }
  @AfterEach void cleanup(){RequestContextHolder.resetRequestAttributes();}
  static void staff(){var request=new MockHttpServletRequest();request.setUserPrincipal(()->"test-only-staff");
    request.addUserRole("STAFF");RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));}
  Queue fixture(){var q=QueueContextTest.queue();q.setTokenHash(new QueueToken().hash(OWNER));return q;}
  QueueRepository repository(Queue q){var repository=mock(QueueRepository.class);
    when(repository.lockById(8L)).thenAnswer(invocation->{
      assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
      assertTrue(TransactionSynchronizationManager.isSynchronizationActive());return Optional.of(q);
    });return repository;}
  ApplicationContextRunner context(QueueRepository repository){return context(repository,new QueueMapper());}
  ApplicationContextRunner context(QueueRepository repository,QueueMapper mapper){return new ApplicationContextRunner()
      .withUserConfiguration(Transactions.class,QueueServiceImpl.class,OrderAccessServiceImpl.class,QueueToken.class,
          WaitingState.class,PreparingState.class,ReadyState.class,CompletedState.class,CancelledState.class)
      .withBean(QueueRepository.class,()->repository).withBean(QueueMapper.class,()->mapper)
      .withBean(Clock.class,()->QueueContextTest.CLOCK);}
  @Test void ownerReadUsesRealMandatoryAccessWithinServiceTransaction(){
    var q=fixture();var repository=repository(q);context(repository).run(ctx->{
      assertNull(ctx.getStartupFailure());var service=ctx.getBean(QueueService.class);
      assertTrue(AopUtils.isAopProxy(service));assertTrue(AopUtils.isAopProxy(ctx.getBean(OrderAccessService.class)));
      var result=service.get(8L,OWNER);assertEquals(QueueStatus.WAITING,result.status());assertEquals(8L,result.id());
      assertFalse(result.toString().contains(OWNER));assertTrue(ctx.getBean(Recorder.class).events.isEmpty());
      assertFalse(TransactionSynchronizationManager.isActualTransactionActive());verify(repository).lockById(8L);
    });
  }
  @Test void wrongMissingAndOtherOrderTokenCannotReadOrCancel(){
    var q=fixture();context(repository(q)).run(ctx->{var service=ctx.getBean(QueueService.class);
      for(String token:new String[]{null,"wrong",new QueueToken().generate()}){
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->service.get(8L,token)).getStatus());
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->service.cancel(8L,token)).getStatus());
      }assertEquals(QueueStatus.WAITING,q.getStatus());assertTrue(ctx.getBean(Recorder.class).events.isEmpty());
    });
  }
  @Test void nonStaffCannotAdvanceBeforeQueueLookup(){
    var repository=repository(fixture());context(repository).run(ctx->{
      assertEquals(HttpStatus.FORBIDDEN,assertThrows(ApiException.class,()->ctx.getBean(QueueService.class).advance(8L)).getStatus());
      verifyNoInteractions(repository);assertTrue(ctx.getBean(Recorder.class).events.isEmpty());
    });
  }
  @Test void staffAdvancesAllThreeStepsAndAfterCommitObserverGetsSnapshots(){
    var q=fixture();staff();context(repository(q)).run(ctx->{var service=ctx.getBean(QueueService.class);var recorder=ctx.getBean(Recorder.class);
      assertEquals(QueueStatus.PREPARING,service.advance(8L).status());assertEquals(1,recorder.events.size());
      assertEquals(QueueStatus.READY,service.advance(8L).status());assertEquals(QueueStatus.COMPLETED,service.advance(8L).status());
      assertEquals(List.of(QueueStatus.PREPARING,QueueStatus.READY,QueueStatus.COMPLETED),recorder.events.stream().map(QueueStatusChangedEvent::status).toList());
      assertEquals(QueueStatus.COMPLETED,service.get(8L,null).status());
    });
  }
  @Test void ownerCanCancelWaitingOrPreparingButReadyAndTerminalCommandsConflict(){
    var q=fixture();context(repository(q)).run(ctx->{var service=ctx.getBean(QueueService.class);var recorder=ctx.getBean(Recorder.class);
      for(var status:new QueueStatus[]{QueueStatus.WAITING,QueueStatus.PREPARING}){
        q.setStatus(status);assertEquals(QueueStatus.CANCELLED,service.cancel(8L,OWNER).status());
      }assertEquals(2,recorder.events.size());
      for(var status:new QueueStatus[]{QueueStatus.READY,QueueStatus.COMPLETED,QueueStatus.CANCELLED}){
        q.setStatus(status);assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->service.cancel(8L,OWNER)).getStatus());
        assertEquals(status,q.getStatus());
      }staff();for(var status:new QueueStatus[]{QueueStatus.COMPLETED,QueueStatus.CANCELLED}){
        q.setStatus(status);assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->service.advance(8L)).getStatus());
      }assertEquals(2,recorder.events.size());
    });
  }
  @Test void absentQueueOrNullStatusFailsWithoutTransitionEvent(){
    var q=fixture();var repository=repository(q);when(repository.lockById(9L)).thenReturn(Optional.empty());
    context(repository).run(ctx->{var service=ctx.getBean(QueueService.class);
      assertEquals(HttpStatus.NOT_FOUND,assertThrows(ApiException.class,()->service.get(9L,OWNER)).getStatus());
      q.setStatus(null);assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->service.cancel(8L,OWNER)).getStatus());
      assertTrue(ctx.getBean(Recorder.class).events.isEmpty());
    });
  }
  @Test void failureAfterPublicationRollsBackWithoutAfterCommitCallback(){
    var q=fixture();staff();var failing=mock(QueueMapper.class);when(failing.toResponse(q)).thenThrow(new IllegalStateException("test-only mapping failure"));
    context(repository(q),failing).run(ctx->{
      assertThrows(IllegalStateException.class,()->ctx.getBean(QueueService.class).advance(8L));
      assertTrue(ctx.getBean(Recorder.class).events.isEmpty());assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
    });
  }
  @Test void incompleteDuplicateAndNullStateRegistriesAreRejected(){
    var access=mock(OrderAccessService.class);var mapper=new QueueMapper();
    var all=new ArrayList<QueueStateHandler>(List.of(new WaitingState(),new PreparingState(),new ReadyState(),new CompletedState(),new CancelledState()));
    assertThrows(IllegalArgumentException.class,()->new QueueServiceImpl(access,mapper,List.of(new WaitingState()),event->{},QueueContextTest.CLOCK));
    all.add(new WaitingState());assertThrows(IllegalArgumentException.class,()->new QueueServiceImpl(access,mapper,all,event->{},QueueContextTest.CLOCK));
    all.remove(all.size()-1);all.add(null);assertThrows(IllegalArgumentException.class,()->new QueueServiceImpl(access,mapper,all,event->{},QueueContextTest.CLOCK));
  }
}
