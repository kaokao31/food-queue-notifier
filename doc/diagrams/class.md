# Class overview

อ้างอิง `develop` baseline `64b3ce7` วันที่ 10 ตุลาคม 2026 แผนภาพนี้แสดง dependency ส่วนออเดอร์และ QR ที่มี implementation แล้ว ดูรายละเอียดเมนู, State, Observer และ Strategy ใน [Class and pattern diagrams](../class-diagram-patterns.md)

```mermaid
classDiagram
  class OrderService {
    <<interface>>
  }
  class OrderAccessService {
    <<interface>>
  }
  class QueueTokenGenerator {
    <<interface>>
  }
  class QueueNumberService {
    <<interface>>
  }
  class QrService {
    <<interface>>
  }
  OrderController --> OrderService
  OrderService <|.. OrderServiceImpl
  OrderServiceImpl --> OrderRepository
  OrderServiceImpl --> MenuItemRepository
  OrderServiceImpl --> QueueRepository
  OrderServiceImpl --> NotificationLogRepository
  OrderServiceImpl --> QueueNumberService
  QueueNumberService <|.. QueueNumberServiceImpl
  QueueNumberServiceImpl --> DailyQueueCounterRepository
  OrderServiceImpl --> QueueTokenGenerator : ObjectProvider
  QueueTokenGenerator <|.. QueueToken
  OrderServiceImpl --> OrderAccessService : ObjectProvider
  OrderAccessService <|.. OrderAccessServiceImpl
  OrderAccessServiceImpl --> QueueRepository : lockById
  OrderAccessServiceImpl --> QueueToken : verifies owner hash
  OrderServiceImpl --> OrderMapper
  OrderMapper --> QueueMapper
  OrderMapper ..> OrderResponse : maps
  QrController --> QrService
  QrController --> OrderAccessService : STAFF check
  QrService <|.. QrServiceImpl
  QrServiceImpl ..> QRCodeWriter : local ZXing library
```

ลูกค้าอ่าน/แก้ออเดอร์ด้วย token เฉพาะออเดอร์ หรือใช้ STAFF session การแก้ออเดอร์ทำได้เฉพาะ WAITING ส่วน QR ใช้ STAFF และ URL HTTPS ของหน้าเมนูราก โดยสร้าง PNG ในแอป ไม่มีการเรียกบริการ QR ภายนอก

ดู [Domain](domain.md), [ER](er.md), [SOLID](../solid-analysis.md) และ [Patterns](../design-patterns.md)
