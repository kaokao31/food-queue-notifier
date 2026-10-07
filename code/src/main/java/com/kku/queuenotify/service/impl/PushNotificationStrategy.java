package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.Customer;
import com.kku.queuenotify.service.NotificationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TODO: ต่อ Firebase Cloud Messaging (FCM) จริงเมื่อมี service account key
 */
@Slf4j
@Component
public class PushNotificationStrategy implements NotificationStrategy {

    @Override
    public NotificationResult send(Customer customer, String message) {
        log.info("[PUSH-NOTIFY] to {}: {}", customer.getName(), message);
        return new NotificationResult(true, "sent via push");
    }
}
