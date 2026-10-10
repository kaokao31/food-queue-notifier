# Use-case overview

อ้างอิง `develop` baseline `64b3ce7` วันที่ 10 ตุลาคม 2026 ใช้ Mermaid flowchart แสดง actor/use case โดยประมาณ รายละเอียดเงื่อนไขอยู่ใน [Use-case descriptions](use-case-descriptions.md)

```mermaid
flowchart LR
  U[ลูกค้า - ไม่ต้องสมัครบัญชี] --> M([ดูเมนู / เลือกจำนวน / ตะกร้า])
  U --> O([สร้างออเดอร์])
  U --> H([ดูประวัติในเบราว์เซอร์ / ติดตามคิว])
  U --> EDIT([แก้ออเดอร์เฉพาะ WAITING])
  U --> CANCEL([ยกเลิกเฉพาะ WAITING / PREPARING])
  U --> SUB([เปิดหรือปิดการแจ้งเตือนของออเดอร์])
  U --> RECEIVE([รับแจ้งเตือนอาหารพร้อม])
  S[พนักงาน] --> LOGIN([Login / Logout])
  S --> MENU([จัดการเมนู / รูป / เปิดขาย])
  S --> LIST([ดูรายการออเดอร์และคิว])
  S --> ADV([เปลี่ยนสถานะคิว])
  S --> SCANCEL([ยกเลิกคิวก่อน READY])
  S --> LOG([ดูผลส่งแจ้งเตือน])
  S --> QR([สร้าง QR หน้าเมนู])
  S --> API([เปิด Swagger / ทดลอง API])
  P[Web Push provider / เบราว์เซอร์] --> RECEIVE
  P --> LOG
```

การดู/แก้/ยกเลิกออเดอร์และการแนบ subscription ของลูกค้าใช้ token เฉพาะออเดอร์ การ mutation ใช้ CSRF พนักงานใช้ STAFF session และ CSRF ส่วน provider → log หมายถึงผล HTTP ที่แอปบันทึกหลังส่ง ไม่ใช่ provider เขียนฐานข้อมูลโดยตรง

ประวัติลูกค้าอาศัย ID/token ที่เก็บในเบราว์เซอร์ ไม่ใช่บัญชีลูกค้าที่ค้นคืนจากเครื่องอื่นได้อัตโนมัติ ปิดการแจ้งเตือนของออเดอร์เป็นการถอด association ไม่ใช่ unsubscribe ทุกออเดอร์ของเบราว์เซอร์
