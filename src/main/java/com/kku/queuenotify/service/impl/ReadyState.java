package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.IllegalStateTransitionException;
import com.kku.queuenotify.service.QueueStateHandler;
import org.springframework.stereotype.Component;

@Component
public class ReadyState implements QueueStateHandler {

    @Override
    public void next(QueueContext context) {
        context.transitionTo(new CompletedState());
    }

    @Override
    public void cancel(QueueContext context) {
        // LSP: ยัง implement ครบ ไม่ throw UnsupportedOperationException
        // แต่ throw business exception ที่ตั้งใจให้เกิดขึ้น (ผ่าน GlobalExceptionHandler)
        throw new IllegalStateTransitionException("ไม่สามารถยกเลิกคิวที่พร้อมรับแล้วได้");
    }

    @Override
    public QueueStatus getStatus() {
        return QueueStatus.READY;
    }
}
