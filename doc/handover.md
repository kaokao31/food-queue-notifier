# Handover

สถานะเอกสาร T16 อ้างอิง develop `76a788a` หลัง T15 merge PR #64 วันที่ 10 ตุลาคม 2026 ไม่ใช่การรับรอง final release

## ส่วนที่รวมแล้ว

- K16: หน้าเมนู/ตะกร้า/checkout/history/queue/staff-menu/staff-queue, local QR, forms, UI/MVC tests และ runtime/CI
- A16: V1–V5, repositories/mappers, menu/image APIs, transactional order CRUD และเลขคิว Bangkok
- C1–C11: QueueToken, owner/STAFF access และ PESSIMISTIC_WRITE lock, BCrypt configured login, security, CSRF endpoint, five State handlers, transactional QueueService, immutable events และ Queue API
- T1–T15: subscription validation/attach/detach, Browser Push/service worker, sender/Strategy, durable READY claim/results, STAFF log, AFTER_COMMIT observer/catch-up, protected standalone demo และ native API integration tests; T16 เพิ่มเอกสารในรอบนี้

## Contract และการส่งต่อ

OrderService/QueueService ใช้ OrderAccessService.locked ภายใน caller transaction; ห้ามเปลี่ยน lock เป็น read-only ที่ไม่รองรับการล็อก Queue token ดิบคืนตอนสร้างออเดอร์และเก็บ hash ใน DB การอ่านภายหลังใช้สิทธิ์เจ้าของหรือ STAFF
QueueContext เผยแพร่ QueueStatusChangedEvent ภายใน business transaction ส่วน T observer รับหลัง commit เท่านั้น กรณี READY ที่มี subscription attachment ใหม่ใช้ OrderSubscriptionAttachedEvent โดยไม่สร้าง C event ซ้ำ
NotificationDeliveryService เป็นเจ้าของ READY claim การส่ง provider ไม่มี transaction/row lock และ claim เดิมทุกสถานะกันส่งซ้ำ ไม่มี automatic retry/replay ดู [Push handover](push-delivery.md) และ [ผล/ข้อจำกัดการทดสอบ](push-test-report.md)

## งานที่ยังเหลือ

คมชาญยังเหลือ C12–C16 ตามแผน: common error handling และงานตรวจรวม/ส่งต่อในขอบเขตของ C ให้ตรวจ diff และ rerun full suite เมื่อปรับ behavior อย่าใช้จำนวน commits ปิด acceptance checklist
ต้องตรวจ notification บนอุปกรณ์จริง, HTTPS/environment ที่ใช้ deploy, final acceptance, สไลด์และหลักฐานส่งงานของทีม ไม่ถือว่า provider ACCEPTED เท่ากับแสดงข้อความบนอุปกรณ์แล้ว
README/Docker/CI ของ K เป็น runtime ที่เตรียมไว้ CI ไม่ publish/deploy image อัตโนมัติ Compose app ยังรับเฉพาะ DB/PORT; ต้องเพิ่ม/ตรวจ STAFF,VAPID,notification mode และ profile ตาม environment จริงก่อนใช้ container ทดสอบ Push

## ตรวจเมื่อรวมงานรอบถัดไป

รัน `mvn clean verify`, JS suites และ `java tools/GenerateVapidKeys.java --check` แล้วบันทึก SHA/ผลใหม่ ผล Java full suite ล่าสุดที่ยืนยันก่อน T16 คือ 247 tests ไม่มี failures/errors/skips บนเครื่องธีธัช (T15)
แยก OrderingTestDoubles และ test-only provider ออกจาก production เสมอ Native API tests ใหม่ใช้ security/CSRF/owner/service จริงกับ PostgreSQL แต่จำลอง provider; ผล UI/OS notification และ deployment ต้องมีหลักฐานเพิ่มเติม
