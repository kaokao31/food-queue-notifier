# พร้อมรับ — Food Queue Notifier

ระบบสั่งอาหารและแจ้งเตือนคิวผ่าน Browser Web Push ลูกค้าสแกน QR เลือกอาหาร รับเลขคิว และเปิดแจ้งเตือนโดยไม่ต้องกรอกชื่อ เบอร์โทร หรือสมัครสมาชิก พนักงานจัดการเมนูและเลื่อนสถานะคิว เมื่ออาหารพร้อมระบบส่ง Push เฉพาะเครื่องที่ผูกกับออเดอร์นั้น รองรับ Android Chrome เป็นขอบเขตทดสอบหลัก

## สมาชิกและขอบเขตรับผิดชอบสำหรับ review/พัฒนาต่อ

ตารางนี้เป็นขอบเขตรับผิดชอบสำหรับตรวจและพัฒนาต่อที่ทีมตกลงล่าสุด ไม่ใช่หลักฐานว่าแต่ละคนเขียนโค้ดตั้งต้นทั้งหมด ประวัติ contribution ต้องอ้าง Git/PR จริง

| คน | ชื่อ | รหัส | Sec | Branch | ขอบเขต |
|---|---|---|---|---|---|
| 1 | กานดิทัต นามสุดตา | 673380392-1 | 3 | kanditat_673380392-1_03 | หน้าเว็บลูกค้า/พนักงาน, ตะกร้า/ประวัติคิว, QR และ Deployment |
| 2 | อนันต์เอกก์ ใหญ่พงศกร | 673380430-9 | 3 | anunake_673380430-9_03 | Schema, เมนู/ภาพ, ออเดอร์, DTO/Mapper และเลขคิวรายวัน |
| 3 | คมชาญ วรสาร | 673380396-3 | 3 | komchan_673380396-3_03 | State, Queue, token เจ้าของบิลและสิทธิ์พนักงาน |
| 4 | ธีธัช ลิ้มประยูรวงศ์ | 673380408-2 | 4 | teetach_673380408-2_04 | Observer, Subscription, Web Push Sender, Strategy และ Log |

## Tech Stack / Architecture

Java 17+, Spring Boot 3.2.5, Maven, Spring MVC, Thymeleaf, JavaScript/CSS, Spring Security, JPA/Hibernate, PostgreSQL, Flyway, springdoc Swagger, JUnit 5/Mockito/Spring Boot Test.

Controller → Service interface → implementation → Repository → Entity. DTO/Mapper แยกจาก persistence. State กำหนดการเปลี่ยนคิว; Observer ฟัง READY หลัง commit; Strategy เลือก Web Push จริงหรือ local console preview.

## System Architecture

Presentation (Thymeleaf/REST Controllers) → Service interfaces/implementations → Repositories → PostgreSQL. DTO/Mapper แยก HTTP contract ออกจาก Entity; Spring Security ดูแล staff session และ CSRF. Order transaction บันทึก order/items/queue และ counter รายวันพร้อมกัน. READY event ส่งหลัง commit ผ่าน Observer/Strategy และบันทึกผลส่ง. QrController รับ HTTP และเรียก QrService เพื่อ validate URL/สร้าง PNG ในเครื่อง โดยไม่ส่ง URL ออกบริการภายนอก.

ดู [Component](doc/diagrams/component.md), [Class/Patterns](doc/diagrams/class.md) และ [Deployment](doc/diagrams/deployment.md).

## Database Design

6 ตารางหลัก: menu_item, orders, order_item, queue, push_subscription, notification_log; ตารางเสริม menu_item_image (V4) และ queue_daily_counter (V5); เก็บ legacy customer/notification_preference รวม 10 ตารางแอป. Order 1:1 Queue ใช้ shared PK และ Order 1:N OrderItem. เลขคิวเริ่ม 1 ต่อวัน Asia/Bangkok ใช้ UNIQUE(queue_date,queue_number); token/order ID เดิมยังใช้ได้ข้ามวัน. ระบบใหม่ไม่เก็บข้อมูลลูกค้าใน legacy tables.

ดู [ER](doc/diagrams/er.md), [Data Dictionary](doc/data-dictionary.md), [migration policy](doc/migration.md).

## Installation & Setup

ต้องมี JDK 17+, Maven และ Docker Desktop ที่เปิด Linux engine อยู่

```bash
[ -f .env ] || cp .env.example .env
# เติม STAFF_PASSWORD อย่างน้อย 12 ตัว และ VAPID ลง .env
docker compose up -d db
```

เครื่องที่ยังไม่มี VAPID ใช้ `java tools/GenerateVapidKeys.java` ได้ แต่ utility จะไม่เขียนทับ .env เดิม ให้จัดการการสร้างกุญแจก่อนคัดลอกไฟล์ตัวอย่างหรือใช้คู่เดิมที่ตั้งไว้แล้ว ห้ามส่ง private key ไปแชต/commit

## How to Run

Git Bash:

```bash
set -a
source .env
set +a
export DB_URL='jdbc:postgresql://127.0.0.1:5433/queuenotify'
export DB_USERNAME='postgres'
export DB_PASSWORD='postgres'
mvn spring-boot:run -Dspring-boot.run.profiles=push-demo
```

หรือ `docker compose up --build` สำหรับ DB+app ใช้พอร์ตเว็บ 8080 และฐานข้อมูลฝั่ง host 5433

- ลูกค้า: http://localhost:8080/
- หน้าคิว: `/queue/{orderId}` (ต้องใช้เครื่องที่มี token)
- พนักงาน: `/staff/login` ใช้ STAFF_USERNAME/STAFF_PASSWORD ที่ตั้งเอง
- จัดการคิว: `/staff`
- จัดการเมนู: `/staff/menu`
- Push demo เดิม: `/push-demo.html` เฉพาะ profile push-demo

## API Documentation

Swagger: http://localhost:8080/swagger-ui.html และ `/v3/api-docs`.

CRUD เมนู: GET/POST `/api/v1/menu-items`, GET/PUT/DELETE `/api/v1/menu-items/{id}`.

พนักงานเลือกภาพเองได้ในฟอร์มเพิ่ม/แก้ไขเมนู รองรับ JPG/PNG ไม่เกิน 2 MB, ไม่เกิน 6000 พิกเซลต่อด้านและ 12 ล้านพิกเซล มีตัวอย่างก่อนบันทึก ภาพเก็บใน PostgreSQL จึงคงอยู่หลัง restart/deploy. เมนูและภาพบันทึกใน transaction เดียว; ไม่เลือกภาพจะคงภาพเดิมหรือใช้ภาพสำรองตามชื่อ/หมวดหมู่. ปุ่มลบอยู่ในฟอร์มแก้ไขและถามยืนยันก่อนลบ; เมนูที่มีประวัติออเดอร์ให้ปิดขายแทน.

API พร้อมภาพ: POST `/api/v1/menu-items/with-image`, PUT `/api/v1/menu-items/{id}/with-image` ใช้ multipart parts `menu` (application/json ตาม MenuItemRequest) และ `file` (ภาพ). ต้อง login พนักงานและส่ง CSRF token; GET `/api/v1/menu-items/{id}/image` อ่านภาพสาธารณะ. MenuItemResponse มี `imageUrl` ที่เปลี่ยนเมื่อเปลี่ยนภาพ.
CRUD ออเดอร์: POST/GET `/api/v1/orders`, GET/PUT/DELETE `/api/v1/orders/{id}`.
คิว: GET `/api/v1/queues/{id}`, PATCH `/{id}/advance`, PATCH `/{id}/cancel`.
Subscription: PUT/DELETE `/api/v1/orders/{id}/push-subscription`.
Push public key: GET `/api/v1/push/public-key`.
Log พนักงาน: GET `/api/v1/notifications?queueId={id}`.

- GET list รองรับ page/size/sort โดย allowlist และ size ไม่เกิน 100
- ลูกค้าใช้ `X-Queue-Token` สำหรับออเดอร์ของตัวเอง Token ไม่อยู่ใน URL และเก็บ hash ใน DB
- พนักงานใช้ session login; ไม่ให้ลูกค้าดูรายการออเดอร์ทั้งหมด เขียนเมนู ลบออเดอร์หรือ advance
- ทุก mutation ต้องมี CSRF: GET `/api/v1/csrf` แล้วส่ง headerName/token ที่ได้รับ (Swagger ใช้ browser session และต้องเติม header ผ่าน client หากลอง mutation)
- Create 201, read/update 200, delete 204, validation 400, unauthorized 401, forbidden 403, missing 404, conflict 409

## How to Run Tests

```bash
mvn clean verify
```

Integration tests ใช้ PostgreSQL จริงแบบชั่วคราวด้วย embedded-postgres ไม่ต้องเข้าถึง Docker หรือ Render ดาวน์โหลด native binaries ครั้งแรก ฐานข้อมูลทดสอบแยกจากข้อมูลใช้งาน ดูผลจริงใน [Test Report](doc/test-report.md) และ `target/surefire-reports/`.

ถ้า Windows sandbox เริ่ม embedded server ไม่ได้ ใช้เฉพาะฐาน container ใหม่ที่ localhost:55433/team_integration_test:

```bash
mvn -Dtest.postgres.url=jdbc:postgresql://127.0.0.1:55433/team_integration_test clean verify
```

ค่า default ของ test helper คือ integration_test/integration_test_local สำหรับฐานทดสอบนี้เท่านั้น. Helper ปฏิเสธ URL อื่น สร้าง schema สุ่มใหม่และไม่ลบ schema เก่า. ไม่ชี้คำสั่งนี้ไป DB_URL ของแอป/Render หรือฐาน V2 เดิม. ดูขอบเขต A17/จุดรอ review ใน [Anunake review](doc/anunake-integration-review.md).

## Deployment URL / Render

URL ของบริการที่ deploy แล้วให้ทีมใส่จาก Render Dashboard หลัง deploy รุ่นนี้และตรวจด้วยตนเอง ห้ามเดา URL จากชื่อ service. รุ่นก่อนมี Web Push demo Live แล้ว แต่ยังไม่ถือว่ารุ่น anonymous ordering นี้ deploy ผ่าน

Docker / same region as PostgreSQL / branch develop ระหว่างรวมงาน; รุ่นส่งมอบต้อง merge main ผ่าน PR และปรับ branch deploy ให้ตรงรุ่นนั้น

ตั้ง DB_URL เป็น JDBC internal hostname, DB_USERNAME, DB_PASSWORD, STAFF_USERNAME, STAFF_PASSWORD, VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, VAPID_SUBJECT และ SPRING_PROFILES_ACTIVE=push-demo ถ้าต้องการ demo เดิม Health path `/actuator/health`.

Render Free อาจพักเมื่อ idle และ free DB หมดอายุ 30 วัน ไม่ใช้รับรองความพร้อมระดับ production. Android ต้องใช้ HTTPS จริง localhost บนมือถือหมายถึงมือถือเอง

## Project Structure

```text
code/src/main/java/com/kku/queuenotify/  controller, service, repository, entity, dto, mapper
code/src/main/resources/              templates, static, db/migration, application.yml
test/java/                           tests
doc/diagrams/                        diagrams และ use-case descriptions
doc/slide/                           presentation
img/                                 evidence
tools/                               VAPID utility
pom.xml, Dockerfile, docker-compose.yml
```

## ข้อจำกัดและการใช้ข้อมูล

- ไม่มีชื่อ/โทรศัพท์ลูกค้า แต่เก็บออเดอร์และ subscription ทางเทคนิค ต้องวางนโยบายลบข้อมูลหลังจบการใช้งานกับร้านจริง
- ต้องอนุญาต notification; browser/device/network อาจหน่วงหรือไม่แสดง ใช้ polling เมื่อเปิดหน้าเว็บเป็น fallback
- การส่ง READY รุ่นนี้เป็น single attempt มี log ไม่มี auto retry/recovery; PENDING หลัง crash ต้องตรวจโดยทีม
- ACCEPTED คือ push provider รับคำขอ ไม่ใช่ delivery receipt; Console PREVIEW ไม่ใช่ Push จริง
- มีการป้องกันกดซ้ำขณะ request แต่ POST orders ยังไม่มี Idempotency-Key; หากเครือข่ายขาดอย่ากดสั่งซ้ำอัตโนมัติ
- Queue token อยู่ใน localStorage เครื่องเดิม ไม่มีการกู้คืน token ข้ามเครื่อง
- ลูกค้าแก้อาหารได้เฉพาะ WAITING, cancel WAITING/PREPARING, พนักงานลบได้เฉพาะ WAITING ไม่มี notification log
- เมนูที่มีออเดอร์อ้างอิงลบไม่ได้ ให้ปิดขายแทน

## เอกสารและส่งมอบ

[Patterns](doc/design-patterns.md) · [SOLID](doc/solid-analysis.md) · [API/Acceptance](doc/acceptance.md) · [ขอบเขต review 4 คน](doc/team-review.md)

ทุกคนตรวจ เข้าใจ และ commit/push ด้วยบัญชีตนเองตามงานจริง ไม่สร้าง contribution ย้อนหลังหรือ empty commits. รายงานแยกผล automated tests จากการทดสอบ Push บนอุปกรณ์จริง

### QR หน้าร้าน

พนักงานเข้าสู่ระบบแล้วเปิด `/staff/qr` ใส่ URL HTTPS หน้าเมนูจริง เช่น URL ที่ Render แสดง กดสร้างและพิมพ์ QR สำหรับวางบนโต๊ะ. ทุกโต๊ะใช้หน้าเมนูเดียวกันในรุ่นนี้ ไม่มีเลขโต๊ะหรือข้อมูลลูกค้า. QR สร้างภายในแอปด้วย [ZXing](https://github.com/zxing/zxing) ไม่ใช้ API ภายนอก. ตรวจการสแกนบนโทรศัพท์ก่อนพิมพ์.

ฐานข้อมูล Docker เปิดพอร์ตเครื่อง 5433 เพื่อไม่ชน PostgreSQL ที่ติดตั้งใน Windows. แอปใน Compose ใช้ `db:5432` ภายใน Docker. `.env.example` เก็บเฉพาะตัวอย่าง ให้ใส่รหัสจริงใน `.env` ที่ Git ignore. Spring Boot ไม่โหลด `.env` เอง ถ้ารัน Maven ใน Git Bash ให้ `set -a; source .env; set +a` ก่อน.

สไลด์นำเสนอเลื่อนไปจัดทำร่วมกันหลังงานระบบและเอกสารครบ ยังไม่รวมใน commit ชุดนี้ ทีมจะตรวจผลล่าสุดใน Test Report ก่อนจัดทำและ commit/push สไลด์ภายหลัง.

เริ่มส่งต่องานจาก [Handover](doc/handover.md): สถานะระบบ ลำดับ deploy/test และขอบเขตงาน 4 คน.

ประวัติคิวของลูกค้า: หน้าเมนูมีปุ่ม “ประวัติคิวที่เคยสั่ง” แสดงทุกบิลที่มี queue token เก็บอยู่ใน localStorage ของเบราว์เซอร์นี้ เรียงใหม่ก่อนเก่า หน้าละ 5 บิล พร้อมเลขคิว สถานะ รายการ ยอดรวม และลิงก์ดูรายละเอียด. อ่านแต่ละบิลผ่าน token ของบิลนั้น ไม่เปิด API รายการออเดอร์ของร้านให้ลูกค้า. ประวัติเดิมที่ยังมี token จะปรากฏด้วย; การล้างข้อมูลเว็บ/เปลี่ยนเบราว์เซอร์/เปลี่ยน origin จะไม่เห็นประวัติเดิม.

เลขคิวรายวัน (V5): เริ่ม 001 ใหม่เมื่อวันตาม Asia/Bangkok เปลี่ยน โดยใช้ `queue_daily_counter` ใน transaction เดียวกับการสร้างออเดอร์ รองรับคำขอพร้อมกันและ rollback. `queue_date` คู่กับ `queue_number` ต้องไม่ซ้ำ; Order ID/token/ลิงก์ยังไม่ซ้ำตลอด. คิวค้างข้ามวันและประวัติเดิมยังอยู่ หน้า tracking/history/staff และข้อความ Push แสดงวันที่คิว. เกิน 999 นับต่อ 1000 ไม่วนกลับ. Migration รักษาเลขคิวเดิม เติมวันที่จาก created_at UTC เป็นวันไทย และตั้งตัวนับของแต่ละวันที่เลขสูงสุดเดิม; วันนี้ที่มีข้อมูลแล้วจะนับต่อ ไม่รีเซ็ตคิวกลางวัน. ไม่ต้องมี cron หรือปุ่มรีเซ็ต; วันใหม่เริ่มเมื่อมีออเดอร์แรก.
