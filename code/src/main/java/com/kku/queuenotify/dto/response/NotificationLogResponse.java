package com.kku.queuenotify.dto.response;

import java.time.LocalDateTime;

public record NotificationLogResponse(
    Long id,
    String message,
    String deliveryStatus,
    Integer httpStatus,
    LocalDateTime attemptedAt) {}
