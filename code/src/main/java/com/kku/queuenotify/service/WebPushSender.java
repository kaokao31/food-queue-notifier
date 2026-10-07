package com.kku.queuenotify.service;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;

public interface WebPushSender {
    int sendTest(PushSubscriptionRequest subscription);
}
