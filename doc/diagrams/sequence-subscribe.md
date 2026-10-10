# สมัคร/ยกเลิก Push ของออเดอร์

```mermaid
sequenceDiagram
    actor Customer
    participant UI as PushClient + CoreUI
    participant Browser as Browser PushManager / service worker
    participant Key as PushController
    participant CSRF as CsrfController
    participant API as OrderSubscriptionController
    participant Service as OrderSubscriptionServiceImpl
    participant Access as OrderAccessServiceImpl
    participant Validator as SubscriptionValidator
    participant DB as PostgreSQL
    participant Observer as ReadyNotificationObserver
    participant Delivery as NotificationDeliveryServiceImpl
    Customer->>UI: click enable notifications for this order
    UI->>Browser: requestPermission from user gesture
    alt denied or unsupported
        Browser-->>UI: not granted
        Note over Customer,UI: Polling remains available; no subscription POST
    else granted
        UI->>Key: GET /api/v1/push/public-key
        Key-->>UI: validated public key, no-store
        UI->>Browser: register /sw.js, await active worker
        UI->>Browser: getSubscription or subscribe using public key
        Browser-->>UI: endpoint + p256dh + auth
        UI->>CSRF: GET /api/v1/csrf in same session
        CSRF-->>UI: headerName + token
        UI->>API: POST /orders/{id}/subscription + owner token + CSRF
        Note over API: SecurityConfig checks CSRF before controller
        API->>Service: attach, begin transaction
        Service->>Access: lock queue and authorize owner or STAFF
        Service->>Validator: validate provider endpoint and encryption keys
        Service->>DB: upsert compatible subscription, attach only this order
        alt current status READY
            Note over Service: Publish OrderSubscriptionAttachedEvent before commit
            Service->>DB: commit
            Observer->>Delivery: AFTER_COMMIT catch-up by order ID
            Note over Delivery: Recheck READY and single-attempt claim; send only if eligible
        else WAITING or PREPARING
            Service->>DB: commit; future READY event triggers delivery
        end
        API-->>UI: 204
    end
    Customer->>UI: click disable for this order
    UI->>API: DELETE /orders/{id}/subscription + owner token + CSRF
    API->>Service: detach, begin transaction
    Service->>Access: lock and authorize
    Service->>DB: set this order subscription association to null, commit
    API-->>UI: 204
    Note over Browser,DB: No browser unsubscribe; other orders retain shared active subscription
```

The picture shows successful requests; authorization, CSRF, endpoint/key validation and COMPLETED/CANCELLED rejection stop before attachment. Rollback never executes catch-up. Same endpoint with changed keys returns 409; an existing READY delivery claim prevents another send. Demo subscriptions use a separate in-memory service and are outside this sequence.
