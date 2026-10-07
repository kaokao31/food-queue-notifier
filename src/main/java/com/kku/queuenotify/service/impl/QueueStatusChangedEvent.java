package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Observer Pattern (ผ่าน Spring ApplicationEventPublisher)
 * QueueService ไม่จำเป็นต้องรู้จัก NotificationEventListener เลย
 * แค่ publish event นี้ออกไป ใครสนใจก็ subscribe เอง (decoupled)
 */
@Getter
public class QueueStatusChangedEvent extends ApplicationEvent {

    private final Queue queue;
    private final QueueStatus previousStatus;
    private final QueueStatus newStatus;

    public QueueStatusChangedEvent(Object source, Queue queue, QueueStatus previousStatus, QueueStatus newStatus) {
        super(source);
        this.queue = queue;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }
}
