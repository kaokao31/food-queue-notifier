# Migration guide — V1–V6

อ้างอิงโค้ด `develop` baseline `64b3ce7` วันที่ 10 ตุลาคม 2026 Flyway เปิดอัตโนมัติ, baseline-on-migrate=false และ Hibernate ddl-auto=validate สำรองข้อมูลก่อนใช้กับฐานเดิม ไม่แก้ SQL migration ที่ถูกใช้แล้วและไม่ใช้ clean กับฐานแอป

| Version | เปลี่ยนแปลง | ไฟล์ |
|---|---|---|
| V1 | schema legacy customer/preferences/menu/order/items/queue/log | [V1](../code/src/main/resources/db/migration/V1__init_schema.sql) |
| V2 | anonymous orders, subscription, item snapshots, token hash, log metadata/index | [V2](../code/src/main/resources/db/migration/V2__anonymous_ordering.sql) |
| V3 | seed 6 เมนูเฉพาะ catalog ว่าง | [V3](../code/src/main/resources/db/migration/V3__sample_menu.sql) |
| V4 | image_key และ menu_item_image แยก bytea จากรายการเมนู | [V4](../code/src/main/resources/db/migration/V4__menu_images.sql) |
| V5 | queue_date Bangkok จาก created_at UTC, unique(date,number), counter จาก max ของแต่ละวัน | [V5](../code/src/main/resources/db/migration/V5__daily_queue_numbers.sql) |
| V6 | เติม image_key แบบ asset: ให้เมนูที่ไม่มี key และไม่มี uploaded image เพื่อรักษารูปหลังเปลี่ยนชื่อ | [V6](../code/src/main/resources/db/migration/V6__persistent_menu_photos.sql) |

## ข้อมูลเดิมและ invariants

V2 หยุดเมื่อ quantity/subtotal เดิมทำให้ reconstruct unit_price แบบ exact ไม่ได้ และ unique constraints ปฏิเสธข้อมูลซ้ำ ต้องตรวจข้อมูลเดิมก่อน ไม่ฝืนด้วยการลบประวัติ Queue เดิมที่ไม่มี token_hash เข้าถึงได้โดย STAFF ตามระบบสิทธิ์ ไม่สร้าง token ลูกค้าให้ย้อนหลังอัตโนมัติ

V5 คง queue ID/number/token/status/log เดิม และเก็บ legacy sequence ไว้ แต่เลขคิวใหม่ใช้ daily counter วันที่ Bangkok การ unique ใช้คู่ queue_date/queue_number ไม่ใช่ queue_number เดี่ยว

V6 เปลี่ยนข้อมูล image_key โดยไม่เพิ่มตารางหรือคอลัมน์ เลือกไฟล์ของ 6 ชื่อเมนูเริ่มต้น หากไม่ตรงชื่อจะใช้ fallback ตามหมวดหมู่ UPDATE เฉพาะ image_key IS NULL และไม่มี menu_item_image ดังนั้น key เดิมและ uploaded image ไม่ถูกเขียนทับ

V6 ใช้ชื่อ/หมวดหมู่ ณ เวลาที่ migrate จึงไม่สามารถกู้ชื่อหรือรูปเดิมของเมนูที่เคยเปลี่ยนชื่อไปแล้วก่อน migration ได้ รูปที่ถูกกำหนดแล้วจะคงเดิมเมื่อแก้ชื่อ/หมวดหมู่ในอนาคต และ API สร้างเมนูใหม่กำหนด key ผ่าน MenuPhotos ใน MenuServiceImpl

## การตรวจและใช้งาน

1. ใช้ฐานเฉพาะ environment และสำรองข้อมูลเดิมก่อนอัปเกรด
2. เปิดแอปด้วยโค้ดที่มี migration ครบ Flyway จะตรวจ checksum และใช้ migration ที่ยังไม่รัน ก่อน Hibernate validate
3. ตรวจผลเริ่มระบบและ migration history ไม่ลบหรือแก้ history เพื่อข้ามข้อผิดพลาด
4. ทดลองรูปเมนู การอ่าน/สร้างออเดอร์ และเลขคิวตามวัน โดยรักษาข้อมูลประวัติ

```bash
(
  unset NOTIFICATION_MODE
  mvn clean verify
)
```

PostgresTestDatabase ใช้ embedded DB แยกจากฐานแอป หากเลือก external test database helper ยอมรับเฉพาะ localhost:55433/team_integration_test และสร้าง schema แยก ไม่ใช้ DB_URL ของแอป

ผล full suite ที่ทีมส่งล่าสุดผ่าน 285 tests ใน C-R17; ไม่ใช่ผลทดสอบใหม่ของเอกสารนี้ หลักฐาน V6 อยู่ใน [MenuPhotoMigrationTest](../test/java/com/kku/queuenotify/MenuPhotoMigrationTest.java), [MenuCrudPersistenceTest](../test/java/com/kku/queuenotify/MenuCrudPersistenceTest.java) และ [MenuMappingTest](../test/java/com/kku/queuenotify/MenuMappingTest.java) รวมทั้ง migration/persistence tests เดิมใน test/java

ดู [ER](diagrams/er.md), [Handover](handover.md) และ [README](../README.md)
