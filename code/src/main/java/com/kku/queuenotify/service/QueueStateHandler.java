package com.kku.queuenotify.service;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.service.impl.QueueContext;

/**
 * State Pattern
 * LSP: ทุก implementation ต้อง next()/cancel() ได้โดยไม่ throw
 * UnsupportedOperationException — ถ้าทำไม่ได้จริง (เช่น cancel ตอน READY แล้ว)
 * ต้อง throw IllegalStateTransitionException ซึ่งเป็น business rule ที่ตั้งใจ ไม่ใช่ผิดสัญญา interface
 */
public interface QueueStateHandler {

    void next(QueueContext context);

    void cancel(QueueContext context);

    QueueStatus getStatus();
}
