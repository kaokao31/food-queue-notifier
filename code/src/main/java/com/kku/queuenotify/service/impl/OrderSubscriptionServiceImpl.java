package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.common.QueueToken;
import com.kku.queuenotify.domain.entity.PushSubscription;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.PushSubscriptionRepository;
import com.kku.queuenotify.service.*;
import java.time.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderSubscriptionServiceImpl implements OrderSubscriptionService {
  private final OrderAccessService access;
  private final PushSubscriptionRepository subscriptions;
  private final SubscriptionValidator validator;
  private final QueueToken hashes;
  private final ApplicationEventPublisher events;

  public OrderSubscriptionServiceImpl(
      OrderAccessService a,
      PushSubscriptionRepository s,
      SubscriptionValidator v,
      QueueToken h,
      ApplicationEventPublisher e) {
    access = a;
    subscriptions = s;
    validator = v;
    hashes = h;
    events = e;
  }

  public void attach(Long id, String token, PushSubscriptionRequest r) {
    var q = access.locked(id, token);
    if (q.getStatus() == QueueStatus.COMPLETED || q.getStatus() == QueueStatus.CANCELLED)
      throw new ApiException(HttpStatus.CONFLICT, "ออเดอร์สิ้นสุดแล้ว");
    validator.validate(r);
    var hash = hashes.hash(r.endpoint());
    var existing = subscriptions.findByEndpointHash(hash);
    var s = existing.orElseGet(PushSubscription::new);
    if (existing.isPresent()
        && (!s.getEndpoint().equals(r.endpoint())
            || !s.getP256dh().equals(r.keys().p256dh())
            || !s.getAuth().equals(r.keys().auth())))
      throw new ApiException(HttpStatus.CONFLICT, "Subscription เปลี่ยนกุญแจ กรุณาติดต่อพนักงาน");
    var now = LocalDateTime.now(ZoneOffset.UTC);
    if (s.getId() == null) s.setCreatedAt(now);
    s.setUpdatedAt(now);
    s.setEndpointHash(hash);
    s.setEndpoint(r.endpoint());
    s.setP256dh(r.keys().p256dh());
    s.setAuth(r.keys().auth());
    s.setActive(true);
    subscriptions.saveAndFlush(s);
    q.getOrder().setPushSubscription(s);
    if (q.getStatus() == QueueStatus.READY)
      events.publishEvent(new QueueStatusChangedEvent(id, QueueStatus.READY, QueueStatus.READY));
  }

  public void detach(Long id, String token) {
    access.locked(id, token).getOrder().setPushSubscription(null);
  }
}
