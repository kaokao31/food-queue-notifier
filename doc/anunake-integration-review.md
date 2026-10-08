# Anunake PR-BASE review — 9 ตุลาคม 2026

สถานะนี้คือผลที่ผู้ช่วยตรวจ/แก้/ทดสอบ ยังต้องให้อนันต์เอกก์อ่านและ review ก่อนใช้บัญชีตัวเอง commit/push. ไม่ถือว่าแบ่งชื่อผู้เขียนจากไฟล์ในแพ็กแล้วเป็น contribution อัตโนมัติ.

เพิ่มเติมหลังรับ PR-BASE-A-REVIEW-K17: รวม Repository interface follow-up และ QrService/K17 แล้วใน integration clone. โค้ด update ทั้ง12ไฟล์ตรง reviewer payload ก่อนปรับเอกสารส่วน A เพิ่ม. ผล28testsวันที่01:04ด้านล่างเป็นรอบก่อน K17; ผลรุ่นหลัง review ให้ดูส่วนล่าสุดใน test-report.md.

## พื้นที่และข้อตกลง

- integration clone ใหม่จาก db06932; ไม่มี changes ก่อน import; ใช้ V1–V5 ของแพ็กชุดเดียว
- import A01–A06 และ K01–K06 เพื่อทดสอบร่วม; ต่อมารับ patch A17 follow-up/K17 ที่กานดิทัต review แล้ว; K runtime files เปลี่ยนเฉพาะ QrController/QrService/QrServiceImpl ตามแพ็ก review
- พื้นที่เก่าใน E:/KAGE/.../food-queue-notifier ไม่แก้; สำเนา changes/patch/reports อยู่ work/preserved-v2 ข้าง integration clone
- queue-notify-v2-test เดิมและ Render ไม่ใช้; ฐานรุ่นรวมใหม่ localhost:55433/team_integration_test
- มีการถอด Java เก่าสองไฟล์ตาม manifest: NotificationFactory และ LineNotificationStrategy เพื่อให้ตรง event/strategy รุ่นรวม ไม่มีการลบตาราง/ข้อมูลเดิม

## A01–A06 ที่ตรวจ

| ชุด | ผล review และสิ่งที่ต้องอธิบาย |
|---|---|
| A01 schema/entities/repos | V1 checksum เดิม, V2 เดียว, V2–V5 ตรงแพ็ก, 10 Entity/tables; shared PK, snapshot, NULL legacy token, image/counter แยกตาราง |
| A02 contracts/mappers | OrderRequest จำกัด 1–50 รายการ ไม่รับ item null, quantity 1–99; ราคาฝั่ง server; response มี queueDate/order ID/token; list page/sort allowlist |
| A03 menu/images | create/read/update/delete, เมนูมีประวัติให้409และปิดขายแทน; validate image ก่อน persist; menu/image อยู่ transaction เดียว |
| A04 ordering | ล็อก menu ตาม ID, snapshot, total overflow guard, order/items/queue/counter อยู่ transaction; WAITING-only edit/delete ประสาน access ของ C |
| A05 tests | เพิ่ม safe Docker test adapter, เปรียบเทียบทุกคอลัมน์เดิมของ 7 V1 tables และ validate Entity ทั้งหมดหลัง upgrade; ไม่ใช้ H2 แทน migrations |
| A06 docs | ER/domain/data dictionary/migration policy ปรับ10ตารางและคิวรายวัน; ปรับ sequence/class/README/report/handover/SOLID ที่เป็นเอกสารร่วมให้ K review ด้วย |

## A17 ที่แก้จากแพ็ก

OrderServiceImpl inject QueueNumberService interface → QueueNumberServiceImpl เลือก queueDate จาก Clock/Asia/Bangkok → DailyQueueCounterRepository interface → JdbcDailyQueueCounterRepository ทำ SQL.

UPDATE ล็อก counter แถวของวันจน transaction ออเดอร์จบ; คำขอพร้อมกันรอ lock และได้เลขต่างกัน. SELECT อ่านค่าที่ transaction ตนเพิ่มแล้ว. INSERT-if-absent รองรับวันใหม่. MANDATORY ห้ามเรียกออกเลขโดยไม่มี transaction. Rollback คืน counter และ order/items/queue พร้อมกัน. ลบ order ไม่ลด counter. ไม่ใช้ MAX+1 ออกเลข.

ไฟล์เพิ่มจากฐาน db06932: service/impl/QueueNumberServiceImpl.java, repository/DailyQueueCounterRepository.java และ repository/JdbcDailyQueueCounterRepository.java. หลัง review Repository แรกเป็น interface และ Jdbc implementation ถือ SQL เดิม.
ไฟล์เปลี่ยน: service/QueueNumberService.java. OrderServiceImpl ใช้ชื่อ QueueNumberService เดิมแต่ตอนนี้เป็น interface.

## ผลทดสอบจริงและขอบเขต

`mvn -Dtest.postgres.url=jdbc:postgresql://127.0.0.1:55433/team_integration_test clean verify` ผ่าน 28 tests 0 failures/errors/skipped จบ 9 Oct 2026 01:04:27 +07:00. PostgreSQL16.15; Hibernate validate ผ่าน schema ว่างและ schema V1 upgrade. ดู test-report.md.

Migration fixtures เป็นข้อมูลจำลอง; ทุกค่าคอลัมน์เดิมทั้ง7ตารางตรงก่อน/หลัง. V5 คำนวณวันของ created_at ที่ถือเป็น UTC; ไม่ได้พิสูจน์ว่า Render legacy ใช้ UTC. ต้องตรวจปลายทางก่อน deploy. ยังไม่ทดสอบ Push จริง/Android/Render/CI บน PR.

## API contract สำหรับรวมกับกานดิทัต

- POST /api/v1/orders รับ items[{menuItemId,quantity}], return201 OrderResponse: id,totalAmount,items,queue{id,queueNumber,queueDate,status,statusChangedAt},queueToken,pushEnabled,createdAt,updatedAt
- queueToken ส่ง raw ตอน create เท่านั้น; หลังจากนั้นใช้ X-Queue-Token กับ get/update/attach/detach ของ order นั้น เลขคิว/วันที่ไม่ใช่หลักฐานสิทธิ์
- GET/PUT /api/v1/orders/{id}; DELETE และ GET list ใช้ staff ตาม security/access; แก้เฉพาะ WAITING
- GET/POST /api/v1/menu-items; GET/PUT/DELETE /{id}; staff เป็นผู้ mutate; list page/size/sort
- POST /api/v1/menu-items/with-image และ PUT /{id}/with-image ใช้ multipart menu(application/json)+file; GET /{id}/image อ่านสาธารณะ; ไม่ส่งภาพตอน JSON update จะคงภาพเดิม
- PUT/DELETE /api/v1/orders/{id}/push-subscription เป็นส่วน K; owner token + CSRF. การ attach ตอน READY และการส่งซ้ำให้ C/T ยืนยันจุดเชื่อม
- ทุก mutation ใช้ CSRF. Validation400 / no staff401 / forbidden403 / missing404 / conflict409. JSON timestamp ไม่มี offset ให้อ่าน UTC; queueDate เป็นวันไทย

## ก่อน owner commit/push และ merge

1. อนันต์เอกก์อ่าน A01–A06/A17 และอธิบายได้ด้วยตัวเอง ตรวจ staged diff รายไฟล์ตาม A manifest ไม่ใช้ git add . เพราะ working tree มีส่วน K เพื่อทดสอบ
2. เอกสารร่วมที่แก้ (README, report, sequence/class/SOLID/handover) ต้องประสาน K06; มี patch เทียบกับแพ็กสำหรับกานดิทัต ไม่ commit โค้ด K runtime ในนาม A
3. กานดิทัตส่งผล review/K17 (QrService) แล้ว; อนันต์เอกก์ต้องอ่าน Repository interface follow-up และผลทดสอบรุ่นรวมหลัง apply ก่อน commit
4. แต่ละคน commit/push เฉพาะ contribution ของตัวเอง ส่ง SHA ให้รวม PR-BASE ตามแผน cherry-pick รักษา author; ไม่มี PR จน remote branch พร้อมและสร้างจริง
5. review กันก่อน merge develop; ยังไม่ merge/deploy Render. A07–A16 ทำตามช่องว่างจริงหลังฐานรวมผ่าน

จำนวน commits ต้องมากกว่า15ต่อคนตามคำยืนยันล่าสุด แต่ต้องมาจากงานจริง ไม่มี empty commits หรือเปลี่ยน author/date เพื่อเติมยอด.

## คำถามที่คุณควรตอบได้ก่อนส่ง review

1. ทำไม Queue ใช้ order ID เป็น PK แต่ queueNumber ซ้ำได้เมื่อคนละวัน?
2. ทำไม counter rollback กับออเดอร์ และคำขอพร้อมกันไม่ชนเลข?
3. ชื่อ/ราคา snapshot ของออเดอร์เก่าต่างจากราคาปัจจุบันอย่างไร และข้อมูล backfill เก่ามีข้อจำกัดอะไร?
4. ทำไมเมนูมีประวัติแล้วลบไม่ได้ และเหตุใด Order update ต้อง lock คิวร่วมกับ State transition?
5. เหตุใดผ่าน PostgreSQL ในเครื่องแล้วยังลง Render ทันทีไม่ได้?
