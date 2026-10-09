package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;
import com.kku.queuenotify.service.NotificationStrategy;
import com.kku.queuenotify.service.WebPushSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.mode", havingValue = "webpush")
public class PushNotificationStrategy implements NotificationStrategy {
  private final WebPushSender sender;
  public PushNotificationStrategy(WebPushSender sender) { this.sender = sender; }

  @Override public int send(PushSubscriptionRequest subscription, PushPayload payload) {
    return sender.send(subscription, payload);
  }
}
