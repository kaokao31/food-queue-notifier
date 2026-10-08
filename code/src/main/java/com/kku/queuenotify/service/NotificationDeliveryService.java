package com.kku.queuenotify.service;

public interface NotificationDeliveryService {
  void deliverReady(Long queueId);
}
