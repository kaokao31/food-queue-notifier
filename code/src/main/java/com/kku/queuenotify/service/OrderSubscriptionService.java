package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;

public interface OrderSubscriptionService {
  void attach(Long id, String token, PushSubscriptionRequest request);
}
