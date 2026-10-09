package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Immutable transition snapshot. No JPA entity, owner token or subscription credentials. */
public record QueueStatusChangedEvent(UUID eventId, Long queueId, QueueStatus fromStatus,
    QueueStatus status, LocalDateTime changedAt) {
  public QueueStatusChangedEvent {
    Objects.requireNonNull(eventId);Objects.requireNonNull(queueId);
    Objects.requireNonNull(fromStatus);Objects.requireNonNull(status);Objects.requireNonNull(changedAt);
    if (queueId<=0 || fromStatus==status) throw new IllegalArgumentException("Invalid queue transition event");
  }
}
