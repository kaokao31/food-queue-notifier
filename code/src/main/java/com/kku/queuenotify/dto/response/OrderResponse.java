package com.kku.queuenotify.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    BigDecimal totalAmount,
    List<Item> items,
    QueueResponse queue,
    String queueToken,
    boolean pushEnabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public record Item(
      Long menuItemId,
      String menuItemName,
      int quantity,
      BigDecimal unitPrice,
      BigDecimal subtotal) {}
}
