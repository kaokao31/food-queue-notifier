# Class

```mermaid
classDiagram
  WebController --> Templates
  OrderController --> OrderService
  OrderServiceImpl ..|> OrderService
  OrderServiceImpl --> OrderRepository
  OrderServiceImpl --> MenuItemRepository
  OrderServiceImpl --> QueueNumberService
  OrderServiceImpl --> QueueTokenGenerator
  OrderServiceImpl --> OrderAccessService
  OrderServiceImpl --> OrderMapper
  QrController --> QrService
  QrController --> OrderAccessService
  QrServiceImpl ..|> QrService
  class QueueTokenGenerator { <<interface>> }
  class OrderAccessService { <<interface>> }
```

เส้นที่ระบุรอ C/T เป็นงานที่ยังไม่รวมในระบบ; deployment เป็น configuration ที่ต้องตรวจรันจริง
