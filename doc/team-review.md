# Team review

PR ใช้ base develop และ branch ส่วนตัวของเจ้าของ ให้สมาชิกอีกคนอ่าน diff ตรวจผลทดสอบ แล้ว review/merge

| งาน | สิ่งที่ reviewer ต้องตรวจ |
|---|---|
| K | DOM/asset wiring, escaping, duplicate/stale response guards, token storage, forms, QR และผล JS/MVC |
| A | migration compatibility, locks/transactions, snapshots, image limits, daily numbers, rollback และ native PostgreSQL results |
| C | State rules, security/CSRF, ownership checks และ production token |
| T | subscription validation, event delivery, duplicate prevention, sender errors และ Log |

ไม่ถือว่าครบระบบจากจำนวน commits หรือการผ่าน test doubles ตรวจผล CI และ acceptance หลังรวม C/T ก่อนส่งเข้า main
