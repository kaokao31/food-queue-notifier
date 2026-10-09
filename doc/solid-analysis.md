# SOLID analysis

- SRP: Web/API controller รับคำขอ, Service จัด transaction/business rules, Repository ติดต่อฐานข้อมูล, Mapper สร้าง response และ UI module แยก basket/checkout/history/queue
- DIP: OrderService และ QR ใช้ interface ของ token/access ผ่าน provider; การไม่มี provider ปฏิเสธคำขอแทนการเปิดสิทธิ์จำลอง
- ISP: QueueTokenGenerator แยกจาก OrderAccessService และ QueueNumberService; ไม่บังคับให้โมดูลออเดอร์รับผิดชอบ Push
- OCP: จุดเชื่อม interface รองรับ implementation จริงภายหลัง; ผลของ State/Observer/Strategy ต้องประเมินหลังรวม C/T
- LSP: fixtures เป็นเพียงตัวแทนใน tests ยังไม่ใช่หลักฐานว่า production security implementation ทุกตัวทดแทนกันได้

ไม่อ้างว่าระบบครบทุกหลัก SOLID โดยไม่มีการตรวจ implementation และ behavior
