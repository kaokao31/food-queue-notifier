# Migration policy

ใช้ V1–V5 จาก TEAM-HANDOFF ชุดเดียวในพื้นที่ integration ใหม่ ไม่วาง V2__anonymous_ordering_expand.sql ของการทดลองเดิมร่วมกับ V2 ของแพ็ก. เก็บโค้ด/ผลทดสอบ/ฐาน queue-notify-v2-test ของเดิมไว้แยก.

V1 คงเดิมทุก byte เพื่อรักษา Flyway checksum. V2 เพิ่ม subscription/price snapshot/token hash/notification delivery fields โดยไม่ลบ legacy customer records และผ่อน customer_id/status เพื่อให้ anonymous order ใช้งานได้. V3 seed เมนูเฉพาะ DB ที่ยังไม่มีเมนู. V4 เพิ่ม image_key และตาราง menu_item_image เก็บ bytes ของ JPG/PNG แยกจาก menu list. V5 เพิ่ม queue_date/counter รายวัน และเปลี่ยน unique จาก queue_number เป็น (queue_date,queue_number); คง sequence เดิมไว้แต่การสร้างออเดอร์ใหม่ไม่ใช้แล้ว.

ก่อนใช้ Render สำรองและตรวจ duplicate queue number, duplicate menu lines ในออเดอร์, quantity<=0, negative prices และราคาย้อนหลังที่ subtotal/quantity ไม่ลงตัวถึงสองตำแหน่ง. V2 จงใจหยุดเมื่อ reconstruct unit price แบบ exact ไม่ได้ และไม่ได้ปัดหรือเขียนทับยอดเก่า.

ชื่อเมนูย้อนหลัง backfill จากชื่อปัจจุบัน เพราะ V1 ไม่มี snapshot จึงกู้ชื่อในอดีตแน่นอนไม่ได้. ราคา unit_price เก่าอนุมานจาก subtotal/quantity โดยต้องหารได้ตรงสองตำแหน่ง. updated_at เก่าเริ่มจาก created_at ไม่ใช่ประวัติการแก้จริง. Token ของคิว legacy เป็น NULL: ลูกค้าเข้าด้วยเลขคิว/id อย่างเดียวไม่ได้; staff ยังตรวจได้. ไม่มี migration ลบ customer/notification_preference หรือข้อมูลเดิมในชุดนี้.

เวลาใหม่ใช้ UTC ด้วย LocalDateTime และตั้ง JDBC timezone UTC; JSON ไม่มี offset จึงต้องอ่านเป็น UTC อย่างชัดเจน. วันคิวใหม่ใช้ Asia/Bangkok. V5 ตีความ orders.created_at ของ legacy ว่าเป็น UTC เพื่อคำนวณ queue_date; ไม่เปลี่ยน created_at เดิม. **สมมติฐาน UTC นี้ต้องตรวจจากข้อมูล/โค้ดรุ่นที่สร้างข้อมูลจริงก่อนใช้ปลายทาง** หากข้อมูลเก่าเป็นเวลาไทยให้หยุดและออกแบบแนวทางกับทีมก่อน ห้ามรัน V5 เพื่อเดาวันย้อนหลัง.

DailyQueueCounterRepository เป็น interface; JdbcDailyQueueCounterRepository ทำ INSERT-if-absent, UPDATE และ SELECT ใน transaction ของ OrderService ผ่าน QueueNumberService interface/implementation. UPDATE ล็อกแถวของวันนั้นจนจบ transaction; rollback จะคืน counter พร้อม order/items/queue. ลบออเดอร์ไม่ลด counter และเลขไม่วนกลับเมื่อเกิน 999. ไม่ใช้ MAX+1 ในการออกเลขใหม่; V5 ใช้ max เฉพาะตั้งค่าเริ่มต้นจากข้อมูลเดิม.

## ก่อนแตะฐานปลายทาง

ยังไม่เชื่อม/แก้ Render ในรอบ integration นี้. ตรวจ Flyway history และ checksum จริงพร้อม backup ก่อน หากเคยใช้ V2 คนละชุด ห้ามแทนไฟล์หรือ repair เพื่อข้ามข้อแตกต่าง ต้องวาง migration ถัดไปตาม schema/history ที่ตรวจพบ. ตรวจ timezone, จำนวนข้อมูล legacy, duplicate queue numbers/menu lines, quantity/ราคา และ backfill assumptions บนสำเนาก่อน.

## การทดสอบที่แยกจากข้อมูลเดิม

`mvn clean verify` ใช้ embedded PostgreSQL แบบชั่วคราวตามแพ็ก. ถ้า Windows sandbox เริ่ม PostgreSQL ไม่ได้ ให้ใช้ container ใหม่ `queue-notify-team-integration-test` ที่ localhost:55433/team_integration_test ผ่าน `-Dtest.postgres.url=jdbc:postgresql://127.0.0.1:55433/team_integration_test`. Test helper ปฏิเสธ URL อื่น สร้าง schema สุ่มใหม่สำหรับแต่ละชุด และไม่ DROP/TRUNCATE schema เก่า; ไม่ใช้ DB_URL ของแอปหรือฐาน V2 เดิม. External test schemas คงไว้ตรวจหลักฐาน.

SystemIntegrationTest ตรวจฐานว่างผ่าน Flyway ทั้ง 5 migrations และ Spring Boot Hibernate validate. MigrationTest ตรวจ V1 ที่มีข้อมูลผ่าน V5 และ Hibernate validate ทุก Entity หลัง upgrade. H2 ไม่เป็นหลักฐาน PostgreSQL migration.

ทำ migration/validate บน DB สำเนาหรือ DB ชั่วคราวก่อน deploy. ห้าม down -v กับ volume ที่อาจมีข้อมูลใช้งาน. หลัง V2 โค้ดเก่าอาจไม่รองรับ anonymous rows จึงไม่ควร rollback binary โดยไม่ประเมินข้อมูลใหม่.
