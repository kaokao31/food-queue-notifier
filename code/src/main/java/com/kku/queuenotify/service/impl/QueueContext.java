package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.service.QueueStateHandler;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Context ของ State Pattern — ถือ reference ไปยัง state ปัจจุบัน และเป็นจุดที่ publish event
 * (Observer) เมื่อสถานะเปลี่ยนจริง
 */
@Getter
public class QueueContext {

  private final Queue queue;
  private final ApplicationEventPublisher eventPublisher;
  private QueueStateHandler currentState;

  public QueueContext(
      Queue queue, QueueStateHandler initialState, ApplicationEventPublisher eventPublisher) {
    this.queue = queue;
    this.currentState = initialState;
    this.eventPublisher = eventPublisher;
  }

  public void next() {
    currentState.next(this);
  }

  public void cancel() {
    currentState.cancel(this);
  }

  public void transitionTo(QueueStateHandler newState) {
    QueueStatus previous = queue.getStatus();
    this.currentState = newState;
    queue.setStatus(newState.getStatus());
    queue.setStatusChangedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
    eventPublisher.publishEvent(
        new QueueStatusChangedEvent(queue.getId(), previous, newState.getStatus()));
  }
}
