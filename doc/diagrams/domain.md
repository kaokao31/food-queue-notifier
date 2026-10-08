# domain

```mermaid
classDiagram
 Order "1" --> "1..*" OrderItem
 MenuItem "1" <-- "0..*" OrderItem
 Order "1" --> "1" Queue
 PushSubscription "0..1" <-- "0..*" Order
 Queue "1" --> "0..*" NotificationLog
 MenuItem "1" --> "0..1" MenuImage
 class DailyQueueCounter {
   LocalDate queueDate
   int lastNumber
 }
 class QueueNumberService {
   <<interface>>
   next() Number
 }
 QueueNumberService <|.. QueueNumberServiceImpl
 OrderServiceImpl --> QueueNumberService
 QueueNumberServiceImpl --> DailyQueueCounterRepository
 class DailyQueueCounterRepository {
   <<interface>>
   incrementAndGet(LocalDate) int
 }
 DailyQueueCounterRepository <|.. JdbcDailyQueueCounterRepository
 JdbcDailyQueueCounterRepository ..> DailyQueueCounter : persists

```

DailyQueueCounter is a technical table keyed by the Bangkok date; it has no FK to Queue.
Customer and NotificationPreference remain as legacy entities. Existing orders retain the
database customer_id/status fields, while the anonymous Order entity does not expose them.
