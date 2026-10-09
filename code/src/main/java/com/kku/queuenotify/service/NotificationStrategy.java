package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;

public interface NotificationStrategy {
  /** Provider HTTP status for real sending; 0 means console preview without provider delivery. */
  int send(PushSubscriptionRequest subscription, PushPayload payload);
}
