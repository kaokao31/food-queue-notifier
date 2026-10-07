package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.Customer;
import com.kku.queuenotify.service.NotificationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * OCP: การเพิ่ม channel นี้ไม่ต้องแก้ NotificationService หรือ Strategy ตัวอื่นเลย
 * TODO: ต่อ LINE Notify API จริงเมื่อมี access token — ตอนนี้เป็นโครงพร้อมใช้งาน
 */
@Slf4j
@Component
public class LineNotificationStrategy implements NotificationStrategy {

    private final RestTemplate restTemplate;

    @Value("${notification.line.access-token:}")
    private String lineAccessToken;

    public LineNotificationStrategy(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public NotificationResult send(Customer customer, String message) {
        if (lineAccessToken == null || lineAccessToken.isBlank()) {
            log.warn("LINE access token ยังไม่ถูกตั้งค่า — ข้ามการส่งจริง");
            return new NotificationResult(false, "line token not configured");
        }
        // ตัวอย่าง: restTemplate.postForEntity("https://notify-api.line.me/api/notify", ...)
        log.info("[LINE-NOTIFY] to {}: {}", customer.getName(), message);
        return new NotificationResult(true, "sent via LINE");
    }
}
