# Component overview

อ้างอิง `develop` baseline `64b3ce7` วันที่ 10 ตุลาคม 2026 State/Security และ Subscription/Push/Log รวมแล้ว

```mermaid
flowchart LR
  B[Browser UI] --> W[Spring Security + MVC / API]
  SW[Browser Service Worker] -->|subscription ผ่านหน้าเว็บ| B
  W --> S[Order / Menu / Queue services]
  W --> Q[QrService / local ZXing]
  W --> DOC[OpenAPI / Swagger - STAFF]
  S --> ACCESS[OrderAccessService / token / row lock]
  S --> STATE[QueueContext + State handlers]
  S --> MAP[DTO / Mapper]
  S --> SUB[OrderSubscriptionService]
  S --> R[Spring Data repositories]
  ACCESS --> R
  SUB --> R
  R --> DB[(PostgreSQL)]
  STATE --> EVT[Spring application events]
  SUB --> EVT
  EVT -->|AFTER_COMMIT| OBS[ReadyNotificationObserver]
  OBS --> DEL[NotificationDeliveryService]
  DEL --> R
  DEL --> STR[NotificationStrategy]
  STR --> CON[Console preview]
  STR --> PUSH[WebPushSender / VAPID]
  PUSH --> PROVIDER[External Web Push provider]
  PROVIDER --> SW
```

เส้น provider → Service Worker เป็นการส่ง Push ผ่านบริการเบราว์เซอร์ ไม่ใช่การตอบกลับ HTTP ของ API หน้าเว็บ Service Worker ขอแสดงแจ้งเตือนกับระบบปฏิบัติการ ภาพนี้ไม่ยืนยันว่ามี deployment ภายนอกแล้ว

QueueContext สร้าง transition event ภายใน transaction และ Observer เรียก delivery หลัง commit การส่ง HTTP ไป provider อยู่นอก transaction/row lock และผลบันทึกใน notification_log ดู [READY sequence](sequence-ready.md) และ [Deployment](deployment.md)
