package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.service.QueueStateHandler;
import org.springframework.stereotype.Component;

@Component
public class PreparingState implements QueueStateHandler {

    @Override
    public void next(QueueContext context) {
        context.transitionTo(new ReadyState());
    }

    @Override
    public void cancel(QueueContext context) {
        context.transitionTo(new CancelledState());
    }

    @Override
    public QueueStatus getStatus() {
        return QueueStatus.PREPARING;
    }
}
