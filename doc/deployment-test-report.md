# Deployment test report — Neon / Render

วันที่ทดสอบ: **10 ตุลาคม 2026** (Asia/Bangkok) ผู้ทดสอบ/ผู้รายงาน: **กานดิทัต นามสุดตา** รายงานนี้บันทึก deployment log และผล manual test ที่ผู้ทดสอบยืนยัน ไม่ใช่ full acceptance หรือผล automated tests ใหม่

## Environment และ revision

| รายการ | ค่าที่ตรวจ/ตั้งไว้ |
|---|---|
| URL | https://food-queue-notifier-1.onrender.com/ |
| Git branch / SHA | develop / c07292ee8b39266f1da4ab04e87b13a5b646c702 |
| Commit | Merge pull request #81 from kaokao31/kanditat_673380392-1_03 |
| Hosting | Render Web Service, Docker, Free, Singapore |
| Build/runtime | repository Dockerfile; Maven build; Java 21 JRE |
| Database | Neon PostgreSQL 16, Free, Singapore; database neondb; Neon branch production |
| DB connection | direct JDBC connection with TLS; credentials in environment |
| Notification mode | webpush; existing VAPID pair; mailto subject |

## ผลที่มีหลักฐานแล้ว

| ขั้นตอน | ผล | หลักฐานและขอบเขต |
|---|---|---|
| Maven ในเครื่องเชื่อม Neon | ผ่าน | log Hikari connection และ Flyway ระบุ Neon PostgreSQL 16.15 |
| Flyway สร้าง schema ใหม่ | ผ่าน | log Successfully applied 6 migrations; schema version v6 |
| แอปในเครื่องเริ่มทำงานกับ Neon | ผ่าน | Started QueueNotifyApplication และ Tomcat port 8080 |
| สร้างออเดอร์และรับ Push ในเครื่องโดยใช้ Neon | ผ่านตามผู้ทดสอบ | ผู้ใช้ยืนยันสร้างแล้วและส่งแจ้งเตือนสำเร็จ |
| Render build/deploy | ผ่าน | ภาพ Render แสดง Deploy succeeded, source c07292e |
| แอป Render เริ่มทำงาน | ผ่าน | log Started QueueNotifyApplication, Tomcat port 10000, Your service is live |
| สร้างออเดอร์และรับ Push บน URL Render | ผ่านตามผู้ทดสอบ | หลังทดสอบบน URL ผู้ใช้ยืนยันว่าถูกต้องและมีแจ้งเตือนตามเลขคิว |

ข้อความเป้าหมายคือ **อาหารคิว <เลขคิวจริง> พร้อมแล้วเชิญรับอาหารได้เลย** รอบออนไลน์ยืนยันว่าเลขคิวแสดงถูกต้อง แต่ยังไม่ได้บันทึกเลขคิว/order ID เฉพาะรอบ รุ่นอุปกรณ์/เบราว์เซอร์ หรือภาพแจ้งเตือนของรอบออนไลน์ลง repository จึงไม่เติมข้อมูลเหล่านั้นโดยคาดเดา

Flyway log มี warning ว่า PostgreSQL 16.15 ใหม่กว่าขอบเขตที่ Flyway 9.22.3 ระบุว่าทดสอบ แต่ migration V1–V6 และการเริ่มแอปสำเร็จตาม log ไม่ถือเป็น error ของรอบนี้ และไม่แทนที่การประเมิน compatibility สำหรับ release ในอนาคต

## วิธีทดสอบซ้ำ

1. เปิด URL Render สร้างออเดอร์ใหม่และเก็บ owner token ใน browser ตาม flow หน้าเว็บ
2. เปิดการแจ้งเตือนและอนุญาต permission สำหรับ Render origin; subscription จาก localhost ใช้แทน origin ใหม่ไม่ได้
3. Login STAFF ในหน้าพนักงานด้วย credentials ของ Render
4. เปลี่ยน WAITING → PREPARING → READY แล้วตรวจข้อความและเลขคิวในแจ้งเตือนจริง
5. บันทึกเวลาทดสอบ, revision, อุปกรณ์/เบราว์เซอร์, เลขคิว และภาพที่ไม่เปิดเผย credentials/token

## ข้อจำกัดและงานก่อน final release

- ผล manual test นี้ไม่รับรองทุกอุปกรณ์ และ HTTP ACCEPTED ของ provider ไม่ใช่การยืนยัน popup ทุกครั้ง
- ยังไม่ได้ทำ acceptance ครบกรณี owner isolation, CSRF, terminal state, ภาพอัปโหลด, ประวัติ และ QR บน URL นี้ ให้ตรวจตาม [Acceptance](acceptance.md)
- ผล Java 285 tests และ JS 14 ไฟล์ที่รายงานก่อนหน้าเป็นผล baseline เดิม ไม่ใช่ tests ที่รันใหม่สำหรับเอกสารชุดนี้
- Render Free อาจพักเมื่อไม่มีการใช้งาน ทำให้เริ่มเว็บช้า Dockerfile ข้าม tests จึงต้องตรวจ CI/test results ของ release แยก
- ฐาน Neon ใหม่ไม่ได้ย้ายข้อมูลเก่าใน Docker; แอปในเครื่องกับ Render ใช้ข้อมูลร่วมกันหากตั้ง datasource ไป Neon เดียวกัน
- การ reset รหัสฐานข้อมูลต้องปรับ credentials ของแอปที่เชื่อมทั้งหมด อย่าใส่ DB password, STAFF password, private key หรือ owner token ลงภาพ/เอกสาร
- Auto-Deploy ของ service ยังไม่ได้ยืนยันจากหลักฐานนี้ และ GitHub Actions ไม่มี deploy job
- หลัง merge develop เข้า main ให้ตั้ง Render branch เป็น main แล้ว deploy/test พร้อมบันทึก SHA และหลักฐานอีกครั้ง

ดู [README](../README.md), [Deployment diagram](diagrams/deployment.md) และ [Handover](handover.md)
