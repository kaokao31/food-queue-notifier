# READY → notification หลัง commit

ชื่อในภาพตรงกับ C11/T15 implementation การทำงานหลัง commit เป็น synchronous และไม่มี event replay

```mermaid
sequenceDiagram
    actor Staff
    participant Security as SecurityConfig
    participant API as QueueController
    participant Queue as QueueServiceImpl
    participant Access as OrderAccessServiceImpl
    participant State as QueueContext + PreparingState
    participant DB as PostgreSQL
    participant Events as Spring transaction events
    participant Observer as ReadyNotificationObserver
    participant Delivery as NotificationDeliveryServiceImpl
    participant Strategy as NotificationStrategy
    Staff->>Security: PATCH /queues/{id}/advance + session + CSRF
    Security->>API: STAFF and CSRF accepted
    API->>Queue: advance(id), begin transaction
    Queue->>Access: locked(id, null)
    Access->>DB: lock Queue row and authorize STAFF
    Queue->>State: next() from PREPARING
    State->>DB: managed status READY + UTC statusChangedAt
    State->>Events: publish immutable QueueStatusChangedEvent
    alt transaction rolls back
        Queue->>DB: rollback
        Note over Events,Observer: No AFTER_COMMIT callback, no notification claim
    else transaction commits
        Queue->>DB: commit, release business row lock
        Events->>Observer: AFTER_COMMIT READY event
        Observer->>Delivery: deliverReady(queueId)
        Delivery->>DB: new transaction: lock, recheck current READY and existing claim
        alt ended status, existing READY claim, or webpush has no active target
            DB-->>Delivery: skip, no provider request
        else eligible
            Delivery->>DB: save PENDING claim, commit and release lock
            Delivery->>Strategy: send outside DB transaction
            alt console mode
                Strategy-->>Delivery: 0, preview only
            else webpush mode
                Note over Strategy: PushNotificationStrategy -> BrowserWebPushSender, encryption/signing
                Strategy-->>Delivery: provider HTTP status or safe error
            end
            Delivery->>DB: new transaction: store PREVIEW / ACCEPTED / FAILED
        end
        Note over Observer: Processing exceptions are logged safely; business commit remains successful
        Queue-->>API: QueueResponse snapshot
        API-->>Staff: 200, no-store
    end
```

Observer ignores PREPARING, COMPLETED and CANCELLED events The delivery service—not the event ID—owns duplicate prevention via the durable READY claim. ACCEPTED means provider acceptance only. The STAFF notifications API reads safe log metadata separately.
