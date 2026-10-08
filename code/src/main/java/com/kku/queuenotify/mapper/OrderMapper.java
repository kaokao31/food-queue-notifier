package com.kku.queuenotify.mapper;

import com.kku.queuenotify.domain.entity.Order;
import com.kku.queuenotify.dto.response.OrderResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
  private final QueueMapper queues;

  public OrderMapper(QueueMapper queues) {
    this.queues = queues;
  }

  public OrderResponse response(Order o, String token) {
    return new OrderResponse(
        o.getId(),
        o.getTotalAmount(),
        o.getOrderItems().stream()
            .map(
                i ->
                    new OrderResponse.Item(
                        i.getMenuItem().getId(),
                        i.getMenuItemName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getSubtotal()))
            .toList(),
        queues.toResponse(o.getQueue()),
        token,
        o.getPushSubscription() != null && o.getPushSubscription().isActive(),
        o.getCreatedAt(),
        o.getUpdatedAt());
  }
}
