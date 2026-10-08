# sequence-subscribe

```mermaid
sequenceDiagram
 participant C as Android Chrome
 participant API as OrderController
 participant S as OrderSubscriptionService
 participant DB as PostgreSQL
 C->>C: user permission + service worker + push subscription
 C->>API: PUT order/id/push-subscription + token + CSRF
 API->>S: attach
 S->>DB: lock Queue, verify token/state
 S->>S: validate FCM endpoint and key sizes
 S->>DB: upsert Subscription; attach to Order
 S->>DB: commit
 API-->>C: 204
 Note over S,DB: if READY publish after-commit event; shared claim prevents duplicate

```
