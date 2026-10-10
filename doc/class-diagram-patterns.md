# Class and pattern diagrams

อ้างอิง `develop` baseline `87070b5` หลัง PR #79 วันที่ 10 ตุลาคม 2026 แผนภาพนี้เน้นคลาสที่เกี่ยวข้องกับ architectural patterns และ GoF Behavioral ทั้งสาม ใช้ประกอบ [Design Patterns](design-patterns.md) และ [SOLID analysis](solid-analysis.md) สำหรับรายละเอียด business entity ดู [ER](diagrams/er.md)

## Layers / MVC

```mermaid
classDiagram
  class WebController
  class MenuTemplate {
    <<view>>
  }
  class MenuUI {
    <<module>>
  }
  class MenuController
  class MenuService {
    <<interface>>
  }
  class MenuServiceImpl
  class MenuItemRepository {
    <<interface>>
  }
  class MenuItem
  class MenuItemMapper
  class MenuItemRequest {
    <<record>>
  }
  class MenuItemResponse {
    <<record>>
  }
  WebController ..> MenuTemplate : selects template
  MenuTemplate ..> MenuUI : loads script
  MenuUI ..> MenuController : HTTP JSON
  MenuController --> MenuService : constructor injection
  MenuController ..> MenuItemRequest : validates
  MenuController ..> MenuItemResponse : returns
  MenuService <|.. MenuServiceImpl
  MenuServiceImpl --> MenuItemRepository
  MenuServiceImpl --> MenuItemMapper
  MenuItemRepository ..> MenuItem : persists
  MenuItemMapper ..> MenuItem : reads
  MenuItemMapper ..> MenuItemResponse : maps
```

MenuTemplate แทน `templates/menu.html`; MenuUI แทน `assets/menu-ui.js` ไม่ใช่คลาส Java เส้น browser → API แสดง HTTP call ไม่ใช่ constructor dependency โครงสร้างเดียวกันใช้กับออเดอร์ผ่าน OrderController/OrderService/OrderRepository/OrderMapper

## State

```mermaid
classDiagram
  class QueueController
  class QueueService {
    <<interface>>
  }
  class QueueServiceImpl
  class OrderAccessService {
    <<interface>>
  }
  class OrderAccessServiceImpl
  class QueueRepository {
    <<interface>>
    lockById(id)
  }
  class Queue
  class QueueMapper
  class QueueResponse {
    <<record>>
  }
  class QueueContext {
    next()
    cancel()
    transitionTo(status)
  }
  class QueueStateContext {
    <<interface>>
    getStatus()
    transitionTo(status)
  }
  class QueueStateHandler {
    <<interface>>
    getStatus()
    next(context)
    cancel(context)
  }
  class WaitingState
  class PreparingState
  class ReadyState
  class CompletedState
  class CancelledState
  QueueController --> QueueService
  QueueService <|.. QueueServiceImpl
  QueueServiceImpl --> OrderAccessService
  OrderAccessService <|.. OrderAccessServiceImpl
  OrderAccessServiceImpl --> QueueRepository
  QueueRepository ..> Queue : locks
  QueueServiceImpl --> QueueMapper
  QueueMapper ..> QueueResponse
  QueueServiceImpl --> QueueStateHandler : registry by status
  QueueServiceImpl ..> QueueContext : creates per operation
  QueueStateContext <|.. QueueContext
  QueueContext --> Queue : mutates in transaction
  QueueContext --> QueueStateHandler : delegates next/cancel
  QueueStateHandler ..> QueueStateContext : requests transition
  QueueStateHandler <|.. WaitingState
  QueueStateHandler <|.. PreparingState
  QueueStateHandler <|.. ReadyState
  QueueStateHandler <|.. CompletedState
  QueueStateHandler <|.. CancelledState
```

QueueStateContext เป็นชื่อแสดงใน diagram ของ nested interface `QueueStateHandler.Context` ไม่มีคลาส production ชื่อ QueueStateContext Context ถือ queue ที่ caller lock ไว้และสร้างใหม่ต่อ operation; handlers เป็น stateless Spring beans

## Observer

```mermaid
classDiagram
  class QueueContext
  class OrderSubscriptionServiceImpl
  class ApplicationEventPublisher {
    <<interface>>
    publishEvent(event)
  }
  class QueueStatusChangedEvent {
    <<record>>
  }
  class OrderSubscriptionAttachedEvent {
    <<record>>
  }
  class ReadyNotificationObserver {
    changed(event)
    attached(event)
  }
  class NotificationDeliveryService {
    <<interface>>
    deliverReady(id)
  }
  class NotificationDeliveryServiceImpl
  QueueContext --> ApplicationEventPublisher : publishes inside transaction
  QueueContext ..> QueueStatusChangedEvent : creates
  OrderSubscriptionServiceImpl --> ApplicationEventPublisher
  OrderSubscriptionServiceImpl ..> OrderSubscriptionAttachedEvent : creates
  ApplicationEventPublisher ..> ReadyNotificationObserver : Spring AFTER_COMMIT dispatch
  ReadyNotificationObserver ..> QueueStatusChangedEvent : consumes READY
  ReadyNotificationObserver ..> OrderSubscriptionAttachedEvent : consumes attachment
  ReadyNotificationObserver --> NotificationDeliveryService
  NotificationDeliveryService <|.. NotificationDeliveryServiceImpl
```

เส้น publisher → observer แทนการ dispatch ของ Spring ด้วย TransactionalEventListener ไม่ใช่ publisher เก็บ observer field เอง `fallbackExecution=false` ป้องกันการส่งนอก transaction; delivery recheck และ claim ป้องกันการส่ง READY ซ้ำ

## Strategy

```mermaid
classDiagram
  class NotificationDeliveryServiceImpl
  class NotificationStrategy {
    <<interface>>
    send(subscription, payload) int
  }
  class ConsoleNotificationStrategy
  class PushNotificationStrategy
  class WebPushSender {
    <<interface>>
    send(subscription, payload) int
    sendTest(subscription) int
  }
  class BrowserWebPushSender
  class PushConfigurationService {
    <<interface>>
    getPublicKey() String
  }
  class PushConfigurationServiceImpl
  class WebPushProperties
  class PushService {
    <<library>>
  }
  NotificationDeliveryServiceImpl --> NotificationStrategy : via ObjectProvider
  NotificationStrategy <|.. ConsoleNotificationStrategy
  NotificationStrategy <|.. PushNotificationStrategy
  PushNotificationStrategy --> WebPushSender : constructor injection
  WebPushSender <|.. BrowserWebPushSender
  BrowserWebPushSender --> PushConfigurationService : constructor injection
  PushConfigurationService <|.. PushConfigurationServiceImpl
  PushConfigurationServiceImpl --> WebPushProperties
  BrowserWebPushSender --> WebPushProperties : private key and subject
  BrowserWebPushSender ..> PushService : encryption/signing adapter
```

ConditionalOnProperty เลือก concrete Strategy ตาม notification.mode; console คืน 0 และไม่เรียก sender ส่วน webpush เรียก WebPushSender โค้ด sender รับ PushConfigurationService ผ่าน interface หลังแก้ T-R18 แผนภาพละรายละเอียด DTO, validator, mapper และ transport test seam ที่ไม่จำเป็นต่อการอธิบาย pattern
