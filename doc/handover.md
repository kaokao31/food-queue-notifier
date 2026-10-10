# Handover

อ้างอิง `develop` deployment revision `c07292e` หลัง PR #81 วันที่ 10 ตุลาคม 2026 เอกสารนี้สรุป implementation ที่รวมแล้วและงานส่งมอบที่ยังเหลือ ไม่ใช่การรับรอง final release

## ส่วนที่รวมแล้ว

| เจ้าของงาน | Implementation ที่มีในระบบ |
|---|---|
| กานดิทัต | หน้าเมนู/ตะกร้า/checkout/history/queue/staff-menu/staff-queue, QR, UI/MVC tests, runtime/CI, ปรับตะกร้า/รูปเมนู และเอกสาร README/SOLID/Patterns |
| อนันต์เอกก์ | migration V1–V6, repositories/mappers, menu/image APIs, transactional order CRUD, เลขคิวรายวัน Bangkok, รูปเมนูคงเดิมหลังเปลี่ยนชื่อ และ Swagger/OpenAPI |
| คมชาญ | owner/STAFF access และ PESSIMISTIC_WRITE lock, BCrypt login, CSRF, State ทั้งห้า, Queue API, common errors, integration tests และสิทธิ์ Swagger เฉพาะ STAFF |
| ธีธัช | subscription validation/attach/detach, Browser Push/Service Worker, sender/Strategy, READY claim/results, STAFF log, AFTER_COMMIT observer/catch-up, standalone demo, ข้อความเลขคิวจริง และ inject PushConfigurationService |

งานชุด 1–16 ของทั้งสี่คนรวมแล้ว รายละเอียด architecture และ behavioral patterns อยู่ใน [Design Patterns](design-patterns.md), [Class diagrams](class-diagram-patterns.md) และ [SOLID](solid-analysis.md)

## Contracts ที่ต้องรักษา

- OrderService/QueueService ใช้ OrderAccessService.locked ภายใน transaction ของ caller เพื่อคง lock ถึง mutation/flush อ่านคิวด้วย path นี้จึงไม่ควรเปลี่ยน transaction เป็น read-only ที่ไม่รองรับ lock
- Queue token ดิบคืนตอนสร้างออเดอร์ เก็บเฉพาะ hash ในฐานข้อมูล ภายหลังลูกค้าต้องส่ง X-Queue-Token ของออเดอร์นั้น หรือใช้ STAFF session การมี token ออเดอร์ไม่ให้สิทธิ์พนักงาน
- POST/PUT/PATCH/DELETE ต้องใช้ CSRF ทั้งลูกค้าและพนักงาน GET /api/v1/csrf คืน token ของ session ให้ขอใหม่หลัง login/logout
- QueueContext สร้างใหม่ต่อ operation ของ queue ที่ lock แล้ว โดยเผยแพร่ QueueStatusChangedEvent ภายใน transaction; Observer รับหลัง commit และไม่ใช้ fallbackExecution
- การสมัครขณะ READY เผยแพร่ OrderSubscriptionAttachedEvent เพื่อ catch up หลัง attachment commit โดยไม่สร้าง transition event ซ้ำ
- NotificationDeliveryService เป็นเจ้าของ READY claim ที่บันทึกถาวร ส่ง provider นอก transaction/row lock แล้วบันทึกผลใน transaction แยก claim เดิมทุกสถานะกันส่งซ้ำ ไม่มี automatic retry/replay
- รูปเมนูใช้ imageKey ที่บันทึกไว้และ imageUrl จาก API การแก้ชื่อ/หมวดหมู่ไม่เลือกรูปใหม่ V6 เติม asset key เฉพาะรายการที่ยังไม่มี key และไม่มี uploaded image
- ข้อความ READY ใช้เลขคิวจริง ส่วนลิงก์แจ้งเตือนใช้ queue/order ID เช่นเลขคิว 4 ของออเดอร์ ID 8 ให้ข้อความเลข 4 และ URL /queue/8

ดู [Security/State](security-and-state.md), [Push delivery](push-delivery.md) และ [Migration](migration.md)

## Runtime และเอกสาร API

ใช้ JDK 21, Maven 3.9 และ PostgreSQL 16; Node.js 24 LTS ใช้ตรวจ UI ในเครื่อง ส่วน CI กำหนด Node.js 22 และ Java 21 ให้ดู workflow จริงที่ [.github/workflows/ci-cd.yml](../.github/workflows/ci-cd.yml)

STAFF_USERNAME/STAFF_PASSWORD ตั้งใน environment; รหัสว่างทำให้ login ไม่ได้ ใช้ NOTIFICATION_MODE=console สำหรับ preview หรือ webpush พร้อม VAPID key pair เดิมและ subject ที่ถูกต้อง การ source key file ต้อง export public/private key ให้ Java และอย่าเปลี่ยน key ทุกครั้งที่เปิดเว็บ

Swagger UI ที่ /swagger-ui/index.html และ OpenAPI ที่ /v3/api-docs หรือ /v3/api-docs.yaml ต้อง login STAFF ในเบราว์เซอร์เดียวกัน เอกสารอนุญาต GET เท่านั้น การทดลอง mutation ต้องใช้ CSRF และมีผลต่อข้อมูลจริง วิธีรันและทดลองอยู่ใน [README](../README.md)

## หลักฐานที่มีและข้อจำกัด

- ผล full Java suite ที่ทีมส่งล่าสุดคือ **285 tests** ไม่มี Failures/Errors/Skipped ใน C-R17 บนเครื่องคมชาญ ก่อน merge PR #78
- ผล JavaScript ที่ทีมส่งล่าสุดผ่าน **14 ไฟล์ทดสอบ** ในงานแก้รูปเมนู ส่วนการแก้เอกสารภายหลังไม่ได้รันชุดทดสอบใหม่
- ผู้ใช้ทดสอบจริงแล้วว่ารูปคงเดิมเมื่อเปลี่ยนชื่อ/หมวดหมู่ และ Swagger อ่านเมนู ID 1 ได้ HTTP 200
- วันที่ 10 ตุลาคม 2026 ตั้งค่า Neon/Render แล้ว ผู้ใช้ยืนยันสร้างออเดอร์และรับ Web Push เลขคิวจริงถูกต้องบน [เว็บออนไลน์](https://food-queue-notifier-1.onrender.com/) ดู [Deployment test report](deployment-test-report.md) ผลรอบนี้ไม่ยืนยันทุกอุปกรณ์ และยังต้องเก็บภาพ/รายละเอียดอุปกรณ์สำหรับ final acceptance

`java tools/GenerateVapidKeys.java --check` ตรวจ key pair ใหม่ที่สร้างในหน่วยความจำ ไม่ได้ตรวจ keys ที่ตั้งใน environment และไม่ได้ทดสอบส่งไป provider

## งานส่งมอบที่ยังเหลือ

- ตรวจเอกสารที่อ้าง baseline เก่า เช่นรายงานทดสอบและ acceptance โดยคงวันที่/ผลเดิมเป็นหลักฐานตามจริง ไม่เปลี่ยนผลเก่าให้ดูเหมือนเพิ่งทดสอบ
- Neon PostgreSQL 16 และ Render Docker Free/Singapore ตั้งค่าแล้ว เก็บ DB/STAFF/VAPID credentials ใน Render Environment; เมื่อเปลี่ยน credentials ให้ปรับแอปทุก environment ที่ใช้ฐานเดียวกัน ไม่ commit secrets
- Compose app ปัจจุบันรับเฉพาะ DB/PORT ต้องเพิ่มการส่ง STAFF/VAPID/mode/profile ก่อนใช้ container ทดลองฟีเจอร์เหล่านี้ GitHub Actions ยังไม่มี publish/deploy job; Render deploy แยกจาก workflow และให้ตรวจการตั้งค่า Auto-Deploy ของ service
- ทำ final acceptance บน revision/ปลายทางที่จะส่งมอบ บันทึก SHA, ผล tests, อุปกรณ์/เบราว์เซอร์ และหลักฐานการสั่งจนรับอาหาร รวมทั้งกรณีสิทธิ์/CSRF/terminal state
- จัดสไลด์ ภาพประกอบ/หลักฐาน review และ release เข้า main หลังตรวจครบ จากนั้นเปลี่ยน Render ให้ติดตาม main, deploy และตรวจ revision ที่ส่งมอบอีกครั้ง ดู [Acceptance](acceptance.md)

เมื่อเปลี่ยน behavior ให้รัน JS suites และ `mvn clean verify` โดย unset NOTIFICATION_MODE ใน subshell ตาม README ส่วนการแก้เอกสารล้วนตรวจ diff, ลิงก์และแผนภาพโดยไม่อ้างว่ารัน tests ใหม่
