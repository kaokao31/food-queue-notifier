package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;

public interface OrderSubscriptionService {
  void detach(Long id, String token);

  void attach(Long id, String token, PushSubscriptionRequest request);
}
