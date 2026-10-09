package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.entity.PushSubscription;
import com.kku.queuenotify.domain.enums.QueueStatus;
import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.exception.ApiException;
import com.kku.queuenotify.repository.PushSubscriptionRepository;
import com.kku.queuenotify.service.OrderAccessService;
import com.kku.queuenotify.service.OrderSubscriptionService;
import com.kku.queuenotify.service.SubscriptionValidator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderSubscriptionServiceImpl implements OrderSubscriptionService {
  private final ObjectProvider<OrderAccessService> access;
  private final PushSubscriptionRepository subscriptions;
  private final SubscriptionValidator validator;
  private final Clock clock;
  private final ApplicationEventPublisher events;

  public OrderSubscriptionServiceImpl(ObjectProvider<OrderAccessService> access,
      PushSubscriptionRepository subscriptions, SubscriptionValidator validator,
      Clock clock, ApplicationEventPublisher events) {
    this.access = access;
    this.subscriptions = subscriptions;
    this.validator = validator;
    this.clock = clock;
    this.events = events;
  }

  @Override
  public void attach(Long id, String token, PushSubscriptionRequest request) {
    OrderAccessService provider = access.getIfAvailable();
    if (provider == null) {
      throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Order access service is not available");
    }
    var queue = provider.locked(id, token);
    if (queue.getStatus() == QueueStatus.COMPLETED || queue.getStatus() == QueueStatus.CANCELLED) {
      throw new ApiException(HttpStatus.CONFLICT, "Order has ended");
    }
    validator.validate(request);
    String endpointHash = endpointHash(request.endpoint());
    String publicKey = canonical(request.keys().p256dh());
    String auth = canonical(request.keys().auth());
    var existing = subscriptions.findByEndpointHash(endpointHash);
    PushSubscription subscription = existing.orElseGet(PushSubscription::new);
    if (existing.isPresent() && (!request.endpoint().equals(subscription.getEndpoint())
        || !publicKey.equals(canonical(subscription.getP256dh()))
        || !auth.equals(canonical(subscription.getAuth())))) {
      throw new ApiException(HttpStatus.CONFLICT, "Subscription keys have changed");
    }
    LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    if (existing.isEmpty()) subscription.setCreatedAt(now);
    subscription.setUpdatedAt(now);
    subscription.setEndpointHash(endpointHash);
    subscription.setEndpoint(request.endpoint());
    subscription.setP256dh(publicKey);
    subscription.setAuth(auth);
    subscription.setActive(true);
    subscriptions.saveAndFlush(subscription);
    queue.getOrder().setPushSubscription(subscription);
    if (queue.getStatus() == QueueStatus.READY) {
      events.publishEvent(new OrderSubscriptionAttachedEvent(UUID.randomUUID(), queue.getId()));
    }
  }

  private String canonical(String value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(Base64.getUrlDecoder().decode(value));
  }

  private String endpointHash(String endpoint) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(endpoint.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available");
    }
  }
}
