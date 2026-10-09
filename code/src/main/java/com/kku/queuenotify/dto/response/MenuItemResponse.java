package com.kku.queuenotify.dto.response;

import java.math.BigDecimal;

public record MenuItemResponse(
    Long id,
    String name,
    String category,
    BigDecimal price,
    Integer prepTimeMinutes,
    boolean isAvailable,
    String imageUrl) {}
