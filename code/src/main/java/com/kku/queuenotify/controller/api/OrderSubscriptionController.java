package com.kku.queuenotify.controller.api;

import com.kku.queuenotify.dto.request.PushSubscriptionRequest;
import com.kku.queuenotify.service.OrderSubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/v1/orders/{id}/subscription")
public class OrderSubscriptionController {
  private final OrderSubscriptionService subscriptions;

  public OrderSubscriptionController(OrderSubscriptionService subscriptions) {
    this.subscriptions = subscriptions;
  }

  @DeleteMapping
  public ResponseEntity<Void> detach(@PathVariable @Positive Long id,
      @RequestHeader(value = "X-Queue-Token", required = false) String token) {
    subscriptions.detach(id, token);
    return ResponseEntity.noContent().build();
  }

  @PostMapping
  public ResponseEntity<Void> attach(@PathVariable @Positive Long id,
      @RequestHeader(value = "X-Queue-Token", required = false) String token,
      @RequestBody @Valid PushSubscriptionRequest request) {
    subscriptions.attach(id, token, request);
    return ResponseEntity.noContent().build();
  }
}
