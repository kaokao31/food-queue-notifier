package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.config.WebPushProperties;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.PushDemoException;
import com.kku.queuenotify.service.WebPushSender;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Urgency;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.concurrent.TimeUnit;

@Service
@Profile("push-demo")
public class BrowserWebPushSender implements WebPushSender {
    private final WebPushProperties properties;

    public BrowserWebPushSender(WebPushProperties properties) {
        this.properties = properties;
    }

    @Override
    public int sendTest(PushSubscriptionRequest subscription) {
        if (blank(properties.getPublicKey()) || blank(properties.getPrivateKey())
                || blank(properties.getSubject())) {
            throw new PushDemoException(HttpStatus.SERVICE_UNAVAILABLE,
                    "ยังไม่ได้ตั้งค่า VAPID ให้ครบทั้งสามค่า");
        }
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            var service = new PushService(properties.getPublicKey(),
                    properties.getPrivateKey(), properties.getSubject());
            var message = Notification.builder()
                    .endpoint(subscription.endpoint())
                    .userPublicKey(subscription.keys().p256dh())
                    .userAuth(subscription.keys().auth())
                    .payload("""
                        {"title":"ทดสอบแจ้งเตือนคิวอาหาร",
                         "body":"ได้รับ Web Push จริงแล้ว — ข้อความนี้เป็นการทดสอบ",
                         "tag":"queue-demo","url":"/push-demo.html"}
                        """)
                    .ttl(60)
                    .urgency(Urgency.HIGH)
                    .build();
            var future = service.sendAsync(message);
            try {
                return future.get(15, TimeUnit.SECONDS).getStatusLine().getStatusCode();
            } finally {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PushDemoException(HttpStatus.SERVICE_UNAVAILABLE,
                    "การส่งถูกขัดจังหวะ กรุณาลองใหม่");
        } catch (Exception ex) {
            // Do not expose keys, endpoints or upstream response bodies.
            throw new PushDemoException(HttpStatus.BAD_GATEWAY,
                    "ส่ง Push ไม่สำเร็จ ตรวจอินเทอร์เน็ตและค่ากุญแจ VAPID");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
