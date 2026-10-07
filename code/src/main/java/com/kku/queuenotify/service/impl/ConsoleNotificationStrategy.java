package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.Customer;
import com.kku.queuenotify.service.NotificationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock strategy — ใช้ระหว่างพัฒนา/เดโม แทนการเรียก LINE Notify หรือ FCM จริง
 * SRP: มีหน้าที่เดียวคือส่งข้อความผ่าน log เท่านั้น
 */
@Slf4j
@Component
public class ConsoleNotificationStrategy implements NotificationStrategy {

    @Override
    public NotificationResult send(Customer customer, String message) {
        log.info("[CONSOLE-NOTIFY] to {} ({}): {}", customer.getName(), customer.getPhone(), message);
        return new NotificationResult(true, "logged to console");
    }
}
