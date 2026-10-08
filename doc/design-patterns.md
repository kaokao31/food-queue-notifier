# Design Patterns

| Pattern | ปัญหาที่แก้ | ตำแหน่งจริง |
|---|---|---|
| Layered Architecture | แยก HTTP/business/persistence | controller/api → service interfaces → service/impl → repository |
| MVC | หน้าร้าน/คิว/พนักงานใช้ template แยกจาก controller | WebController + templates + DTO API |
| Repository | ซ่อน JPA query/locking | MenuItemRepository, OrderRepository, QueueRepository |
| Service Layer | Transaction และกฎธุรกิจมีเจ้าของเดียว | MenuServiceImpl, OrderServiceImpl, QueueServiceImpl |
| DTO + Mapper | ไม่ expose Entity/keys/token hash | MenuItemRequest, OrderRequest, OrderResponse, MenuItemMapper, OrderMapper, QueueMapper |
| Dependency Injection | เปลี่ยน implementation/test double โดย constructor | Controllers/Services และ Spring Security config |
| State (Behavioral) | แต่ละสถานะอนุญาต next/cancel ต่างกัน | QueueStateHandler, QueueContext, Waiting/Preparing/Ready/Completed/CancelledState |
| Observer (Behavioral) | เปลี่ยนคิวไม่ผูกกับ provider และไม่ส่งก่อน commit | QueueStatusChangedEvent, AFTER_COMMIT NotificationEventListener |
| Strategy (Behavioral) | เลือก Web Push จริงหรือ console preview สำหรับ local/testing | NotificationStrategy, PushNotificationStrategy, ConsoleNotificationStrategy เลือกด้วย notification.mode |

State terminal โยน business conflict 409 ไม่ใช้ UnsupportedOperationException. Observer ใช้ id/status event ไม่ส่ง managed Entity ข้าม thread. Delivery claim และผลส่งใช้ transaction ใหม่ก่อน/หลัง provider call และไม่ถือ queue lock ระหว่างเรียกภายนอก.

Console strategy return 0 ทำให้ log PREVIEW ไม่ masquerade ว่าส่งจริง; Web Push ส่งผ่าน BrowserWebPushSender ซึ่งเป็น dependency interface. ไม่มี LINE mock ที่อ้าง success แล้ว.

ดู class/sequence/state diagrams ใน diagrams/. Pattern เป็นเหตุผลเชิงออกแบบ ต้องสาธิตเส้นทางเรียกใช้จริงประกอบ.
