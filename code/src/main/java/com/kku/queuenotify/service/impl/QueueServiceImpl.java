package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.exception.ResourceNotFoundException;
import com.kku.queuenotify.repository.QueueRepository;
import com.kku.queuenotify.service.QueueStateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SRP: หน้าที่เดียวคือ orchestrate การเปลี่ยนสถานะคิว ผ่าน QueueContext (State Pattern)
 * ไม่รู้จัก NotificationStrategy ใด ๆ เลย — decouple ผ่าน Observer (event publishing)
 * DIP: ขึ้นกับ QueueRepository (interface) และ ApplicationEventPublisher (Spring abstraction)
 */
@Service
@RequiredArgsConstructor
public class QueueServiceImpl {

    private final QueueRepository queueRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Queue advanceQueue(Long queueId) {
        Queue queue = getQueueOrThrow(queueId);
        QueueStateHandler currentState = resolveState(queue.getStatus());
        QueueContext context = new QueueContext(queue, currentState, eventPublisher);
        context.next();
        return queueRepository.save(queue);
    }

    @Transactional
    public Queue cancelQueue(Long queueId) {
        Queue queue = getQueueOrThrow(queueId);
        QueueStateHandler currentState = resolveState(queue.getStatus());
        QueueContext context = new QueueContext(queue, currentState, eventPublisher);
        context.cancel();
        return queueRepository.save(queue);
    }

    private Queue getQueueOrThrow(Long queueId) {
        return queueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบคิวหมายเลข id=" + queueId));
    }

    // ทางเลือกที่ดีกว่าคือ inject Map<QueueStatus, QueueStateHandler> ผ่าน Spring
    // เพื่อไม่ต้องมี switch นี้เลย — เก็บไว้แบบนี้เพื่อความชัดเจนในการสอน/สาธิต
    private QueueStateHandler resolveState(QueueStatus status) {
        return switch (status) {
            case WAITING -> new WaitingState();
            case PREPARING -> new PreparingState();
            case READY -> new ReadyState();
            case COMPLETED -> new CompletedState();
            case CANCELLED -> new CancelledState();
        };
    }
}
