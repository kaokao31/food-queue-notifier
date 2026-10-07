package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.IllegalStateTransitionException;
import com.kku.queuenotify.service.QueueStateHandler;
import org.springframework.stereotype.Component;

@Component
public class CancelledState implements QueueStateHandler {

    @Override
    public void next(QueueContext context) {
        throw new IllegalStateTransitionException("คิวนี้ถูกยกเลิกแล้ว");
    }

    @Override
    public void cancel(QueueContext context) {
        throw new IllegalStateTransitionException("คิวนี้ถูกยกเลิกไปแล้ว");
    }

    @Override
    public QueueStatus getStatus() {
        return QueueStatus.CANCELLED;
    }
}
