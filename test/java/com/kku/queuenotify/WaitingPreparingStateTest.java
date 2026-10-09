package com.kku.queuenotify;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.QueueStateHandler;
import com.kku.queuenotify.service.impl.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.IllegalTransactionStateException;
import static org.junit.jupiter.api.Assertions.*;

class WaitingPreparingStateTest {
  @Test void waitingThenPreparingAdvancesToReadyThroughRealContext(){
    var queue=QueueContextTest.queue();var events=new ArrayList<Object>();
    QueueContextTest.transaction().executeWithoutResult(tx->{
      new QueueContext(queue,new WaitingState(),events::add,QueueContextTest.CLOCK).next();
      assertEquals(QueueStatus.PREPARING,queue.getStatus());
      new QueueContext(queue,new PreparingState(),events::add,QueueContextTest.CLOCK).next();
      assertEquals(QueueStatus.READY,queue.getStatus());
    });
    assertEquals(2,events.size());var first=(QueueStatusChangedEvent)events.get(0);var second=(QueueStatusChangedEvent)events.get(1);
    assertEquals(QueueStatus.WAITING,first.fromStatus());assertEquals(QueueStatus.PREPARING,first.status());
    assertEquals(QueueStatus.PREPARING,second.fromStatus());assertEquals(QueueStatus.READY,second.status());
    assertEquals(queue.getStatusChangedAt(),second.changedAt());assertNotEquals(first.eventId(),second.eventId());
  }
  @Test void bothStatesCanCancelWithExactlyOneEvent(){
    for(QueueStateHandler state:new QueueStateHandler[]{new WaitingState(),new PreparingState()}){
      var queue=QueueContextTest.queue();queue.setStatus(state.getStatus());var events=new ArrayList<Object>();
      QueueContextTest.transaction().executeWithoutResult(tx->new QueueContext(queue,state,events::add,QueueContextTest.CLOCK).cancel());
      assertEquals(QueueStatus.CANCELLED,queue.getStatus());assertEquals(1,events.size());
      var event=(QueueStatusChangedEvent)events.get(0);assertEquals(state.getStatus(),event.fromStatus());
      assertEquals(QueueStatus.CANCELLED,event.status());assertEquals(queue.getStatusChangedAt(),event.changedAt());
    }
  }
  @Test void handlersRejectEveryDifferentStateBeforeCallingTransition(){
    for(QueueStateHandler handler:new QueueStateHandler[]{new WaitingState(),new PreparingState()}){
      for(var current:QueueStatus.values()){
        if(current==handler.getStatus())continue;var transitions=new AtomicInteger();
        var context=new QueueStateHandler.Context(){
          public QueueStatus getStatus(){return current;}
          public void transitionTo(QueueStatus target){transitions.incrementAndGet();}
        };
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->handler.next(context)).getStatus());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->handler.cancel(context)).getStatus());
        assertEquals(0,transitions.get());
      }
    }
  }
  @Test void realHandlersDoNotBypassContextTransactionRequirement(){
    for(QueueStateHandler state:new QueueStateHandler[]{new WaitingState(),new PreparingState()}){
      var queue=QueueContextTest.queue();queue.setStatus(state.getStatus());var events=new ArrayList<Object>();
      var context=new QueueContext(queue,state,events::add,QueueContextTest.CLOCK);
      assertThrows(IllegalTransactionStateException.class,context::next);
      assertThrows(IllegalTransactionStateException.class,context::cancel);
      assertEquals(state.getStatus(),queue.getStatus());assertTrue(events.isEmpty());
    }
  }
  @Test void previousHandlerCannotBeUsedAfterStatusChanged(){
    var queue=QueueContextTest.queue();var events=new ArrayList<Object>();
    QueueContextTest.transaction().executeWithoutResult(tx->{
      new QueueContext(queue,new WaitingState(),events::add,QueueContextTest.CLOCK).next();
      assertThrows(ApiException.class,()->new QueueContext(queue,new WaitingState(),events::add,QueueContextTest.CLOCK).cancel());
      new QueueContext(queue,new PreparingState(),events::add,QueueContextTest.CLOCK).cancel();
    });assertEquals(QueueStatus.CANCELLED,queue.getStatus());assertEquals(2,events.size());
  }
  @Test void handlersRegisterAsIndependentStatelessSpringComponents(){
    new ApplicationContextRunner().withUserConfiguration(WaitingState.class,PreparingState.class).run(ctx->{
      assertNull(ctx.getStartupFailure());var handlers=ctx.getBeansOfType(QueueStateHandler.class);
      assertEquals(2,handlers.size());assertEquals(Set.of(QueueStatus.WAITING,QueueStatus.PREPARING),
          Set.copyOf(handlers.values().stream().map(QueueStateHandler::getStatus).toList()));
      var events=new ArrayList<Object>();
      QueueContextTest.transaction().executeWithoutResult(tx->{
        var one=QueueContextTest.queue();var two=QueueContextTest.queue();
        new QueueContext(one,ctx.getBean(WaitingState.class),events::add,QueueContextTest.CLOCK).next();
        new QueueContext(two,ctx.getBean(WaitingState.class),events::add,QueueContextTest.CLOCK).cancel();
        assertEquals(QueueStatus.PREPARING,one.getStatus());assertEquals(QueueStatus.CANCELLED,two.getStatus());
      });assertEquals(2,events.size());
    });
  }
}
