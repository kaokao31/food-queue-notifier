package com.kku.queuenotify;

import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.service.QueueStateHandler;
import com.kku.queuenotify.service.impl.QueueContext;
import com.kku.queuenotify.service.impl.QueueStatusChangedEvent;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

class QueueContextTest {
  static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"),ZoneId.of("Asia/Bangkok"));
  static Queue queue(){var q=new Queue();q.setId(8L);q.setStatus(QueueStatus.WAITING);return q;}
  // State fixtures exercise the Context port; actual state rules arrive in C08/C09.
  static QueueStateHandler handler(QueueStatus status){return new QueueStateHandler(){
    public QueueStatus getStatus(){return status;}
    public void next(Context context){context.transitionTo(QueueStatus.PREPARING);}
    public void cancel(Context context){context.transitionTo(QueueStatus.CANCELLED);}
  };}
  static TransactionTemplate transaction(){return new TransactionTemplate(new OrderAccessTransactionTest.FixtureManager());}
  @Test void nextDelegatesAndPublishesUtcSnapshot(){
    var q=queue();var events=new ArrayList<Object>();var context=new QueueContext(q,handler(q.getStatus()),events::add,CLOCK);
    transaction().executeWithoutResult(tx->context.next());
    assertEquals(QueueStatus.PREPARING,q.getStatus());assertEquals(LocalDateTime.parse("2026-10-10T00:00:00"),q.getStatusChangedAt());
    assertEquals(1,events.size());var event=(QueueStatusChangedEvent)events.get(0);
    assertNotNull(event.eventId());assertEquals(8L,event.queueId());assertEquals(QueueStatus.WAITING,event.fromStatus());
    assertEquals(QueueStatus.PREPARING,event.status());assertEquals(q.getStatusChangedAt(),event.changedAt());
    q.setStatus(QueueStatus.READY);q.setTokenHash("secret-fixture");assertEquals(QueueStatus.PREPARING,event.status());
    assertFalse(event.toString().contains("secret-fixture"));
  }
  @Test void cancelDelegatesAndGetsIndependentEventIdentity(){
    var events=new ArrayList<Object>();var a=queue();var b=queue();
    transaction().executeWithoutResult(tx->{new QueueContext(a,handler(a.getStatus()),events::add,CLOCK).cancel();
      new QueueContext(b,handler(b.getStatus()),events::add,CLOCK).cancel();});
    assertEquals(QueueStatus.CANCELLED,a.getStatus());assertEquals(2,events.size());
    assertNotEquals(((QueueStatusChangedEvent)events.get(0)).eventId(),((QueueStatusChangedEvent)events.get(1)).eventId());
  }
  @Test void mutationWithoutTransactionOrInReadOnlyTransactionIsRejected(){
    var q=queue();var events=new ArrayList<Object>();var context=new QueueContext(q,handler(q.getStatus()),events::add,CLOCK);
    assertThrows(IllegalTransactionStateException.class,context::next);
    assertThrows(IllegalTransactionStateException.class,context::cancel);
    assertThrows(IllegalTransactionStateException.class,()->context.transitionTo(QueueStatus.READY));
    var readOnly=transaction();readOnly.setReadOnly(true);
    readOnly.executeWithoutResult(tx->assertThrows(IllegalTransactionStateException.class,context::next));
    assertEquals(QueueStatus.WAITING,q.getStatus());assertNull(q.getStatusChangedAt());assertTrue(events.isEmpty());
  }
  @Test void staleHandlerInvalidQueueAndSameStateDoNotPublish(){
    var q=queue();var events=new ArrayList<Object>();
    transaction().executeWithoutResult(tx->{
      assertThrows(ApiException.class,()->new QueueContext(q,handler(QueueStatus.READY),events::add,CLOCK).next());
      var context=new QueueContext(q,handler(q.getStatus()),events::add,CLOCK);
      assertThrows(ApiException.class,()->context.transitionTo(QueueStatus.WAITING));
      assertThrows(NullPointerException.class,()->context.transitionTo(null));
      q.setId(null);assertThrows(ApiException.class,context::cancel);
    });assertEquals(QueueStatus.WAITING,q.getStatus());assertTrue(events.isEmpty());
  }
  @Test void aContextCannotReuseItsPreviousStateAfterTransition(){
    var q=queue();var events=new ArrayList<Object>();var context=new QueueContext(q,handler(q.getStatus()),events::add,CLOCK);
    transaction().executeWithoutResult(tx->{context.next();assertThrows(ApiException.class,context::next);});assertEquals(1,events.size());
  }
  @Test void publisherFailureEscapesSoCallerCanRollback(){
    var q=queue();ApplicationEventPublisher fail=event->{throw new IllegalStateException("test-only publication failure");};
    assertThrows(IllegalStateException.class,()->transaction().executeWithoutResult(tx->new QueueContext(q,handler(q.getStatus()),fail,CLOCK).next()));
  }
  @Test void eventContractRejectsInvalidSnapshot(){
    assertThrows(IllegalArgumentException.class,()->new QueueStatusChangedEvent(UUID.randomUUID(),0L,QueueStatus.WAITING,QueueStatus.READY,LocalDateTime.now()));
    assertThrows(IllegalArgumentException.class,()->new QueueStatusChangedEvent(UUID.randomUUID(),8L,QueueStatus.READY,QueueStatus.READY,LocalDateTime.now()));
    assertThrows(NullPointerException.class,()->new QueueStatusChangedEvent(null,8L,QueueStatus.WAITING,QueueStatus.READY,LocalDateTime.now()));
  }
}
