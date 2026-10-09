package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.IllegalStateTransitionException;
import com.kku.queuenotify.service.QueueStateHandler;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** One operation on a queue locked by the caller's transaction; create a fresh context per operation. */
public final class QueueContext implements QueueStateHandler.Context {
  private final Queue queue;
  private final QueueStateHandler state;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public QueueContext(Queue queue,QueueStateHandler state,ApplicationEventPublisher events,Clock clock) {
    this.queue=Objects.requireNonNull(queue);this.state=Objects.requireNonNull(state);
    this.events=Objects.requireNonNull(events);this.clock=Objects.requireNonNull(clock);
  }
  @Override public QueueStatus getStatus(){return queue.getStatus();}
  public void next(){requireMutation();state.next(this);}
  public void cancel(){requireMutation();state.cancel(this);}

  @Override public void transitionTo(QueueStatus target) {
    requireMutation();Objects.requireNonNull(target);
    var before=getStatus();if(before==target)throw conflict();
    var changedAt=LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);
    queue.setStatus(target);queue.setStatusChangedAt(changedAt);
    // Publish inside the transaction. Notification observers must use AFTER_COMMIT, without fallback execution.
    events.publishEvent(new QueueStatusChangedEvent(UUID.randomUUID(),queue.getId(),before,target,changedAt));
  }

  private void requireMutation() {
    if(!TransactionSynchronizationManager.isActualTransactionActive()
        || !TransactionSynchronizationManager.isSynchronizationActive()
        || TransactionSynchronizationManager.isCurrentTransactionReadOnly())
      throw new IllegalTransactionStateException("Queue mutation requires a synchronized writable transaction");
    if(queue.getId()==null || queue.getId()<=0 || getStatus()==null || state.getStatus()!=getStatus())throw conflict();
  }
  private IllegalStateTransitionException conflict(){return new IllegalStateTransitionException("Queue state does not match this operation");}
}
