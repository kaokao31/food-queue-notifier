# Food Queue Notifier

ระบบสั่งอาหารและแจ้งเตือนคิวรับอาหาร/เครื่องดื่ม สำหรับรายวิชา **CP353002 Principles of Software Design and Development**

ลูกค้าเลือกเมนู สั่งอาหาร และติดตามคิวด้วย token ของออเดอร์ พนักงานจัดการเมนูและเปลี่ยนสถานะคิว เมื่ออาหารพร้อม ระบบส่ง Web Push ไปยังเบราว์เซอร์ที่เชื่อมการแจ้งเตือนไว้

## สมาชิกและขอบเขตงาน

| ชื่อ–นามสกุล | รหัสนักศึกษา | Section | Branch | ส่วนรับผิดชอบ |
|---|---|---|---|---|
| นายกานดิทัต นามสุดตา | 673380392-1 | 3 | `kanditat_673380392-1_03` | หน้าเว็บ ตะกร้า ประวัติ/ติดตามคิว หน้าจอพนักงาน QR และ runtime/CI |
| นายอนันต์เอกก์ ใหญ่พงศกร | 673380430-9 | 3 | `anunake_673380430-9_03` | schema/migration, persistence, เมนู/ภาพเมนู, ออเดอร์ เลขคิวรายวัน และ OpenAPI |
| นายคมชาญ วรสาร | 673380396-3 | 3 | `komchan_673380396-3_03` | State, เปลี่ยนสถานะ/ยกเลิกคิว, token, สิทธิ์ออเดอร์, Security/CSRF และสิทธิ์ Swagger |
| นายธีธัช ลิ้มประยูรวงศ์ | 673380408-2 | 4 | `teetach_673380408-2_04` | Subscription, Observer, Strategy/sender, delivery, log และ Browser Push |

## สถานะที่ตรวจแล้ว

รวมงานตามแผน K/A/C/T ชุด 1–16 และงานเพิ่มเติมเรื่องรูปเมนูคงเดิมเมื่อเปลี่ยนชื่อ ข้อความแจ้งเตือนที่ใช้เลขคิวจริง การรับ Push configuration ผ่าน interface และ Swagger/OpenAPI พร้อมสิทธิ์ STAFF แล้ว

- ผล `mvn clean verify` ของ C-R17 ผ่าน **285 tests** ไม่มี Failures/Errors/Skipped เมื่อ 10 ตุลาคม 2026 บนเครื่องคมชาญ ก่อน merge PR #78
- ผล JavaScript ล่าสุดผ่าน **14 ไฟล์ทดสอบ** เมื่อแก้ UI รูปเมนู
- ทดสอบจริงแล้วว่ารูปเมนูคงเดิมหลังเปลี่ยนชื่อ/หมวดหมู่ และ Swagger เรียกเมนู ID 1 ได้ HTTP 200
- วันที่ 10 ตุลาคม 2026 ผู้ทดสอบยืนยันสร้างออเดอร์บน Neon และรับ Web Push ที่แสดงเลขคิวจริงถูกต้องบน URL ของ Render แล้ว ดู [Deployment test report](doc/deployment-test-report.md) ผลนี้ครอบคลุมรอบที่ทดสอบ ไม่ใช่การรับรอง popup ทุกอุปกรณ์

Deployment ที่ทดสอบใช้ `develop` commit `c07292e` หลัง PR #81 ใช้ Neon PostgreSQL และ Render Docker ผ่าน HTTPS แล้ว เอกสาร SOLID/Design Patterns และแบบจำลองได้รับการปรับใน PR #80–81 ยังต้องทำ final acceptance, จัดเตรียมสไลด์/หลักฐาน และ release เข้า `main` จึงยังไม่ถือเป็น final release

## เทคโนโลยีและโครงสร้าง

ใช้ Spring Boot 3.2.5, Spring MVC/Security/Data JPA, PostgreSQL 16, Flyway, Thymeleaf และ JavaScript ฝั่งเบราว์เซอร์ เอกสาร API ใช้ springdoc-openapi 2.5.0 และ Swagger UI ระบบ Push ใช้ Service Worker และ VAPID

ใช้ **JDK 21** สำหรับ build/run โดย compile target เป็น Java 17, Maven 3.9 และ Node.js 24 LTS สำหรับ JavaScript tests

| ตำแหน่ง | เนื้อหา |
|---|---|
| `code/src/main/java` | controller, service/interface, mapper, DTO, entity และ repository |
| `code/src/main/resources` | configuration, Flyway migration, template และ static assets |
| `test/java` | unit, MVC/Security และ PostgreSQL integration tests |
| `test/js`, `test/fixtures` | UI/Service Worker tests และข้อมูลจำลองเฉพาะการทดสอบ |
| `doc` | แบบจำลอง รายงานทดสอบ acceptance และคู่มือส่งต่อ |
| `tools/GenerateVapidKeys.java` | เครื่องมือสร้าง VAPID key pair สำหรับการตั้งค่าในเครื่อง |

คำขอจากหน้าเว็บผ่าน Controller → Service interface/implementation → Repository → PostgreSQL โดยใช้ DTO/Mapper สำหรับข้อมูล API การเปลี่ยนสถานะคิวใช้ State และเผยแพร่ event ภายใน transaction; Observer รับ event หลัง commit แล้วใช้ notification Strategy ส่งหรือแสดง preview

ดู [ER](doc/diagrams/er.md), [State](doc/diagrams/state.md), [Sequence สร้างออเดอร์](doc/diagrams/sequence-create.md), [Sequence READY](doc/diagrams/sequence-ready.md), [Sequence subscription](doc/diagrams/sequence-subscribe.md), [Security/State](doc/security-and-state.md), [SOLID](doc/solid-analysis.md) และ [Design Patterns](doc/design-patterns.md) เอกสารบางฉบับระบุ baseline เก่า ให้ตรวจวันที่และขอบเขตของแต่ละฉบับก่อนใช้อ้างอิง

## รันเว็บในเครื่อง

คำสั่งต่อไปนี้ใช้ Git Bash บน Windows โดยเปิด Docker Desktop ก่อน ปรับตำแหน่ง JDK ให้ตรงกับเครื่องตนเองหาก Java/Maven ยังไม่ใช้ JDK 21

```bash
cd "$USERPROFILE/food-queue-notifier-restart"
java -version
mvn -version

export STAFF_USERNAME="staff"
read -r -s -p "ตั้งรหัสพนักงานอย่างน้อย 12 ตัวอักษร: " STAFF_PASSWORD
echo
export STAFF_PASSWORD
export NOTIFICATION_MODE="console"

docker compose up -d db &&
mvn spring-boot:run
```

เปิด [หน้าลูกค้า](http://localhost:8080/) และ [หน้าล็อกอินพนักงาน](http://localhost:8080/staff/login) ใช้ username/password ที่ตั้งเองข้างต้น รหัสผ่านต้องมีอย่างน้อย 12 ตัวอักษรและไม่เกิน 72 UTF-8 bytes รหัสว่างทำให้ login ไม่ได้ ไม่มีรหัสผ่านเริ่มต้นที่ใช้งานได้ และข้อมูลพนักงานในระบบใช้ BCrypt

การรัน Maven ใช้ฐานข้อมูลเริ่มต้น `queuenotify_restart` ที่ `localhost:5433` โดย Compose เปิด PostgreSQL ที่ `127.0.0.1:5433` ผู้ใช้/รหัส `postgres` เป็นค่าเริ่มต้นสำหรับทดลองในเครื่อง ตั้งค่าเพิ่มเติมผ่าน `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT` ตาม [.env.example](.env.example) ถ้าเปลี่ยน credentials ของ Compose ต้อง export ค่าเดียวกันให้ Maven ด้วย เพราะ Maven ไม่โหลด `.env` อัตโนมัติ

Flyway ใช้ migration **V1–V6** และ Hibernate ใช้ `ddl-auto: validate` โดย V6 บันทึกรูปเริ่มต้นให้เมนูเดิมที่ยังไม่มีรูป เพื่อรักษารูปเมื่อเปลี่ยนชื่อ/หมวดหมู่และคงรูปอัปโหลดที่มีอยู่ สำรองข้อมูลก่อนใช้กับฐานเดิม และอย่าแก้ migration ที่รันไปแล้ว ดู [Migration guide](doc/migration.md) สำหรับ V1–V6

## Swagger / OpenAPI

1. Login ที่ [หน้าพนักงาน](http://localhost:8080/staff/login) ในเบราว์เซอร์เดียวกัน
2. เปิด [Swagger UI](http://localhost:8080/swagger-ui/index.html)
3. เลือก `GET /api/v1/menu-items` → **Try it out → Execute** เพื่อดูรายการ หรือใช้ ID จากรายการกับ `GET /api/v1/menu-items/{id}`

เอกสาร JSON อยู่ที่ [OpenAPI JSON](http://localhost:8080/v3/api-docs) และ YAML ที่ [OpenAPI YAML](http://localhost:8080/v3/api-docs.yaml) ทั้งหน้าเอกสารและไฟล์ที่เกี่ยวข้องอนุญาตเฉพาะ GET และ STAFF ผู้ที่ยังไม่ login จะถูกส่งไป `/staff/login`; ผู้ที่ login แต่ไม่มี STAFF จะได้รับ 403

สำหรับ POST/PUT/PATCH/DELETE ให้เรียก `GET /api/v1/csrf` ก่อน แล้วคัดลอกค่า `token` ไปที่ **Authorize → csrfToken** เพื่อส่ง header `X-CSRF-TOKEN` โดยต้องขอ token ใหม่หลัง login/logout เบราว์เซอร์ส่ง `JSESSIONID` ของ session ที่ login ให้อัตโนมัติ; Authorize ไม่ได้สร้าง session พนักงานให้

API ของออเดอร์ลูกค้าใช้ `X-Queue-Token` ที่คืนตอนสร้างออเดอร์แต่ละรายการ ผู้ใช้ STAFF มีสิทธิ์เข้าถึงตามบทบาท จึงไม่ควรใช้การทดสอบใน Swagger ขณะ login STAFF เป็นหลักฐานว่าลูกค้าไม่สามารถเข้าถึงออเดอร์อื่นได้ ให้ตรวจ owner isolation ผ่าน integration tests และหน้าลูกค้าด้วย

**Try it out สร้างหรือแก้ข้อมูลจริง** ให้ใช้ข้อมูลทดลอง สิทธิ์ในเอกสาร API เป็นคำอธิบาย; การบังคับสิทธิ์จริงอยู่ใน SecurityConfig และ services

## เปิด Web Push จริง

`NOTIFICATION_MODE=console` แสดง PREVIEW และไม่ส่งแจ้งเตือนไปเบราว์เซอร์ หากต้องการส่งจริงให้ใช้ `webpush` พร้อม VAPID key pair เดิมที่เบราว์เซอร์สมัครไว้

หากยังไม่มี key pair ให้สร้าง **ครั้งแรกเท่านั้น** ไว้ภายนอก repository:

```bash
java tools/GenerateVapidKeys.java "$USERPROFILE/Downloads/queue-notify-vapid.env"
```

เครื่องมือจะไม่เขียนทับไฟล์เดิม เก็บ private key ไว้ในเครื่องและไม่ commit หรือส่งในภาพหน้าจอ อย่าสร้าง key ใหม่ทุกครั้งที่เปิดเว็บ เพราะ subscription เดิมผูกกับ public key ตอนสมัคร

หลังหยุดเว็บเดิมด้วย `Ctrl+C` ให้ใช้ Git Bash หน้าต่างที่ตั้ง STAFF ไว้ แล้วโหลดและ export ค่า:

```bash
source "$USERPROFILE/Downloads/queue-notify-vapid.env"
export VAPID_PUBLIC_KEY VAPID_PRIVATE_KEY
read -r -p "อีเมลติดต่อสำหรับ VAPID: " vapid_contact_email
export VAPID_SUBJECT="mailto:$vapid_contact_email"
export NOTIFICATION_MODE="webpush"
mvn spring-boot:run
```

ต้องตั้ง `VAPID_SUBJECT` เป็นอีเมลจริงแทน placeholder ในไฟล์ การ `source` อย่างเดียวไม่ได้ export ตัวแปรใหม่ให้ Java จึงต้องใช้ `export` ตามตัวอย่าง

ทดสอบด้วยออเดอร์ใหม่: เปิดหน้าติดตามคิว → กดเปิด/เชื่อมแจ้งเตือน → อนุญาตเบราว์เซอร์ → พนักงานเปลี่ยนสถานะจน READY ข้อความใช้เลขคิวจริง เช่น **อาหารคิว 4 พร้อมแล้วเชิญรับอาหารได้เลย** Web Push ต้องใช้ secure context เช่น `localhost` หรือ HTTPS หากเปลี่ยน public key หรือ origin ให้สมัคร subscription ใหม่

ตรวจผลส่งในหน้าพนักงาน: PREVIEW ไม่ได้ส่ง provider; ACCEPTED/HTTP 201 หมายถึง provider รับคำขอ ยังไม่ยืนยันว่าอุปกรณ์แสดง popup ระบบส่งหนึ่งครั้งและไม่มี automatic retry ดู [Push delivery](doc/push-delivery.md) และ [Push test report](doc/push-test-report.md)

Standalone demo ที่ `/push-demo.html` เปิดเฉพาะ profile `push-demo` และ STAFF ต้องใช้ CSRF และเป็นการทดสอบแยกจากออเดอร์/production log

## ทดสอบ

```bash
node --test test/js/*.test.cjs

(
  unset NOTIFICATION_MODE
  mvn clean verify
)
```

คำสั่ง subshell ปิดค่า Web Push เฉพาะช่วงทดสอบ เพื่อให้ tests ที่ตรวจ default console ทำงานตรงตามเงื่อนไข แล้วคืน environment ของหน้าต่างเดิม PostgreSQL tests ใช้ embedded database แยกจากฐานแอป ไม่ต้องเปิด Compose เพื่อรัน tests

GitHub Actions ตรวจ JavaScript และ Maven พร้อมเก็บ Surefire reports โดย workflow ยังไม่มี deploy job การ deploy เว็บทำผ่าน Render แยกจาก workflow; ตรวจการตั้งค่า Auto-Deploy ของ service ก่อนใช้อ้างอิง ผลอัตโนมัติจำลอง provider และไม่ยืนยันการแสดงแจ้งเตือนจริงบนอุปกรณ์ ดู [Frontend tests](doc/frontend-test-report.md), [State tests](doc/state-test-report.md) และ [Push tests](doc/push-test-report.md); จำนวน tests ในรายงานเก่าเป็นผลตาม baseline ของรายงานนั้น

## Docker และการเผยแพร่

```bash
docker compose config --quiet
docker compose up --build -d
docker compose logs app
```

Compose เปิด port เฉพาะ localhost และเก็บฐานข้อมูลใน volume `queue_restart_data` โดย `docker compose down` เก็บ volume ไว้ Docker image build ข้าม tests จึงต้องตรวจ tests แยกก่อนส่งมอบ

**Compose app ปัจจุบันรับเฉพาะ DB/PORT** ยังไม่ได้ส่ง STAFF, VAPID, notification mode หรือ profile ให้ container การรันทั้งแอปด้วย Compose จึงยังไม่ใช่ขั้นตอนตั้งค่า STAFF/Web Push ที่ครบ ให้ใช้วิธี Maven ข้างต้นสำหรับการทดลองฟีเจอร์เหล่านี้ หรือปรับ environment ของ container ก่อนใช้

### เว็บออนไลน์ที่ทดสอบแล้ว

- [หน้าลูกค้า](https://food-queue-notifier-1.onrender.com/)
- [หน้าล็อกอินพนักงาน](https://food-queue-notifier-1.onrender.com/staff/login)
- [Swagger UI](https://food-queue-notifier-1.onrender.com/swagger-ui/index.html) — ต้อง login STAFF ก่อน

ใช้ **Render Web Service / Docker / Free / Singapore** และ **Neon PostgreSQL 16 / Free / Singapore** บน branch ฐานข้อมูล `production`, database `neondb` ชื่อนี้เป็นชื่อ branch ของ Neon ไม่ใช่สถานะ final release ของโค้ด แอปที่ตรวจใช้ branch Git `develop`, SHA `c07292e`

Render ใช้ Dockerfile ที่ราก repository และ Root Directory ว่าง ตั้งค่าต่อไปนี้ใน Environment ของ service:

| ตัวแปร | ค่า/วิธีตั้ง |
|---|---|
| `DB_URL` | `jdbc:postgresql://<NEON_DIRECT_HOST>:5432/neondb?sslmode=require&channelBinding=require` |
| `DB_USERNAME` | role ของ Neon ที่ใช้เชื่อมต่อ |
| `DB_PASSWORD` | รหัสผ่าน role ของ Neon |
| `STAFF_USERNAME` | ชื่อบัญชีพนักงาน |
| `STAFF_PASSWORD` | รหัสใหม่อย่างน้อย 12 ตัวอักษรและไม่เกิน 72 UTF-8 bytes |
| `NOTIFICATION_MODE` | `webpush` |
| `VAPID_PUBLIC_KEY` / `VAPID_PRIVATE_KEY` | key pair เดียวกันที่ทดสอบสำเร็จ |
| `VAPID_SUBJECT` | `mailto:<CONTACT_EMAIL>` โดยใช้อีเมลจริง |

ใช้ Neon direct connection สำหรับชุดนี้ เพราะ Flyway ใช้ datasource เดียวกันในการ migrate แอปรับ `PORT` จาก Render; log deployment ที่ตรวจแสดง port 10000 ไม่ต้องใส่ credentials ลงโค้ดหรือ commit ไฟล์ secrets

Flyway V1–V6 บน Neon ผ่านแล้ว การสร้างฐานใหม่ไม่ได้ย้ายข้อมูลเดิมจาก Docker ไปด้วย Maven ในเครื่องและ Render จะอ่าน/เขียนข้อมูลร่วมกันเมื่อชี้ Neon database เดียวกัน

เมื่อเปลี่ยนจาก localhost เป็น URL นี้ ให้สร้างออเดอร์ใหม่และสมัครแจ้งเตือนสำหรับ origin ใหม่ ผลทดสอบที่ผู้ใช้ยืนยันคือสร้างออเดอร์และรับแจ้งเตือนเลขคิวถูกต้อง Render Free อาจพักเมื่อไม่มีการใช้งาน ทำให้การเปิดเว็บครั้งแรกต้องรอ

หลังเอกสาร/สไลด์และ final acceptance ครบ ให้เปิด PR `develop` → `main` แล้วเปลี่ยน Render ให้ deploy revision จาก `main` พร้อมทดสอบและบันทึก SHA อีกครั้ง การ merge Git ไม่ได้เปลี่ยน branch ที่ Render ติดตามให้เอง

ดู [Deployment diagram](doc/diagrams/deployment.md), [Deployment test report](doc/deployment-test-report.md) และ [Acceptance](doc/acceptance.md)

## Development workflow

- `main`: รุ่นพร้อมส่งมอบ
- `develop`: รวมงานระหว่างพัฒนา
- สมาชิกทำงานและ push บน branch ของตนเอง
- รวมงานผ่าน Pull Request และให้สมาชิกอีกคน review/approve ก่อน merge

ดู [Handover](doc/handover.md) และ [Team review](doc/team-review.md) โดยตรวจ baseline ของแต่ละเอกสาร งานถัดไปคือปรับเอกสารประกอบที่ยังล้าสมัย อัปเดตหลักฐานการเผยแพร่ ทำ final acceptance และจัดเตรียมสไลด์/หลักฐานส่งงาน
