package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.dto.response.PushPayload;

public interface WebPushSender {
  int sendTest(PushSubscriptionRequest subscription);

  default int send(PushSubscriptionRequest subscription, PushPayload payload) {
    throw new IllegalStateException("Sender must support queue payloads");
  }
}
