# Ordering and persistence review

ผล native `mvn clean verify` วันที่ 2026-10-09 17:35:23+07: 52 tests, failures/errors/skipped 0, BUILD SUCCESS

ขอบเขต: fresh/V1-upgrade V1–V5 migration, validation/mappers, menu CRUD/history protection, JSON/multipart image byte storage และ rollback, order snapshots/edit/delete, queue daily transaction/concurrency/Bangkok midnight

Integration tests ใช้ access/token/clock fixtures ใน test sources และฐาน PostgreSQL แยก ทดสอบ delegation และ transaction ของ A; ไม่อ้างว่า production token/security/State ของ C หรือ Observer/Push/Log ของ T ผ่านแล้ว

พบและแก้ชื่อ table ใน MigrationTest จาก menu_image เป็น menu_item_image แล้วรันครบใหม่ผ่าน โดยไม่แก้ migration หลังใช้ งานที่รอรวมคือ C/T providers และ API แล้วตรวจ browser/HTTPS Push acceptance จริง

หลังรวม K tests ล่าสุด จำนวน tests อาจเพิ่ม ให้บันทึกผลใหม่จากรันจริงแทนคัดลอกจำนวนเดิม สไลด์ไม่รวมในรอบนี้
