package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.IllegalStateTransitionException;
import com.kku.queuenotify.service.QueueStateHandler;
import org.springframework.stereotype.Component;

@Component
public class CompletedState implements QueueStateHandler {

    @Override
    public void next(QueueContext context) {
        throw new IllegalStateTransitionException("คิวนี้เสร็จสิ้นแล้ว ไม่มีสถานะถัดไป");
    }

    @Override
    public void cancel(QueueContext context) {
        throw new IllegalStateTransitionException("ไม่สามารถยกเลิกคิวที่เสร็จสิ้นแล้วได้");
    }

    @Override
    public QueueStatus getStatus() {
        return QueueStatus.COMPLETED;
    }
}
