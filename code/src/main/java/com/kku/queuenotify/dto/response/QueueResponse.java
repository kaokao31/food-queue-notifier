package com.kku.queuenotify.dto.response;

import com.kku.queuenotify.domain.enums.QueueStatus;
import java.time.LocalDateTime;

// Builder ผ่าน record + static factory ก็เพียงพอสำหรับ DTO ที่ immutable
public record QueueResponse(
    Long id,
    Integer queueNumber,
    QueueStatus status,
    LocalDateTime statusChangedAt) {}
