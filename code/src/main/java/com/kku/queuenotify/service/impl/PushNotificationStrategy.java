package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;
import com.kku.queuenotify.service.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.mode", havingValue = "webpush", matchIfMissing = true)
public class PushNotificationStrategy implements NotificationStrategy {
  private final WebPushSender sender;

  public PushNotificationStrategy(WebPushSender sender) {
    this.sender = sender;
  }

  public int send(PushSubscriptionRequest sub, PushPayload payload) {
    return sender.send(sub, payload);
  }
}
