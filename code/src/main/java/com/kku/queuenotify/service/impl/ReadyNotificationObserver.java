package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.service.NotificationDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Delivery rechecks current READY state and owns the durable single-attempt claim. */
@Component
public class ReadyNotificationObserver {
  private static final Logger LOG=LoggerFactory.getLogger(ReadyNotificationObserver.class);
  private final NotificationDeliveryService delivery;
  public ReadyNotificationObserver(NotificationDeliveryService delivery){this.delivery=delivery;}

  @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT,fallbackExecution=false)
  public void changed(QueueStatusChangedEvent event){
    if(event.status()==QueueStatus.READY)deliver(event.queueId());
  }
  @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT,fallbackExecution=false)
  public void attached(OrderSubscriptionAttachedEvent event){deliver(event.orderId());}
  private void deliver(Long id){
    if(id==null || id<=0)return;
    try {delivery.deliverReady(id);}
    catch(RuntimeException error){
      // The business transaction has committed. Do not report a queue mutation as failed
      // or expose endpoint/key/exception text. There is no automatic retry here.
      LOG.warn("Notification processing failed after commit; no automatic retry");
    }
  }
}
