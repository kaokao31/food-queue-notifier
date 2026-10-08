package com.kku.queuenotify.service.impl;

import com.kku.queuenotify.domain.enums.QueueStatus;

public record QueueStatusChangedEvent(
    Long queueId, QueueStatus previousStatus, QueueStatus newStatus) {}
