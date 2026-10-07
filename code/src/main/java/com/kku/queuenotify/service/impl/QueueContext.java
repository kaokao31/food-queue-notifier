package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.service.QueueStateHandler;
import lombok.Getter;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

/**
 * Context ของ State Pattern — ถือ reference ไปยัง state ปัจจุบัน
 * และเป็นจุดที่ publish event (Observer) เมื่อสถานะเปลี่ยนจริง
 */
@Getter
public class QueueContext {

    private final Queue queue;
    private final ApplicationEventPublisher eventPublisher;
    private QueueStateHandler currentState;

    public QueueContext(Queue queue, QueueStateHandler initialState, ApplicationEventPublisher eventPublisher) {
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
        queue.setStatusChangedAt(LocalDateTime.now());
        eventPublisher.publishEvent(new QueueStatusChangedEvent(this, queue, previous, newState.getStatus()));
    }
}
