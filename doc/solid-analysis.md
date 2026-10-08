# SOLID analysis

อ้างบรรทัดจากโค้ดหลังจัดรูปแบบ 8 ตุลาคม 2026

| หลัก | ไฟล์/บรรทัด | เหตุผล |
|---|---|---|
| SRP | `code/src/main/java/com/kku/queuenotify/mapper/OrderMapper.java:8` | แปลง Entity เป็น DTO ไม่รับ HTTP หรือส่ง notification |
| OCP | `code/src/main/java/com/kku/queuenotify/service/NotificationStrategy.java:6` | เพิ่ม strategy ผ่าน interface และ DI โดยไม่แก้ QueueService |
| LSP | `code/src/main/java/com/kku/queuenotify/service/impl/ReadyState.java:9` | State ใช้สัญญา next/cancel เดียวกัน พร้อม conflict สำหรับ transition ที่ไม่อนุญาต |
| ISP | `code/src/main/java/com/kku/queuenotify/service/OrderSubscriptionService.java:5` | แยก attach/detach จาก Menu และ Order CRUD |
| DIP | `code/src/main/java/com/kku/queuenotify/controller/api/OrderController.java:22` | Controller รับ service interface ผ่าน constructor injection |

ส่วน A17 ตรวจ 9 ตุลาคม 2026: `service/impl/OrderServiceImpl.java:32` ขึ้นต่อ QueueNumberService interface;
`service/impl/QueueNumberServiceImpl.java:25` เลือกวันไทยจาก Clock และเรียก Repository;
`repository/JdbcDailyQueueCounterRepository.java:21` รับผิดชอบ SQL/row lock ผ่าน DailyQueueCounterRepository interface. แยกการเลือกวันจาก persistence (SRP)
และให้ OrderService ขึ้นกับสัญญา service (DIP); ทั้ง Service/Repository ใช้ MANDATORY ร่วม transaction ออเดอร์.
ทุก path ในย่อหน้านี้อยู่ใต้ `code/src/main/java/com/kku/queuenotify/`.

ส่วน K17: `controller/QrController.java:13` รับ QrService interface ผ่าน constructor (DIP); `service/impl/QrServiceImpl.java:20` รับผิดชอบกฎ URL และการสร้าง QR (SRP). Controller คงหน้าที่ HTTP/response headers. Path อยู่ใต้ `code/src/main/java/com/kku/queuenotify/`; endpoint และกฎ HTTPS เดิมไม่เปลี่ยน.
