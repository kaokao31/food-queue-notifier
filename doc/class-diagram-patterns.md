# Class Diagram — Design Patterns

## 1. Strategy Pattern — ช่องทางแจ้งเตือน

```mermaid
classDiagram
    class NotificationStrategy {
        <<interface>>
        +send(Customer customer, String message) NotificationResult
    }
    class LineNotificationStrategy {
        +send(Customer customer, String message) NotificationResult
    }
    class PushNotificationStrategy {
        +send(Customer customer, String message) NotificationResult
    }
    class ConsoleNotificationStrategy {
        +send(Customer customer, String message) NotificationResult
    }
    class NotificationFactory {
        -Map~NotificationChannel, NotificationStrategy~ strategies
        +getStrategy(NotificationChannel channel) NotificationStrategy
    }
    class NotificationService {
        -NotificationFactory factory
        +notify(Queue queue, String message)
    }

    NotificationStrategy <|.. LineNotificationStrategy
    NotificationStrategy <|.. PushNotificationStrategy
    NotificationStrategy <|.. ConsoleNotificationStrategy
    NotificationFactory --> NotificationStrategy : creates
    NotificationService --> NotificationFactory : uses
```

**ปัญหาที่แก้:** ถ้าไม่ใช้ Strategy จะต้องเขียน `if (channel == LINE) {...} else if (channel == PUSH) {...}` ใน `NotificationService` โดยตรง ทุกครั้งที่เพิ่มช่องทางใหม่ต้องแก้คลาสเดิม (ผิด OCP) — Strategy ทำให้เพิ่ม channel ใหม่ = เพิ่มคลาส implement `NotificationStrategy` เท่านั้น

## 2. Observer Pattern — แจ้งเตือนเมื่อสถานะคิวเปลี่ยน (ใช้ Spring ApplicationEvent)

```mermaid
classDiagram
    class QueueStatusChangedEvent {
        -Queue queue
        -QueueStatus previousStatus
        -QueueStatus newStatus
        +getQueue() Queue
    }
    class QueueService {
        -ApplicationEventPublisher publisher
        +updateStatus(Long queueId, QueueStatus newStatus)
    }
    class NotificationEventListener {
        -NotificationService notificationService
        +onQueueStatusChanged(QueueStatusChangedEvent event)
    }
    class NotificationService {
        +notify(Queue queue, String message)
    }

    QueueService ..> QueueStatusChangedEvent : publishes
    NotificationEventListener ..> QueueStatusChangedEvent : listens (@EventListener)
    NotificationEventListener --> NotificationService : uses
```

**ปัญหาที่แก้:** `QueueService` ไม่จำเป็นต้องรู้จักหรือ inject `NotificationService` โดยตรง (decouple) — เมื่อสถานะเปลี่ยน แค่ publish event ใครก็ตามที่สนใจ (ตอนนี้คือ `NotificationEventListener` แต่ในอนาคตอาจมี `AnalyticsListener`, `KitchenDisplayListener`) มา subscribe เพิ่มได้โดยไม่แตะ `QueueService` เลย

## 3. State Pattern — วงจรสถานะของ Queue

```mermaid
classDiagram
    class QueueStateHandler {
        <<interface>>
        +next(QueueContext context) void
        +cancel(QueueContext context) void
        +getStatus() QueueStatus
    }
    class WaitingState {
        +next(QueueContext context) void
        +cancel(QueueContext context) void
    }
    class PreparingState {
        +next(QueueContext context) void
        +cancel(QueueContext context) void
    }
    class ReadyState {
        +next(QueueContext context) void
        +cancel(QueueContext context) void
    }
    class CompletedState {
        +next(QueueContext context) void
        +cancel(QueueContext context) void
    }
    class QueueContext {
        -Queue queue
        -QueueStateHandler currentState
        +transitionTo(QueueStateHandler state)
        +next()
        +cancel()
    }

    QueueStateHandler <|.. WaitingState
    QueueStateHandler <|.. PreparingState
    QueueStateHandler <|.. ReadyState
    QueueStateHandler <|.. CompletedState
    QueueContext --> QueueStateHandler : delegates to
```

**Transition ที่อนุญาต:** `WAITING → PREPARING → READY → COMPLETED` (เดินหน้าได้ทางเดียว) ยกเว้น `cancel()` ที่เรียกได้จาก `WAITING`/`PREPARING` เท่านั้น — `ReadyState.cancel()` และ `CompletedState.cancel()` ต้อง throw `IllegalStateTransitionException` แทนการ silent fail

**ปัญหาที่แก้:** ถ้าไม่ใช้ State จะต้องมี `if/switch` เช็คสถานะปัจจุบันทุกครั้งก่อนเปลี่ยนสถานะกระจายอยู่หลายจุดในโค้ด เสี่ยงต่อการเปลี่ยนสถานะผิดกฎ (เช่น COMPLETED ย้อนกลับไป WAITING) — State pattern รวม logic การเปลี่ยนสถานะไว้ในคลาสของสถานะนั้นเอง
