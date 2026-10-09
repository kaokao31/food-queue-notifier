# Handover

## ส่วนที่พร้อมส่งต่อ

K: หน้าเมนู/ตะกร้า/checkout/history/queue/staff-menu/staff-queue, local QR, forms และชุดทดสอบ UI/MVC
A: V1–V5, repositories/mappers, menu/image APIs, transactional order CRUD และเลขคิว Bangkok

## Interface ที่ต้องเติม

- C implements `QueueTokenGenerator.generate/hash` และ `OrderAccessService.isStaff/locked`; ใช้ provider จริงเพียงตัวเดียวของแต่ละ interface
- `locked` ต้องตรวจ staff/token และล็อก Queue ภายใน transaction ให้เข้ากับ OrderService; token ดิบคืนเฉพาะสร้างออเดอร์และบันทึกเฉพาะ hash
- C เติม SecurityConfig, CSRF endpoint/forms, Queue API/State และการยกเลิกตามกฎ; อย่าใช้ OrderingTestDoubles ใน main sources
- T เติม subscription validation/API, status event/Observer, Strategy/sender/delivery และ Log; ป้องกัน READY ส่งซ้ำ และใช้ Browser Push/Service Worker จริง

## ตรวจเมื่อรวม

รัน `mvn clean verify` และ JS suites ก่อนตรวจ acceptance จริง จัดการ test-only access/clock fixtures ให้แยกจาก security tests ใหม่ ไม่ทำให้ผล module tests ถูกตีความเป็นการตรวจสิทธิ์จริง บันทึกผลและปัญหาใหม่ ไม่ปิด checklist จากจำนวน commits

Docker/CI configuration เป็นการเตรียมรันในเครื่อง ยังไม่มีผล deploy จริง CI ไม่ publish หรือ deploy image สไลด์ยังไม่รวมในรอบนี้
