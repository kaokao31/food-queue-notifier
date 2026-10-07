package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.PushDemoException;
import com.kku.queuenotify.service.PushSubscriptionService;
import com.kku.queuenotify.service.WebPushSender;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Profile("push-demo")
public class InMemoryPushSubscriptionService implements PushSubscriptionService {
    private record Entry(PushSubscriptionRequest subscription, Instant expiresAt,
                         Instant lastAttempt) { }

    private final Map<String, Entry> subscriptions = new HashMap<>();
    private final WebPushSender sender;

    public InMemoryPushSubscriptionService(WebPushSender sender) {
        this.sender = sender;
    }

    @Override
    public synchronized void subscribe(String sessionId, PushSubscriptionRequest request) {
        validate(request);
        Instant now = Instant.now();
        subscriptions.entrySet().removeIf(e -> !e.getValue().expiresAt().isAfter(now));
        if (!subscriptions.containsKey(sessionId) && subscriptions.size() >= 100) {
            throw new PushDemoException(HttpStatus.TOO_MANY_REQUESTS,
                    "โหมดทดลองเต็ม กรุณาลองใหม่ภายหลัง");
        }
        Entry previous = subscriptions.get(sessionId);
        subscriptions.put(sessionId, new Entry(request, now.plusSeconds(1800),
                previous == null ? Instant.EPOCH : previous.lastAttempt()));
    }

    @Override
    public synchronized void sendTest(String sessionId) {
        Entry entry = subscriptions.get(sessionId);
        Instant now = Instant.now();
        if (entry == null || !entry.expiresAt().isAfter(now)) {
            subscriptions.remove(sessionId);
            throw new PushDemoException(HttpStatus.NOT_FOUND,
                    "กรุณากดเปิดรับแจ้งเตือนอีกครั้ง แล้วจึงส่งทดสอบ");
        }
        if (entry.lastAttempt().plusSeconds(5).isAfter(now)) {
            throw new PushDemoException(HttpStatus.TOO_MANY_REQUESTS,
                    "กรุณารอ 5 วินาทีก่อนส่งซ้ำ");
        }
        subscriptions.put(sessionId, new Entry(entry.subscription(), entry.expiresAt(), now));
        int status = sender.sendTest(entry.subscription());
        if (status == 404 || status == 410) {
            subscriptions.remove(sessionId);
            throw new PushDemoException(HttpStatus.GONE,
                    "Subscription หมดอายุ กรุณารีเซ็ตสิทธิ์แจ้งเตือนของเว็บแล้วสมัครใหม่");
        }
        if (status < 200 || status >= 300) {
            throw new PushDemoException(HttpStatus.BAD_GATEWAY,
                    "บริการ Push ปฏิเสธคำขอ (HTTP " + status + ")");
        }
    }

    private void validate(PushSubscriptionRequest request) {
        try {
            URI uri = URI.create(request.endpoint());
            // This demo supports Chrome/FCM only. Never forward arbitrary URLs.
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"fcm.googleapis.com".equalsIgnoreCase(uri.getHost())
                    || uri.getRawUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !(uri.getPath().startsWith("/fcm/send/")
                         || uri.getPath().startsWith("/wp/"))) {
                throw new IllegalArgumentException();
            }
            byte[] key = Base64.getUrlDecoder().decode(request.keys().p256dh());
            byte[] auth = Base64.getUrlDecoder().decode(request.keys().auth());
            if (key.length != 65 || key[0] != 4 || auth.length != 16) {
                throw new IllegalArgumentException();
            }
        } catch (Exception ex) {
            throw new PushDemoException(HttpStatus.BAD_REQUEST,
                    "ข้อมูล Subscription ไม่ถูกต้อง โหมดทดลองนี้รองรับ Chrome/FCM เท่านั้น");
        }
    }
}
