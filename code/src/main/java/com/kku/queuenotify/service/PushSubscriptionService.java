package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;

/** Standalone demo only; sessions never attach to orders or write delivery logs. */
public interface PushSubscriptionService {
  void register(String sessionId,PushSubscriptionRequest request);
  void remove(String sessionId);
  int send(String sessionId);
}
