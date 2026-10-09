package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;
import com.kku.queuenotify.service.NotificationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.mode", havingValue = "console", matchIfMissing = true)
public class ConsoleNotificationStrategy implements NotificationStrategy {
  private static final Logger LOG = LoggerFactory.getLogger(ConsoleNotificationStrategy.class);

  @Override public int send(PushSubscriptionRequest subscription, PushPayload payload) {
    // Log no subscription credentials or user-controlled message text.
    LOG.info("Local notification preview (no provider request)");
    return 0;
  }
}
