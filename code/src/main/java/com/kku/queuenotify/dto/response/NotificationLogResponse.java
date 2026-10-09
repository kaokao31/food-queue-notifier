package com.kku.queuenotify.dto.response;
import java.time.LocalDateTime;
/** Only safe delivery metadata; never a subscription, token or raw provider error. */
public record NotificationLogResponse(Long id,String channel,String eventType,String deliveryStatus,
    Integer httpStatus,LocalDateTime attemptedAt,LocalDateTime sentAt,String message) {}
