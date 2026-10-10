package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;

/** Returns the provider HTTP status, not evidence that a browser displayed the notification. */
public interface WebPushSender {
  int sendTest(PushSubscriptionRequest subscription);
  int send(PushSubscriptionRequest subscription, PushPayload payload);
}
