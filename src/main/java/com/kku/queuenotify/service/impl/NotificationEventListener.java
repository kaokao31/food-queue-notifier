package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.common.NotificationFactory;
import com.kku.queuenotify.domain.entity.NotificationLog;
import com.kku.queuenotify.domain.entity.NotificationPreference;
import com.kku.queuenotify.domain.entity.Queue;
import com.kku.queuenotify.repository.NotificationLogRepository;
import com.kku.queuenotify.service.NotificationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * SRP: หน้าที่เดียวคือ "ฟัง event แล้วสั่งส่งแจ้งเตือน + บันทึก log"
 * DIP: ขึ้นกับ NotificationFactory (คืน interface NotificationStrategy) ไม่ใช่ concrete class
 */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationFactory notificationFactory;
    private final NotificationLogRepository notificationLogRepository;

    @Async
    @EventListener
    public void onQueueStatusChanged(QueueStatusChangedEvent event) {
        Queue queue = event.getQueue();
        NotificationPreference preference = queue.getOrder().getCustomer().getNotificationPreference();

        String message = buildMessage(event);
        NotificationStrategy strategy = notificationFactory.getStrategy(preference.getChannel());
        NotificationStrategy.NotificationResult result =
                strategy.send(queue.getOrder().getCustomer(), message);

        notificationLogRepository.save(NotificationLog.builder()
                .queue(queue)
                .channel(preference.getChannel())
                .message(message)
                .success(result.success())
                .sentAt(LocalDateTime.now())
                .build());
    }

    private String buildMessage(QueueStatusChangedEvent event) {
        return switch (event.getNewStatus()) {
            case PREPARING -> "คิวหมายเลข " + event.getQueue().getQueueNumber() + " กำลังเตรียมอาหารของคุณ";
            case READY -> "คิวหมายเลข " + event.getQueue().getQueueNumber() + " พร้อมรับแล้ว!";
            case COMPLETED -> "รับออเดอร์เรียบร้อยแล้ว ขอบคุณที่ใช้บริการ";
            case CANCELLED -> "คิวหมายเลข " + event.getQueue().getQueueNumber() + " ถูกยกเลิก";
            default -> "สถานะคิวของคุณมีการอัปเดต";
        };
    }
}
