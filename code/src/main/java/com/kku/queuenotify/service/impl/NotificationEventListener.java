package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.service.NotificationDeliveryService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class NotificationEventListener {
  private final NotificationDeliveryService delivery;

  public NotificationEventListener(NotificationDeliveryService d) {
    delivery = d;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void changed(QueueStatusChangedEvent e) {
    if (e.newStatus() == QueueStatus.READY) delivery.deliverReady(e.queueId());
  }
}
