# Recorded test evidence

| เจ้าของ | คำสั่ง/ขอบเขต | ผลที่เจ้าของส่งมา |
|---|---|---|
| K | Node browser-workflow; Maven WebMvcIntegrationTest, StaffFormTemplateTest, QrServiceTest, QrControllerTest | JS ผ่าน; Java 9 tests, failures/errors/skipped 0, BUILD SUCCESS 2026-10-09 17:27:12+07 |
| A | mvn clean verify หลังแก้ชื่อ table ใน MigrationTest เป็น menu_item_image | 52 tests, failures/errors/skipped 0, BUILD SUCCESS 2026-10-09 17:35:23+07 |

การทดสอบ A รอบแรกมี error เดียวจากชื่อ table ใน test query; แก้เฉพาะ test แล้วรันใหม่ผ่าน ไม่แก้ migration ที่ใช้งานแล้ว

ชุด A ทดสอบ V1–V5 fresh/upgrade, catalog visibility, snapshots, image JSON/multipart, rollback, Bangkok midnight และ concurrent ordering โดยใช้ access/token fixtures ใน test sources ผล K ตรวจ DOM/assets/MVC และ JS modules ด้วย API fixtures

หลักฐานนี้ยังไม่ครอบคลุม production security/State/Push ของ C/T, browser acceptance จริง หรือ Docker startup/CI run ของ configuration รอบล่าสุด ต้องบันทึกผลเหล่านั้นหลังรันจริง
