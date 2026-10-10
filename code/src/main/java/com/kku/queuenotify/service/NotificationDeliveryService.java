package com.kku.queuenotify.service;

/** Receives an ID after commit; implementations reload state instead of receiving managed entities. */
public interface NotificationDeliveryService {
  void deliverReady(Long queueId);
}
