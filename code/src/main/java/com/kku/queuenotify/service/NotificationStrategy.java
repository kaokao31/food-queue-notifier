package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;

public interface NotificationStrategy {
  int send(PushSubscriptionRequest subscription, PushPayload payload);
}
