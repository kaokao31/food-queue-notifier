package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;
import com.kku.queuenotify.service.NotificationStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.mode", havingValue = "console")
public class ConsoleNotificationStrategy implements NotificationStrategy {
  public int send(PushSubscriptionRequest sub, PushPayload payload) {
    org.slf4j.LoggerFactory.getLogger(getClass())
        .info("Local notification preview: {}", payload.tag());
    return 0;
  }
}
