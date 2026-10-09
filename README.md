# Food Queue Notifier

โปรเจกต์ระบบแจ้งเตือนคิวรับอาหารและเครื่องดื่ม
รายวิชา CP353002 Principles of Software Design and Development

## สถานะปัจจุบัน

มีหน้าเมนู ตะกร้า ประวัติและติดตามคิว หน้าจอพนักงาน และ QR พร้อมฐานข้อมูล เมนู/ภาพเมนู ออเดอร์และเลขคิวรายวัน การเรียก API ที่ต้องใช้ token หรือสิทธิ์พนักงานจะตอบ 503 เมื่อยังไม่มี implementation ที่เกี่ยวข้อง ระบบยังรอส่วน State/security ของคมชาญ และ Subscription/Push/Log ของธีธัช จึงยังไม่ใช่ release ที่ผ่านการรับรองครบระบบ

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

Compose เปิด port เฉพาะ localhost และเก็บข้อมูลใน named volume `queue_restart_data` คำสั่ง `docker compose down` หยุด container โดยเก็บ volume ไว้ Docker image build ข้าม tests; ให้รันชุดทดสอบแยกก่อนส่งมอบ ไม่เปิดระบบต่อสาธารณะจนรวม security และตรวจ acceptance ครบ

## ทดสอบ

```bash
for test in test/js/*.test.cjs; do node "$test" || exit 1; done
mvn clean verify
```

PostgreSQL tests ใช้ embedded database แยกจากฐานข้อมูลแอป ไม่ต้องรัน Compose เพื่อทดสอบ CI ตรวจทั้ง JS และ Maven และเก็บ Surefire reports; ไม่มีการ deploy อัตโนมัติ

ผลตรวจที่ผ่านมาอยู่ใน [test report](doc/test-report.md) การทดสอบที่ใช้ access/token fixtures ยืนยันส่วนเมนูและออเดอร์ ไม่ใช่หลักฐานว่า production security หรือ Push ทำงานแล้ว

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
