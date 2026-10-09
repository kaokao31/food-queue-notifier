# Migration guide

Flyway เปิดอัตโนมัติ, baseline-on-migrate=false, Hibernate ddl-auto=validate สำรองข้อมูลก่อนใช้กับฐานเดิม ไม่แก้ SQL migration ที่ถูกใช้แล้วและไม่ใช้ clean กับฐานแอป

| Version | เปลี่ยนแปลง |
|---|---|
| V1 | schema legacy customer/preferences/menu/order/items/queue/log |
| V2 | anonymous orders, subscription data, snapshots, token hash และ log metadata/index |
| V3 | seed 6 เมนูเฉพาะ catalog ว่าง |
| V4 | image_key และ menu_item_image แยก bytea จากรายการเมนู |
| V5 | queue_date Bangkok จาก created_at UTC, unique(date,number), counter จาก max ของแต่ละวัน |

V2 หยุดเมื่อ quantity/subtotal เดิมทำให้ reconstruct unit_price แบบ exact ไม่ได้ และ unique constraints จะปฏิเสธข้อมูลซ้ำ ต้องตรวจข้อมูลเดิมก่อน ไม่ฝืนด้วยการลบประวัติ V5 คง queue ID/number/token/status/log เดิม และเก็บ legacy sequence ไว้แต่เลขคิวใหม่ใช้ daily counter

รัน `mvn clean verify` เพื่อทดสอบฐานแยก fresh และ V1 upgrade และ Flyway validate/migrate ซ้ำ ผล owner ล่าสุดผ่าน 52 tests ณ 2026-10-09 17:35:23+07; ยังต้องตรวจ integration หลังรวม C/T

PostgresTestDatabase สร้าง embedded DB สำหรับ tests ถ้าใช้ external test database helper ยอมรับเฉพาะ localhost:55433/team_integration_test และสร้าง schema ใหม่ ไม่ชี้ tests ไป DB_URL ของแอป
