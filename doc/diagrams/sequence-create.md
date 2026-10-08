# sequence-create

```mermaid
sequenceDiagram
 participant C as Customer Browser
 participant API as OrderController
 participant S as OrderServiceImpl
 participant N as QueueNumberServiceImpl
 participant R as JdbcDailyQueueCounterRepository
 participant DB as PostgreSQL
 C->>API: POST orders + items + CSRF
 API->>S: create(valid DTO)
 S->>DB: begin, lock menu rows in id order
 S->>S: calculate price/name snapshots
 S->>N: next() within the order transaction
 N->>N: derive queueDate in Asia/Bangkok from Clock
 N->>R: incrementAndGet(queueDate)
 R->>DB: insert counter if absent; update last_number + 1
 Note over R,DB: row lock held until order commit/rollback
 R->>DB: read last_number in the same transaction
 R-->>N: allocated number
 N-->>S: queueDate + queueNumber
 S->>S: generate token and SHA256
 S->>DB: persist Order + Items + shared-PK Queue
 S->>DB: commit
 Note over S,DB: failure rolls back order, items, queue and counter together
 S-->>API: OrderResponse + raw token only once
 API-->>C: 201 Created
 C->>C: store token locally, navigate queue page

```
