# sequence-ready

```mermaid
sequenceDiagram
 participant Staff
 participant Q as QueueService / State
 participant DB as PostgreSQL
 participant O as Observer
 participant N as NotificationDeliveryService
 participant P as Push Provider
 participant C as Android Chrome SW
 Staff->>Q: advance authenticated + CSRF
 Q->>DB: lock queue / transition PREPARING to READY
 Q->>Q: publish id/status event
 Q->>DB: commit
 Q-->>Staff: READY
 Q-->>O: AFTER_COMMIT async
 O->>N: deliverReady(queueId)
 N->>DB: claim PENDING once under queue lock; commit
 N->>P: encrypted Web Push (without DB lock)
 P-->>N: HTTP accepted/rejected
 N->>DB: store ACCEPTED/FAILED in new transaction
 P-->>C: push delivery when available
 C->>C: showNotification
 Note over N,C: ACCEPTED is not a browser display receipt

```
