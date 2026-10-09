# Food Queue Notifier

โปรเจกต์ระบบแจ้งเตือนคิวรับอาหารและเครื่องดื่ม
รายวิชา CP353002 Principles of Software Design and Development

## สถานะปัจจุบัน

มีหน้าเมนู ตะกร้า ประวัติ/ติดตามคิว หน้าจอพนักงานและ QR พร้อม persistence เมนู/ภาพเมนู ออเดอร์และเลขคิวรายวัน รวม C1–C11 สำหรับ token, owner/STAFF access, BCrypt login, CSRF, State และ Queue API แล้ว รวม T1–T15 สำหรับ subscription, Browser Push, AFTER_COMMIT observer, single-attempt delivery, STAFF log และ standalone demo แล้ว เอกสาร T16 อ้างอิง baseline `76a788a`; ผล Java full suite ล่าสุดผ่าน 247 tests บนเครื่องธีธัช ยังเหลือคมชาญ C12–C16 การตรวจอุปกรณ์จริงและ acceptance/deployment ของทีม จึงยังไม่ใช่ release ที่ผ่านการรับรองครบระบบ

## โครงสร้าง

- `code/src/main/java`: controller, service, mapper, DTO, entity และ repository
- `code/src/main/resources`: configuration, Flyway migration, template และ assets
- `test/java`: Java/MVC/PostgreSQL tests
- `test/js`, `test/fixtures`: การทดสอบ UI และ API fixtures เฉพาะชุดทดสอบ
- `doc`: แบบจำลอง ข้อกำหนดรับงาน และคู่มือส่งต่อ

## รันในเครื่อง

ใช้ JDK 21 สำหรับ build (โค้ด compile target Java 17), Maven 3.9 และ Node.js สำหรับ JS tests

```bash
docker compose up -d db
mvn spring-boot:run
```

เปิด http://localhost:8080/ ฐานข้อมูล default คือ `queuenotify_restart` บน localhost:5433 ผู้ใช้/รหัสผ่าน `postgres` สำหรับการทดลองในเครื่อง การตั้งค่าใช้ environment `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT` ตาม `.env.example`; การรัน Maven ไม่อ่าน `.env` อัตโนมัติ ถ้าเปลี่ยนค่าใน Compose ให้ export ค่าเดียวกันก่อนรัน Maven

ใช้ฐานข้อมูลสำหรับงานรอบนี้โดยเฉพาะและสำรองก่อนนำ migration ไปใช้กับข้อมูลเดิม Flyway ทำ migration V1–V5 และ Hibernate ตรวจ schema ด้วย `ddl-auto: validate`

## รันทั้งแอปด้วย Docker

```bash
docker compose config --quiet
docker compose up --build -d
docker compose logs app
```

Compose เปิด port เฉพาะ localhost และเก็บข้อมูลใน named volume `queue_restart_data` คำสั่ง `docker compose down` หยุด container โดยเก็บ volume ไว้ Docker image build ข้าม tests; ให้รันชุดทดสอบแยกก่อนส่งมอบ ตรวจการส่ง STAFF/VAPID/profile environment ให้ container และ acceptance ก่อนเผยแพร่; Compose ปัจจุบันยังไม่ได้ส่งค่าเหล่านี้ให้ app

## ทดสอบ

```bash
for test in test/js/*.test.cjs; do node "$test" || exit 1; done
mvn clean verify
```

PostgreSQL tests ใช้ embedded database แยกจากฐานข้อมูลแอป ไม่ต้องรัน Compose เพื่อทดสอบ CI ตรวจทั้ง JS และ Maven และเก็บ Surefire reports; ไม่มีการ deploy อัตโนมัติ

ผลตรวจเดิมอยู่ใน [test report](doc/test-report.md) และผลรวม Push/owner/security ใหม่อยู่ใน [Push test report](doc/push-test-report.md) Module tests ที่ใช้ access/token fixtures ยังยืนยันเฉพาะส่วนธุรกิจ ส่วน full API tests ใหม่ใช้ security/CSRF/owner implementation จริงกับ PostgreSQL และจำลองเฉพาะ provider; ไม่มีหลักฐานว่าอุปกรณ์แสดง Push จริงจากผลอัตโนมัติ

## เจ้าของงาน

| สมาชิก | ส่วนรับผิดชอบ |
|---|---|
| กานดิทัต | หน้าเว็บ ตะกร้า ประวัติ/ติดตามคิว หน้าจอพนักงาน QR และ runtime/CI |
| อนันเอก | schema/migration, persistence, เมนู/ภาพเมนู, ออเดอร์และเลขคิวรายวัน |
| คมชาญ | State, เปลี่ยนสถานะ/ยกเลิกคิว, token, สิทธิ์ออเดอร์, security และ CSRF |
| ธีธัช | Subscription, Observer, Strategy/sender, delivery, Log และ Browser Push |

## Development workflow

- main: รุ่นพร้อมส่งมอบ
- develop: รวมงานระหว่างพัฒนา
- สมาชิกทำงานบน branch ส่วนตัว
- รวมงานผ่าน Pull Request และมีสมาชิกอีกคนรีวิว

ดู [handover](doc/handover.md), [acceptance](doc/acceptance.md), [ER](doc/diagrams/er.md) และ [migration](doc/migration.md) สไลด์จะจัดทำกับทีมภายหลัง

## ตั้งค่าแจ้งเตือนและพนักงาน

ดู [Push delivery/handover](doc/push-delivery.md), [sequence READY](doc/diagrams/sequence-ready.md) และ [sequence subscribe](doc/diagrams/sequence-subscribe.md)
`NOTIFICATION_MODE=console` เป็น preview และค่า `webpush` ใช้ VAPID settings จาก environment รหัสผ่าน STAFF ว่างทำให้ล็อกอินไม่ได้ ไม่มี default credential ที่ใช้งานได้
Demo เปิดเฉพาะ profile `push-demo` และ STAFF ที่ `/push-demo.html`; ต้องใช้ CSRF และเป็น provider test แยกจากข้อมูลออเดอร์/production log
PREVIEW ไม่ส่ง provider ส่วน ACCEPTED หมายถึง provider รับคำขอเท่านั้น ไม่ยืนยัน device display
