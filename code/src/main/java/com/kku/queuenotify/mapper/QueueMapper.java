package com.kku.queuenotify.mapper;

import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.dto.response.QueueResponse;
import org.springframework.stereotype.Component;

// SRP: หน้าที่เดียวคือแปลง Entity <-> DTO แยกออกจาก Service โดยเด็ดขาด
@Component
public class QueueMapper {

    public QueueResponse toResponse(Queue queue) {
        return new QueueResponse(
                queue.getId(),
                queue.getQueueNumber(),
                queue.getStatus(),
                queue.getStatusChangedAt()
        );
    }
}
