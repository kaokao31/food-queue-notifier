package com.kku.queuenotify;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.QueueStateHandler;
import com.kku.queuenotify.service.impl.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class ReadyTerminalStateTest {
  static QueueStateHandler state(QueueStatus status){return switch(status){
    case WAITING -> new WaitingState();case PREPARING -> new PreparingState();
    case READY -> new ReadyState();case COMPLETED -> new CompletedState();case CANCELLED -> new CancelledState();
  };}
  @Test void readyAdvancesToCompletedAndPublishesOneSnapshot(){
    var queue=QueueContextTest.queue();queue.setStatus(QueueStatus.READY);var events=new ArrayList<Object>();
    QueueContextTest.transaction().executeWithoutResult(tx->new QueueContext(queue,new ReadyState(),events::add,QueueContextTest.CLOCK).next());
    assertEquals(QueueStatus.COMPLETED,queue.getStatus());assertEquals(1,events.size());
    var event=(QueueStatusChangedEvent)events.get(0);assertEquals(QueueStatus.READY,event.fromStatus());
    assertEquals(QueueStatus.COMPLETED,event.status());assertEquals(queue.getStatusChangedAt(),event.changedAt());
  }
  @Test void readyCancellationIsConflictWithoutMutationOrEvent(){
    var queue=QueueContextTest.queue();queue.setStatus(QueueStatus.READY);
    var original=LocalDateTime.parse("2026-10-09T20:00:00");queue.setStatusChangedAt(original);var events=new ArrayList<Object>();
    QueueContextTest.transaction().executeWithoutResult(tx->{
      var context=new QueueContext(queue,new ReadyState(),events::add,QueueContextTest.CLOCK);
      assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,context::cancel).getStatus());
    });assertEquals(QueueStatus.READY,queue.getStatus());assertEquals(original,queue.getStatusChangedAt());assertTrue(events.isEmpty());
  }
  @Test void terminalStatesRejectNextAndCancelWithoutMutationOrEvent(){
    for(var status:new QueueStatus[]{QueueStatus.COMPLETED,QueueStatus.CANCELLED}){
      var queue=QueueContextTest.queue();queue.setStatus(status);var original=LocalDateTime.parse("2026-10-09T20:00:00");queue.setStatusChangedAt(original);
      var events=new ArrayList<Object>();
      QueueContextTest.transaction().executeWithoutResult(tx->{
        var context=new QueueContext(queue,state(status),events::add,QueueContextTest.CLOCK);
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,context::next).getStatus());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,context::cancel).getStatus());
      });assertEquals(status,queue.getStatus());assertEquals(original,queue.getStatusChangedAt());assertTrue(events.isEmpty());
    }
  }
  @Test void completeNextAndCancelMatrixUsesAllRealHandlers(){
    var next=new EnumMap<QueueStatus,QueueStatus>(QueueStatus.class);
    next.put(QueueStatus.WAITING,QueueStatus.PREPARING);next.put(QueueStatus.PREPARING,QueueStatus.READY);next.put(QueueStatus.READY,QueueStatus.COMPLETED);
    for(var initial:QueueStatus.values())for(boolean cancel:new boolean[]{false,true}){
      var queue=QueueContextTest.queue();queue.setStatus(initial);var events=new ArrayList<Object>();
      QueueStatus target=cancel?(initial==QueueStatus.WAITING||initial==QueueStatus.PREPARING?QueueStatus.CANCELLED:null):next.get(initial);
      QueueContextTest.transaction().executeWithoutResult(tx->{
        var context=new QueueContext(queue,state(initial),events::add,QueueContextTest.CLOCK);
        Runnable action=cancel?context::cancel:context::next;
        if(target==null)assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,action::run).getStatus());else action.run();
      });assertEquals(target==null?initial:target,queue.getStatus());assertEquals(target==null?0:1,events.size());
    }
  }
  @Test void newHandlersDoNotChangeAnUnrelatedContextStatus(){
    for(QueueStateHandler handler:new QueueStateHandler[]{new ReadyState(),new CompletedState(),new CancelledState()})
      for(var current:QueueStatus.values()){
        if(current==handler.getStatus())continue;var transitions=new AtomicInteger();
        var context=new QueueStateHandler.Context(){public QueueStatus getStatus(){return current;}
          public void transitionTo(QueueStatus target){transitions.incrementAndGet();}};
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->handler.next(context)).getStatus());
        assertEquals(HttpStatus.CONFLICT,assertThrows(ApiException.class,()->handler.cancel(context)).getStatus());assertEquals(0,transitions.get());
      }
  }
  @Test void springRegistersExactlyOneHandlerForEveryStatus(){
    new ApplicationContextRunner().withUserConfiguration(WaitingState.class,PreparingState.class,ReadyState.class,CompletedState.class,CancelledState.class).run(ctx->{
      assertNull(ctx.getStartupFailure());var handlers=ctx.getBeansOfType(QueueStateHandler.class);
      assertEquals(5,handlers.size());assertEquals(EnumSet.allOf(QueueStatus.class),
          EnumSet.copyOf(handlers.values().stream().map(QueueStateHandler::getStatus).toList()));
    });
  }
  @Test void fullLifecycleCannotAdvanceOrCancelAfterCompletion(){
    var queue=QueueContextTest.queue();var events=new ArrayList<Object>();
    QueueContextTest.transaction().executeWithoutResult(tx->{
      for(int i=0;i<3;i++)new QueueContext(queue,state(queue.getStatus()),events::add,QueueContextTest.CLOCK).next();
      assertEquals(QueueStatus.COMPLETED,queue.getStatus());
      var finalContext=new QueueContext(queue,new CompletedState(),events::add,QueueContextTest.CLOCK);
      assertThrows(ApiException.class,finalContext::next);assertThrows(ApiException.class,finalContext::cancel);
    });assertEquals(3,events.size());
    assertEquals(List.of(QueueStatus.PREPARING,QueueStatus.READY,QueueStatus.COMPLETED),
        events.stream().map(event->((QueueStatusChangedEvent)event).status()).toList());
  }
}
