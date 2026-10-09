# Domain model

```mermaid
classDiagram
 Order "1" *-- "0..*" OrderItem
 Order "1" *-- "0..1" Queue
 OrderItem --> MenuItem
 note for Order "SQL customer_id retained as optional legacy column"
 Order --> PushSubscription : optional
 MenuItem "1" -- "0..1" MenuImage : shared ID
 Queue "1" -- "0..*" NotificationLog
 QueueNumberService --> DailyQueueCounterRepository
 OrderService --> QueueTokenGenerator : interface
 OrderService --> OrderAccessService : interface
```

MenuImage entity map ไป menu_item_image; order item เก็บ snapshot อิสระจากราคาที่เปลี่ยนใน MenuItem Order/Queue map shared ID ด้วย MapsId ไม่เติม State, security หรือ Push implementation จากแบบจำลองนี้
