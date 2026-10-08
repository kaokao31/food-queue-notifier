# ส่งต่องาน Food Queue Notifier

สถานะ 9 ตุลาคม 2026: รุ่นรวมใน working tree มี A17/K17 และ Repository interface follow-up; full PostgreSQL verify ผ่าน 28 tests เวลา 01:27:59 +07:00. กานดิทัต push K01–K05 ถึง c3f891d และอนันต์เอกก์ push A01–A05 ถึง 4a3290c แล้วตามผล Git ที่ทีมส่ง. K06/A06 อยู่ระหว่างตรวจเอกสาร ยังไม่รวมสอง branch ผ่าน Git, ไม่ได้ merge/deploy รุ่นส่งมอบ และยังต้องทดสอบ commit ฉบับรวมก่อน PR merge.

ทีมยืนยัน Android รับแจ้งเตือนขณะล็อกจอแล้ว แต่ยังต้องเก็บ URL/รุ่นที่ทดสอบและตรวจแยกสองเครื่อง/สองออเดอร์บน URL รุ่นส่งมอบ. สไลด์จะทำร่วมกันหลังงานระบบครบและ commit/push ภายหลัง ไม่รวมใน K06.

## งานที่มีแล้ว

- หน้าเลือกอาหารพร้อมหมวดหมู่ เรียงลำดับ แบ่งหน้า ตะกร้า และยืนยันออเดอร์.
- หน้าติดตามคิว แก้จำนวนก่อนเริ่มปรุง ยกเลิกตามสถานะ และสมัครแจ้งเตือนเฉพาะออเดอร์ของเครื่อง.
- หน้าล็อกอินพนักงาน จัดการคิว จัดการเมนู และสร้าง QR จาก URL HTTPS ของร้าน.
- PostgreSQL/Flyway V1–V5, snapshots ชื่อ/ราคา, counter เลขคิวรายวัน Asia/Bangkok, shared PK Order–Queue และข้อมูล subscription/log ที่คงอยู่ใน DB.
- Menu/Order CRUD, DTO/Mapper, State/Observer/Strategy, token เจ้าของคิว, BCrypt และ CSRF.
- tests, README, Data Dictionary, SOLID, Pattern analysis, diagrams และ checklist ทดสอบจริง; เอกสารยังต้องตรวจให้ตรงรุ่นสุดท้ายและ rubric. สไลด์ยังไม่รวมในการส่งมอบรอบนี้.

ผลตรวจและข้อจำกัดทั้งหมดอยู่ใน [Test Report](test-report.md) และ [Acceptance](acceptance.md). ภาพใน `img/` เป็นหลักฐาน localhost; QR example ไม่ใช่ URL ใช้งานจริง.

## ลำดับที่ทีมต้องทำต่อ

1. ตรวจ `mvn clean verify` รุ่นล่าสุดให้ผ่านบน PostgreSQL ชั่วคราว และเก็บรายงาน `target/surefire-reports/`.
2. แต่ละคนตรวจขอบเขตของตนตาม [แผน review](team-review.md), อธิบายโค้ดและปรับแก้จากผลตรวจจริง.
3. ตรวจ staged diff ไม่รวม `.env`/private key/build outputs; เจ้าของบัญชี commit/push เอง. PR เข้า develop ให้เพื่อน review จริง.
4. Deploy รุ่นรวมบน Render ตั้ง DB connection จาก Render, STAFF_PASSWORD ของร้าน และ VAPID เดิม. ตรวจ health กับ Swagger และหน้าเมนู.
5. ทดสอบบนสองเครื่อง: สั่งคนละออเดอร์ เปิด Push แล้วล็อกจอเครื่อง A; พนักงานกด READY เฉพาะ A. ตรวจว่า A ได้ข้อความคิวถูกต้องและ B ไม่ได้. ตรวจ log แยกจากการแสดงแจ้งเตือนจริง.
6. ใส่ URL จริงใน README/QR เก็บภาพและผลทดสอบใหม่ ปรับสไลด์ให้ตรงผล แล้ว release เข้า main ผ่าน PR.

## ข้อตกลงเรื่อง commit

ประวัติ author ของ commit เดิมต้องคงไว้ตามจริง ไม่แบ่งชื่อ author ย้อนหลังตามขอบเขตรับผิดชอบใหม่. สมาชิกใช้บัญชีตนสำหรับงานที่ตนตรวจ ปรับ ทดสอบ และอธิบายได้จริง. ห้าม empty commits หรือเปลี่ยนเวลาเพื่อเติมจำนวนตามเกณฑ์.

งานหลายชุดพึ่งกันทั้ง schema/security/service/UI; อย่าแยกไฟล์ตามชื่อคนแล้วถือว่าทุก commit build ได้ทันที. เริ่มจากบันทึกฐานระบบที่รวมแล้วอย่างตรงไปตรงมา จากนั้นแยก PR ของการปรับปรุงและผล review จริงตามขอบเขต 4 คน. ทุก PR ระบุพฤติกรรมที่เปลี่ยน ผลทดสอบ และข้อจำกัด.

## การรันในเครื่อง

แก้ค่าจริงใน `.env` ไม่ใช่ `.env.example`; STAFF_PASSWORD ต้องยาวอย่างน้อย 12 ตัว. ใน Git Bash ที่โฟลเดอร์ repository:

```bash
set -a
source .env
set +a
docker compose up -d db
DB_URL=jdbc:postgresql://localhost:5433/queuenotify DB_USERNAME=postgres DB_PASSWORD=postgres mvn spring-boot:run
```

ค่า postgres ในคำสั่งนี้เป็นค่าเริ่มต้นของ Docker DB ในเครื่องเท่านั้น ถ้าเปลี่ยนรหัส Docker แล้วต้องใช้ค่าที่ตั้งเอง. ห้ามนำ connection นี้ไปแทน Render. เปิด `http://localhost:8080/` และ `/staff/login`; ห้ามส่งรหัสผ่านหรือ `.env` ลงแชต.
