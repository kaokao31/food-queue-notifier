# Design Patterns

อ้างอิง `develop` baseline `87070b5` หลัง PR #79 วันที่ 10 ตุลาคม 2026 ระบบมี implementation ของ State, Observer และ Strategy แล้ว เอกสารนี้เลือก **กลุ่ม Behavioral จำนวน 3 แบบ** ตามเกณฑ์ที่ให้เลือกหนึ่งกลุ่มและใช้ไม่น้อยกว่า 3 patterns ในกลุ่มนั้น ไม่ต้องเพิ่ม Creational/Structural เพื่อให้ครบทุกกลุ่ม

## Enterprise / Architectural Patterns

| Pattern | ปัญหาที่แก้ | ไฟล์ / คลาสที่ใช้ | Class Diagram ประกอบ |
|---|---|---|---|
| Layered Architecture | แยก HTTP, business rules, persistence และ API mapping เพื่อไม่ผูกทุกหน้าที่กับ Controller | [OrderController](../code/src/main/java/com/kku/queuenotify/controller/api/OrderController.java#L19), [OrderServiceImpl](../code/src/main/java/com/kku/queuenotify/service/impl/OrderServiceImpl.java#L24), [OrderRepository](../code/src/main/java/com/kku/queuenotify/repository/OrderRepository.java#L8), [OrderMapper](../code/src/main/java/com/kku/queuenotify/mapper/OrderMapper.java#L8) | [Layers / MVC](class-diagram-patterns.md#layers--mvc) |
| MVC | แยกการนำทางหน้าเว็บ, template/view และข้อมูลที่หน้าเว็บโหลดจาก API | [WebController](../code/src/main/java/com/kku/queuenotify/controller/WebController.java#L7), [menu.html](../code/src/main/resources/templates/menu.html), [menu-ui.js](../code/src/main/resources/static/assets/menu-ui.js), [MenuController](../code/src/main/java/com/kku/queuenotify/controller/api/MenuController.java#L20), [MenuItemResponse](../code/src/main/java/com/kku/queuenotify/dto/response/MenuItemResponse.java#L5) | [Layers / MVC](class-diagram-patterns.md#layers--mvc) |
| Repository | ซ่อนรายละเอียด JPA/query และ lock จาก HTTP layer | [OrderRepository](../code/src/main/java/com/kku/queuenotify/repository/OrderRepository.java#L8), [QueueRepository.lockById](../code/src/main/java/com/kku/queuenotify/repository/QueueRepository.java#L9), [OrderAccessServiceImpl](../code/src/main/java/com/kku/queuenotify/service/impl/OrderAccessServiceImpl.java#L16) | [Layers / MVC](class-diagram-patterns.md#layers--mvc), [State](class-diagram-patterns.md#state) |
| Service Layer | รวม use case และ transaction ของออเดอร์/คิว รวมทั้งตรวจสิทธิ์และประสานงาน persistence | [OrderServiceImpl](../code/src/main/java/com/kku/queuenotify/service/impl/OrderServiceImpl.java#L23), [QueueServiceImpl](../code/src/main/java/com/kku/queuenotify/service/impl/QueueServiceImpl.java#L17) | [Layers / MVC](class-diagram-patterns.md#layers--mvc), [State](class-diagram-patterns.md#state) |
| DTO + Mapper | กำหนด request/response contract โดยไม่ส่ง JPA entity graph และ token hash ทั้งก้อนไป API | [OrderRequest](../code/src/main/java/com/kku/queuenotify/dto/request/OrderRequest.java#L7), [OrderResponse](../code/src/main/java/com/kku/queuenotify/dto/response/OrderResponse.java#L7), [OrderMapper.response](../code/src/main/java/com/kku/queuenotify/mapper/OrderMapper.java#L15) | [Layers / MVC](class-diagram-patterns.md#layers--mvc) |
| Dependency Injection | แยกการใช้งาน service จากการเลือก implementation และแทน dependency ใน tests ได้ | [QueueServiceImpl constructor](../code/src/main/java/com/kku/queuenotify/service/impl/QueueServiceImpl.java#L25), [PushNotificationStrategy constructor](../code/src/main/java/com/kku/queuenotify/service/impl/PushNotificationStrategy.java#L14), [BrowserWebPushSender constructor](../code/src/main/java/com/kku/queuenotify/service/impl/BrowserWebPushSender.java#L41) | [State](class-diagram-patterns.md#state), [Strategy](class-diagram-patterns.md#strategy) |

## GoF — กลุ่ม Behavioral

| Pattern | ปัญหาที่แก้ | ไฟล์ / คลาสที่ใช้ | Class Diagram ประกอบ |
|---|---|---|---|
| **State** | กฎ next/cancel แตกต่างตามสถานะคิว ต้องปฏิเสธ transition ที่ผิดและ terminal mutation โดยไม่กระจายกฎทุกสถานะใน Controller | [QueueStateHandler](../code/src/main/java/com/kku/queuenotify/service/QueueStateHandler.java#L6), [QueueContext](../code/src/main/java/com/kku/queuenotify/service/impl/QueueContext.java#L17), [QueueServiceImpl](../code/src/main/java/com/kku/queuenotify/service/impl/QueueServiceImpl.java#L18), [WaitingState](../code/src/main/java/com/kku/queuenotify/service/impl/WaitingState.java#L11), [PreparingState](../code/src/main/java/com/kku/queuenotify/service/impl/PreparingState.java#L11), [ReadyState](../code/src/main/java/com/kku/queuenotify/service/impl/ReadyState.java#L11), [CompletedState](../code/src/main/java/com/kku/queuenotify/service/impl/CompletedState.java#L11), [CancelledState](../code/src/main/java/com/kku/queuenotify/service/impl/CancelledState.java#L11) | [State](class-diagram-patterns.md#state), [State transition diagram](diagrams/state.md) |
| **Observer** | แยกการเปลี่ยนสถานะและ commit ออกจากการส่ง Push เพื่อไม่ส่งเมื่อ transaction rollback และไม่ทำให้คิวที่ commit แล้วดูเหมือนล้มเหลวเพราะ provider error | [QueueContext publisher](../code/src/main/java/com/kku/queuenotify/service/impl/QueueContext.java#L37), [QueueStatusChangedEvent](../code/src/main/java/com/kku/queuenotify/service/impl/QueueStatusChangedEvent.java#L9), [ReadyNotificationObserver](../code/src/main/java/com/kku/queuenotify/service/impl/ReadyNotificationObserver.java#L18), [OrderSubscriptionAttachedEvent](../code/src/main/java/com/kku/queuenotify/service/impl/OrderSubscriptionAttachedEvent.java#L7), [OrderSubscriptionServiceImpl publisher](../code/src/main/java/com/kku/queuenotify/service/impl/OrderSubscriptionServiceImpl.java#L88) | [Observer](class-diagram-patterns.md#observer), [READY sequence](diagrams/sequence-ready.md), [Subscribe sequence](diagrams/sequence-subscribe.md) |
| **Strategy** | เลือกพฤติกรรม console preview หรือ Web Push จริงผ่าน contract เดียว โดยไม่ให้ delivery service ทำงานเข้ารหัสและ HTTP เอง | [NotificationStrategy](../code/src/main/java/com/kku/queuenotify/service/NotificationStrategy.java#L6), [ConsoleNotificationStrategy](../code/src/main/java/com/kku/queuenotify/service/impl/ConsoleNotificationStrategy.java#L12), [PushNotificationStrategy](../code/src/main/java/com/kku/queuenotify/service/impl/PushNotificationStrategy.java#L11), [NotificationDeliveryServiceImpl](../code/src/main/java/com/kku/queuenotify/service/impl/NotificationDeliveryServiceImpl.java#L56) | [Strategy](class-diagram-patterns.md#strategy) |

## การใช้จริงและหลักฐานทดสอบ

### State

QueueServiceImpl รับ handlers เป็น List และสร้าง registry ตาม QueueStatus โดยต้องมีครบทุกสถานะเพียงครั้งเดียว เมื่อ advance/cancel จะ lock queue ผ่าน OrderAccessService แล้วสร้าง QueueContext สำหรับ operation นั้น Context มอบหมาย next/cancel ให้ handler; handler ขอ transitionTo ซึ่ง Context เป็นผู้แก้ entity/time และเผยแพร่ event

เส้นทางหลักคือ WAITING → PREPARING → READY → COMPLETED ยกเลิกได้จาก WAITING/PREPARING ส่วน READY ยกเลิกไม่ได้ และ COMPLETED/CANCELLED ปฏิเสธ mutation ดู [WaitingPreparingStateTest](../test/java/com/kku/queuenotify/WaitingPreparingStateTest.java), [ReadyTerminalStateTest](../test/java/com/kku/queuenotify/ReadyTerminalStateTest.java), [QueueContextTest](../test/java/com/kku/queuenotify/QueueContextTest.java), [QueueServiceTest](../test/java/com/kku/queuenotify/QueueServiceTest.java) และ [QueueStateIntegrationTest](../test/java/com/kku/queuenotify/QueueStateIntegrationTest.java)

QueueStatus enum อย่างเดียวไม่ใช่ State pattern หลักฐานสำคัญคือ handler interface, implementations และ delegation ใน Context ที่มีอยู่จริง

### Observer

QueueContext ใช้ ApplicationEventPublisher เผยแพร่ QueueStatusChangedEvent ภายใน transaction ส่วน ReadyNotificationObserver ใช้ `@TransactionalEventListener(phase=AFTER_COMMIT, fallbackExecution=false)` จึงรับหลัง commit และส่งต่อเฉพาะสถานะ READY อีก listener รับ OrderSubscriptionAttachedEvent เพื่อรองรับการสมัครเมื่อออเดอร์ READY แล้ว Delivery service ตรวจ READY/subscription และ durable claim ซ้ำอีกชั้น

ดู [QueueTransitionEventTest](../test/java/com/kku/queuenotify/QueueTransitionEventTest.java), [ReadyNotificationObserverTest](../test/java/com/kku/queuenotify/ReadyNotificationObserverTest.java) และ [ReadyNotificationIntegrationTest](../test/java/com/kku/queuenotify/ReadyNotificationIntegrationTest.java) ซึ่งตรวจการส่งหลัง commit และการไม่ส่งเมื่อ rollback

Observer นี้ใช้ event ของ Spring ภายในแอป ไม่ใช่ durable message broker และไม่รับประกันว่า event จะกู้คืนได้เมื่อ process หยุดในช่วงหลัง commit ระบบไม่มี automatic retry/replay และผล provider ACCEPTED ไม่ยืนยัน device display

### Strategy

Spring เลือก ConsoleNotificationStrategy เมื่อ `notification.mode=console` หรือไม่กำหนดค่า และ PushNotificationStrategy เมื่อ `notification.mode=webpush` ผ่าน ConditionalOnProperty ทั้งสอง implement NotificationStrategy โดย console คืน 0 (PREVIEW) ส่วน webpush ส่งผ่าน WebPushSender และคืน HTTP status ของ provider

NotificationDeliveryServiceImpl เรียก Strategy หลังสร้าง durable claim ใน transaction แยก แล้วบันทึกผลใน transaction อีกช่วง การส่ง HTTP ไม่ถือ business row lock อยู่ ดู [NotificationStrategyTest](../test/java/com/kku/queuenotify/NotificationStrategyTest.java), [NotificationDeliveryTest](../test/java/com/kku/queuenotify/NotificationDeliveryTest.java) และ [PushDeliveryIntegrationTest](../test/java/com/kku/queuenotify/PushDeliveryIntegrationTest.java)

การเลือก mode เป็น configuration ของแอป ไม่ได้สลับ Strategy ต่อผู้ใช้หรือระหว่าง request โดยอัตโนมัติ และการเพิ่มช่องทางใหม่ยังต้องตรวจ claim/log/configuration ที่ปัจจุบันแยก console/webpush

## ขอบเขตการนับ patterns

เลือก **State + Observer + Strategy** เป็นหลักฐาน GoF กลุ่ม Behavioral ทั้งสามมี consumer และ tests ที่ใช้งานจริง ไม่ใช้เพียงชื่อ enum, entity Subscription หรือ repository Log เป็นหลักฐาน pattern และไม่อ้าง static factory ของ DTO ว่าเป็น GoF Factory Method เพื่อเพิ่มจำนวน

ดู [SOLID analysis](solid-analysis.md) และ [Class diagrams](class-diagram-patterns.md) เอกสารนี้อธิบายโค้ดที่มีอยู่ ไม่เพิ่มฟีเจอร์หรือเปลี่ยนผลทดสอบเดิม
