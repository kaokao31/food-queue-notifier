package com.kku.queuenotify.common;

import com.kku.queuenotify.domain.enums.NotificationChannel;
import com.kku.queuenotify.service.NotificationStrategy;
import com.kku.queuenotify.service.impl.ConsoleNotificationStrategy;
import com.kku.queuenotify.service.impl.LineNotificationStrategy;
import com.kku.queuenotify.service.impl.PushNotificationStrategy;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory Pattern
 * DIP: คลาสที่มาใช้ (NotificationService) จะขึ้นกับ interface NotificationStrategy
 * เท่านั้น ไม่รู้จัก concrete class ใด ๆ เลย — Factory เป็นจุดเดียวที่ผูก mapping
 * channel -> strategy ไว้ ทำให้ OCP: เพิ่ม channel ใหม่ = เพิ่ม case ที่นี่ที่เดียว
 * (ทางเลือกที่ดีกว่านี้อีกขั้นคือให้ Spring inject List<NotificationStrategy>
 * พร้อม getChannel() ในตัว strategy เอง เพื่อไม่ต้องแก้ Factory เลยแม้แต่บรรทัดเดียว)
 */
@Component
public class NotificationFactory {

    private final Map<NotificationChannel, NotificationStrategy> strategies = new EnumMap<>(NotificationChannel.class);

    public NotificationFactory(LineNotificationStrategy line,
                                PushNotificationStrategy push,
                                ConsoleNotificationStrategy console) {
        strategies.put(NotificationChannel.LINE, line);
        strategies.put(NotificationChannel.PUSH, push);
        strategies.put(NotificationChannel.CONSOLE, console);
    }

    public NotificationStrategy getStrategy(NotificationChannel channel) {
        NotificationStrategy strategy = strategies.get(channel);
        if (strategy == null) {
            throw new IllegalArgumentException("ไม่รองรับช่องทางแจ้งเตือน: " + channel);
        }
        return strategy;
    }
}
