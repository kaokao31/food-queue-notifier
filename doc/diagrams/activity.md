# Activity — Order to READY

อ้างอิง `develop` baseline `64b3ce7` วันที่ 10 ตุลาคม 2026

```mermaid
flowchart TD
  M[ลูกค้าเลือกเมนูและจำนวน] --> B[ตรวจตะกร้า / ยืนยันสั่ง]
  B --> C[ขอ CSRF token ของ session]
  C --> O[POST Order API]
  O --> V{ข้อมูลและ CSRF ถูกต้อง?}
  V -->|ไม่ถูกต้อง| E[แสดง error / ไม่สร้างออเดอร์]
  V -->|ถูกต้อง| TX[Transaction: ตรวจราคา / snapshot / token hash / เลขคิวรายวัน]
  TX --> SAVE[บันทึกออเดอร์และคิว WAITING]
  SAVE --> Q[คืน ID / เลขคิว / token แล้วเปิดหน้าติดตาม]
  Q --> ACCESS[อ่านคิวด้วย X-Queue-Token ของออเดอร์]
  Q --> CHOICE{ลูกค้าเปิดแจ้งเตือนหรือไม่?}
  CHOICE -->|เปิด| PERM[ขอสิทธิ์ / ตรวจ Service Worker และ VAPID key]
  PERM --> ATTACH[แนบ subscription กับออเดอร์โดยใช้ token + CSRF]
  CHOICE -->|ไม่เปิด| POLL[ติดตามคิวผ่าน polling]
  ATTACH --> POLL
  STAFF[พนักงาน login] --> ADV[เปลี่ยนสถานะด้วย STAFF session + CSRF]
  ADV --> LOCK[Lock queue / ตรวจ State handler]
  LOCK --> PREP[WAITING ไป PREPARING]
  PREP --> READY[PREPARING ไป READY / publish event]
  READY --> COMMIT{Transaction commit?}
  COMMIT -->|rollback| NOSEND[ไม่ส่ง notification จาก event นี้]
  COMMIT -->|commit| OBS[AFTER_COMMIT Observer]
  OBS --> CLAIM{ยัง READY และยังไม่มี READY claim?}
  CLAIM -->|ไม่ใช่| STOP[ไม่ส่งซ้ำ]
  CLAIM -->|ใช่| MODE{notification mode}
  MODE -->|console| PREVIEW[บันทึก PREVIEW / ไม่ส่ง provider]
  MODE -->|webpush| SUB{มี active subscription?}
  SUB -->|ไม่มี| WAIT[ไม่สร้าง claim / รองรับสมัครภายหลัง]
  SUB -->|มี| SEND[บันทึก PENDING claim / ส่งนอก transaction]
  SEND --> RESULT[บันทึก ACCEPTED หรือ FAILED / ไม่มี retry อัตโนมัติ]
  RESULT --> DEVICE[Provider และเบราว์เซอร์จัดการแจ้งเตือน]
  POLL --> FINISH[พนักงาน COMPLETED หรือยกเลิกก่อน READY / UI หยุดเมื่อ terminal]
```

แผนภาพแสดงกิจกรรมลูกค้าและพนักงานที่เกิดแยกกัน การสมัครหลัง READY ใช้ attachment event แล้วเข้า delivery ตรวจใหม่ การยกเลิกทำได้เฉพาะ WAITING/PREPARING และต้องมี owner token หรือ STAFF พร้อม CSRF

ACCEPTED เป็นผลรับคำขอของ provider ไม่ใช่การยืนยัน popup โดยอุปกรณ์ ลูกค้ายังอ่านสถานะผ่าน polling ได้แม้ไม่ได้เปิด Push ดู [State](state.md) และ [READY sequence](sequence-ready.md)
