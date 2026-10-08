# Test Report

## ยืนยัน full PostgreSQL รุ่นรวม — 9 ตุลาคม 2026 01:27:59 +07:00

กานดิทัตรัน `mvn clean verify` จาก Git Bash ใน C:/food-queue-notifier หลังรวม A17/K17 และ Repository interface follow-up: **28 tests, failures 0, errors 0, skipped 0, BUILD SUCCESS**, รวม 47.859 วินาที. SystemIntegrationTest 19 ผ่าน; MigrationTest ตรวจ V1 upgrade และ Hibernate validate รวมอยู่ในชุด. ผู้ review ตรวจจำนวนใน Surefire XML ตรงกับผลที่ผู้ใช้ส่งมา.

ผลนี้แทนสถานะรอ full verification ของรอบ sandbox ด้านล่าง เป็นผลของ working tree รุ่นรวม ไม่ใช่ผลทดสอบ Git tree หลังรวม branch และไม่ใช่หลักฐาน Render/Android/remote CI หรือ GitHub PR review. ต่อมาทีม push K01–K05 ถึง c3f891d และ A01–A05 ถึง 4a3290c แล้ว; K06/A06 ยังตรวจเอกสารอยู่. ต้องทดสอบ commit ฉบับรวมอีกครั้งก่อน merge/deploy.

## ประวัติ review และ K17 — ก่อนผล full verify เวลา 01:27:59

รวมส่วน A จาก ANUNAKE-PR-BASE-REVIEW.zip และทำ K17 แล้ว: QrController inject QrService interface; URL validation/PNG encoding อยู่ QrServiceImpl. Review A17 เพิ่ม DailyQueueCounterRepository interface และ JdbcDailyQueueCounterRepository implementation โดยคง SQL และ MANDATORY transaction เดิม. V1–V5 ไม่เปลี่ยน.

`mvn -o -DskipTests package` BUILD SUCCESS เวลา 01:24:25 +07:00: compile production/tests และสร้าง executable JAR ผ่าน แต่คำสั่งนี้ข้ามการรัน tests.

QR smoke ตรวจ Controller → Service: PNG 480×480 ถอด QR กลับเป็น URL เดิม, HTTP 200/no-store; HTTP/query/userinfo/fragment/non-root path/malformed URL ถูกปฏิเสธด้วย 400. เป็น smoke check ไม่ใช่ผล full PostgreSQL suite หรือการตรวจ servlet authentication.

สถานะในเวลารอบ sandbox (ถูกแทนด้วยผล 01:27:59 ด้านบน): Full verify รอบใหม่ยังต้องยืนยัน: sandbox เปิด initdb.exe ไม่ได้ (CreateProcess error 623), เข้า Docker engine ไม่ได้ และรอบ H2 integration ไม่จบ จึงไม่อ้างว่ารุ่นหลัง K17 ผ่าน 28 tests. หยุด test processes ที่เริ่มรอบนี้แล้ว. ผล 28 tests ด้านล่างเป็นผลรุ่นก่อน K17 จากฝั่งอนันเอก; ต้องรันรุ่นรวมปัจจุบันใหม่ก่อน merge.

## ประวัติผล integration ก่อน K17 — 9 ตุลาคม 2026 01:04:27

พื้นที่ clone ใหม่จาก db06932 รวม A01–A06/K01–K06 จาก TEAM-HANDOFF และทำ A17 แล้ว.
`mvn -Dtest.postgres.url=jdbc:postgresql://127.0.0.1:55433/team_integration_test clean verify`
จบ 01:04:27 +07:00: **BUILD SUCCESS, 28 tests, failures/errors/skipped 0**.
PostgreSQL 16.15 ใน container ใหม่ queue-notify-team-integration-test; ไม่ใช้ฐาน queue-notify-v2-test เดิมหรือ Render.

| ชุด | ผลและขอบเขต |
|---|---|
| MigrationTest | 1 ผ่าน: V1 fixture ทั้ง 7 ตาราง → V5, เทียบทุกค่าคอลัมน์เดิม, ตรวจ snapshot/backfill/วันไทย/counter และ Hibernate validate ทั้ง 10 Entities |
| SystemIntegrationTest | 19 ผ่าน: ฐาน schema ว่าง → V1–V5 + Spring Boot Hibernate validate, CRUD, security boundaries, images, daily rollover/rollback/concurrency และ UI/API |
| BrowserWebPushSenderTest | 1 ผ่าน: sender validation ด้วยข้อมูลทดสอบ |
| InMemoryPushSubscriptionServiceTest | 7 ผ่าน: subscription ของ demo เดิม |

Fresh schema: `handoff_test_9cbf53f0e640478891f403fd5654e5f1`.
V1 upgrade schema: `handoff_test_41efadc8083d485cab4e90c3bbf529f5`.
Test helper สร้าง schema ใหม่ในฐานทดสอบเฉพาะและเก็บไว้ ไม่ DROP/TRUNCATE ข้อมูลเดิม.
V1 checksum เดิม; V2–V5 ไม่แก้เนื้อหาจากแพ็ก. ไม่มี V2 สองไฟล์.

ข้อจำกัด: Embedded PostgreSQL เริ่ม server ไม่ได้ใน Windows sandbox จึงใช้ PostgreSQL Docker ผ่าน JDBC.
Flyway 9.22.3 เตือนว่า PostgreSQL 16 ใหม่กว่ารุ่นที่รองรับทดสอบ แต่ migrations/validate/tests รอบนี้ผ่านจริง.
Mock sender ไม่เรียก Push provider; ไม่ใช่หลักฐาน Android/Render/remote CI.
ข้อความรอบ 01:04:27 นี้เป็นประวัติก่อน K17. K17 และการทดสอบ working tree รุ่นรวมเสร็จในรอบถัดมาแล้ว; GitHub PR review และการทดสอบ Git tree ฉบับรวมยังต้องทำก่อน merge PR-BASE.

## ประวัติผลจากเครื่องต้นทางในแพ็ก

วันที่ 8 ตุลาคม 2026 — ข้อมูลด้านล่างเป็นประวัติจากแพ็ก ไม่ใช่ผลล่าสุดของ integration clone นี้; Android READY รุ่นส่งมอบบน Render ยังรอตรวจ

| หลักฐาน | ผล |
|---|---|
| ผู้ใช้รัน `mvn -Dtest=MigrationTest test` บน Windows นอก sandbox 19:38:13 | PASS: 1 test, failures 0, errors 0, PostgreSQL 14.22; V1→V2→V3 พร้อมข้อมูล legacy |
| ผู้ช่วยรัน SystemIntegrationTest + unit Push ด้วย H2 19:45:12 | PASS: 21 tests, failures 0, errors 0 |
| ผู้ใช้รัน PostgreSQL full `mvn clean verify` 19:46:55 | PASS: 22 tests, failures 0, errors 0, skipped 0; สร้าง JAR สำเร็จ |
| Android Web Push demo รุ่นเดิม | ผู้ใช้ยืนยันรับตอนกด Home และล็อกจอแล้ว |
| Android order READY รุ่นใหม่บน Render | ยังไม่ทดสอบ |

Windows sandbox ป้องกัน initdb และเข้าถึง Docker engine; native PostgreSQL test จึงให้ผู้ใช้รันเองใน Git Bash. Integration tests ใช้ Mockito sender ไม่เรียก provider จริง. ฐานข้อมูลชั่วคราวไม่เปลี่ยน Docker/Render. หลังเพิ่ม QR endpoint และจัดรูปแบบโค้ด: H2 ผ่าน 22 tests (SystemIntegrationTest 14 + Push unit 8) เวลา 19:51:00. มีการ decode PNG QR แล้วเทียบ URL และปฏิเสธ HTTP/URL ที่มี token. ผล PostgreSQL รุ่นสุดท้ายรวม QR และ real login อยู่ท้ายรายงาน: ผ่าน 24 tests.

## Runtime / browser checks

- แก้ commons-lang3 จาก test scope ให้ใช้ตอน runtime; `mvn -DskipTests package` ผ่าน 19:59:36 และตรวจพบ BOOT-INF/lib/commons-lang3-3.18.0.jar.
- เริ่ม packaged JAR ผ่าน JarLauncher พร้อม H2 สำหรับ smoke test เวลา20:00:46. เปิด Tomcat สำเร็จ; H2 ไม่เป็นหลักฐาน PostgreSQL migration.
- ผู้ใช้ยืนยัน localhost8080 เปิดหน้าเมนูได้ หลังโหลด STAFF_PASSWORD และชี้ Docker5433.
- Browser: เมนู 6 รายการ, เพิ่มกะเพรา55 + ชาไทย35, สร้าง order1/queue001 รวม90; แก้กะเพราเป็น2 แล้วรวม145. ใช้ DB ในเครื่องของผู้ใช้. ออเดอร์นี้เป็นข้อมูลทดสอบที่ผู้ช่วยสร้าง ไม่ลบข้อมูลเดิม.
- หลักฐานหน้า queue: img/queue-desktop.jpg. หน้า staff และ QR ตรวจผ่านตามรายละเอียดด้านล่าง; responsive บนโทรศัพท์และ Android READY ยังรอตรวจ.

แก้ authentication: เพิ่ม PasswordEncoder bean ใช้ BCrypt ในทั้งสร้างและตรวจรหัสผ่าน. เพิ่ม test POST `/staff/login` พร้อมรหัสทดสอบ แล้วตรวจ session เข้า `/staff` ได้. ผล H2 ล่าสุด 20:07:18: 23 tests ผ่าน (integration15 + Push unit8), failures/errors0. PostgreSQL รุ่นสุดท้ายรวม MigrationTest ผ่าน 24 tests ตามผลท้ายรายงาน.

## ตรวจหน้าพนักงานหลังแก้ BCrypt

- ผู้ใช้เข้าสู่ระบบได้จริงหลัง restart แอป; ไม่มีรหัสผ่านอยู่ในหลักฐาน.
- Browser บน localhost: queue001 จาก PREPARING → READY → COMPLETED; หน้าลูกค้าแสดงพร้อมรับและปุ่มแก้/ยกเลิกหายหลัง READY.
- คิวทดสอบไม่ได้เปิด Push: dialog แสดงไม่มีประวัติส่งตามจริง ไม่อ้างว่าส่งสำเร็จ.
- หน้าเมนูแสดง 6 รายการ เปิดฟอร์มแก้กะเพราและบันทึกค่าเดิมสำเร็จ.
- หน้า QR สร้างภาพได้ด้วย `https://example.onrender.com/` ซึ่งเป็น URL ตัวอย่างสำหรับตรวจ UI เท่านั้น ห้ามพิมพ์ใช้จริง. ต้องแทนด้วย URL Render ที่ deploy รุ่นใหม่แล้ว.
- หลักฐาน: `img/queue-ready.jpg`, `img/staff-completed.jpg`, `img/qr-example.jpg`.
- ไม่ลบออเดอร์หรือเมนูเดิม; queue001 เป็นออเดอร์ที่ผู้ช่วยสร้างในการทดสอบนี้.

## ตรวจ PostgreSQL รุ่นสุดท้าย

ผู้ใช้รัน `mvn clean verify` หลังแก้ runtime dependency, QR และ BCrypt: 24 tests ผ่าน, failures/errors/skipped 0. ตรวจ XML ใน `target/surefire-reports/` ตรงกัน (Migration1 + integration15 + Push unit8). PostgreSQL tests ไม่เรียก Push provider จริง. Windows clean เคยติด JAR ของ smoke test ผู้ช่วย; หยุด process แล้วรันใหม่ผ่าน.

## Android เครื่องจริง — 8 ตุลาคม 2026

กานดิทัตรายงานว่าอนันต์เอกก์เปิดเว็บบน Android และทดสอบใช้งานแล้ว ได้รับแจ้งเตือนจริงรวมถึงขณะล็อกจอ หลังขั้นตอนทดสอบมือถือก่อน commit. ถือเป็นผลยืนยันจากผู้ทดสอบ ไม่ใช่เพียงสถานะ ACCEPTED จาก provider.

ยังไม่มีข้อมูล URL/รุ่นอุปกรณ์/ภาพหรือ notification log ของรอบนี้ จึงไม่ระบุว่าเป็นผล deploy รุ่นใหม่บน Render. ยังไม่ยืนยันการแยกแจ้งเตือนระหว่างสองเครื่อง/สองออเดอร์ และการแตะแจ้งเตือนกลับเข้าคิว. ต้องตรวจรายการเหล่านี้และทดสอบ URL รุ่นส่งมอบก่อน release.

## อัปโหลดภาพเมนู — 8 ตุลาคม 2026 21:44

หลังเพิ่ม V4 และ API multipart: H2 ผ่าน 25 tests (integration17 + Push unit8), failures/errors/skipped0. Tests ใหม่ตรวจสร้างพร้อมภาพ, อ่าน bytes แบบสาธารณะ, เปลี่ยนภาพและ cache key, ลบภาพพร้อมเมนู, สิทธิ์พนักงาน/CSRF, ไฟล์เสียหาย/ปลอมชนิด/ใหญ่เกิน/ไม่มีไฟล์ และไม่สร้างเมนูเมื่อภาพไม่ผ่าน. JavaScript syntax check ผ่าน. ผลนี้ไม่แทน Flyway/Hibernate validate บน PostgreSQL; รอยืนยัน native `mvn clean verify` และทดสอบฟอร์มใน browser รุ่นใหม่.

## เลขคิวรายวัน — 8 ตุลาคม 2026 23:20

H2 ผ่าน 27 tests (integration19 + Push unit8), failures/errors/skipped0. เพิ่มการจำลองก่อน/หลังเที่ยงคืน Asia/Bangkok: คนละวันเริ่มเลข 1 และเปิดออเดอร์/token เดิมได้, ทดสอบ counter rollback กับ transaction และนับ 1000/1001. ทดสอบออกออเดอร์พร้อมกัน 8 คำขอและตรวจเลขไม่ซ้ำ. MigrationTest เพิ่มกรณี legacy UTC timestamp → วันไทย, เลขคิวเก่า 9 ยังอยู่และ counter เริ่มที่ 9. PostgreSQL/Flyway V5/Hibernate validate ยังรอผล native `mvn clean verify` (คาดว่า 28 tests); H2 ไม่ใช่หลักฐานว่า migration ผ่าน. ไม่มีการล้างข้อมูลหรือแก้ฐานข้อมูล Docker/Render ในขั้นตอนนี้.
