package com.kku.queuenotify.service.impl;

import java.util.UUID;

/** Requests READY catch-up notification after subscription attachment commits.
 * This is a subscription event, not a queue state transition. No credentials or JPA entities. */
public record OrderSubscriptionAttachedEvent(UUID eventId, Long orderId) {}
