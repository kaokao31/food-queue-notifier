package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.service.QueueStateHandler;
import org.springframework.stereotype.Component;

@Component
public class WaitingState implements QueueStateHandler {

    @Override
    public void next(QueueContext context) {
        context.transitionTo(new PreparingState());
    }

    @Override
    public void cancel(QueueContext context) {
        context.transitionTo(new CancelledState());
    }

    @Override
    public QueueStatus getStatus() {
        return QueueStatus.WAITING;
    }
}
