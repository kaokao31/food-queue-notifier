package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;

public interface PushSubscriptionService {
    void subscribe(String sessionId, PushSubscriptionRequest request);
    void sendTest(String sessionId);
}
