# State and security test report

## หลักฐานล่าสุดก่อน C16

Shared baseline `431e824` คือ merge PR #70 ของ C15; owner commit `1971cb6`
ผู้ใช้ส่ง output `mvn clean verify` จากเครื่องคมชาญ (Windows 11, JDK21, Maven3.9.16):

| ชุด | Owner commit | Tests | Failures / Errors / Skipped | Finished (+07:00) |
|---|---|---:|---|---|
| C12 shared error contract | 8e4c6c1 | 254 | 0 / 0 / 0 | 2026-10-10 05:17:26 |
| C13 staff session/CSRF | 4b96f90 | 260 | 0 / 0 / 0 | 2026-10-10 05:30:00 |
| C14 owner isolation | f6190e4 | 267 | 0 / 0 / 0 | 2026-10-10 05:41:20 |
| C15 state/concurrency | 1971cb6 | 273 | 0 / 0 / 0 | 2026-10-10 05:53:21 |

ทุกชุดข้างต้นมี BUILD SUCCESS และ export staged diff ที่ตรวจตรงกับ payload ก่อน owner commit
log ของเครื่องแสดงปีพุทธศักราช 2569; ตารางใช้คริสต์ศักราช 2026
273 คือจำนวน Java tests รวมทั้งโปรเจ็ค ไม่ใช่ 273 tests ของคมชาญหรือของ concurrency อย่างเดียว
การทดสอบ Node/browser fixture แยกต่างหาก ไม่รวมในตัวเลข Maven นี้
C16 เปลี่ยนเอกสารเท่านั้น ต้องรัน verify บนเครื่อง owner อีกครั้งก่อน commit; ตารางนี้ไม่อ้างผล C16 ล่วงหน้า

## ขอบเขต tests

ไฟล์อยู่ใต้ `test/java/com/kku/queuenotify/`:

| ไฟล์ | สิ่งที่ยืนยันและขอบเขต |
|---|---|
| QueueTokenTest | token URL-safe 32 bytes, hash SHA256, missing/malformed/oversized input, deterministic character tampering/case/whitespace rejection |
| OrderAccessServiceTest / OrderAccessTransactionTest | owner/STAFF checks และ transaction contract |
| SecurityRoutesTest / StaffLoginTest / CsrfSecurityTest | real security filters, configured BCrypt, session ID rotation, returned CSRF and role boundaries; บาง route/service เป็น test fixtures |
| StaffSecurityIntegrationTest | 6 กรณี real login/CSRF/filter/QueueController; QueueService mock เฉพาะใน test context; ไม่ยืนยัน PostgreSQL |
| QueueContextTest / QueueTransitionEventTest | mutation invariant, UTC timestamp, event และ rollback callback ด้วย test transaction fixture |
| WaitingPreparingStateTest / ReadyTerminalStateTest | state rules ทั้ง 5 สถานะ |
| QueueServiceTest | real access/handlers/service และ registry/transaction contracts บน test-only repository/transaction fixture |
| QueueServicePersistenceTest | real PostgreSQL status/timestamp/rollback และ edit ที่รอ transition lock |
| QueueApiIntegrationTest | real PostgreSQL, BCrypt login, filters, CSRF, controllers/services และ persisted lifecycle |
| OrderAccessIntegrationTest | 5 กรณี real PostgreSQL/API: hash, cross-order read/edit/cancel, missing token, owner vs STAFF, legacy NULL hash |
| QueueStateIntegrationTest | 6 กรณี real PostgreSQL: edit/delete state matrix, terminal API preservation และ 4 concurrency cases |
| controller/api/GlobalExceptionHandlerTest | safe validation/business/internal errors และ 405/415 headers |

OrderAccessIntegrationTest และ QueueStateIntegrationTest ใช้ PushApiTestSupport ซึ่ง mock เฉพาะ provider transport
ไม่ได้ mock access/state/order/queue persistence; ทุกกรณีของ C14/C15 assert ว่า provider ไม่ถูกเรียก และไม่มี subscription
HTTP cases ใช้ login และ token ที่ fetch จริง; service concurrency ใช้ STAFF servlet request fixture ในแต่ละ thread แล้ว cleanup
tests ของ Push delivery อยู่ในรายงาน [push-test-report.md](push-test-report.md)

## รายละเอียด concurrency C15

| กรณี | วิธีประสานงาน | ผลที่ตรวจ |
|---|---|---|
| cancellation ก่อน advance | outer transaction ถือ lock หลัง cancel; worker รอ | commit แล้ว worker 409, persisted CANCELLED |
| READY ก่อน cancel | outer transaction ทำ PREPARING → READY และถือ lock | worker 409, persisted READY |
| completion ก่อน duplicate advance | outer transaction ทำ READY → COMPLETED และถือ lock | worker 409, persisted COMPLETED |
| advance 2 คำขอจาก WAITING | 2 workers รอ start latch แล้วเรียก service แยก transaction | ผล PREPARING และ READY อย่างละหนึ่ง, persisted READY |

สามกรณีแรกใช้ latch บอกว่า worker เริ่มแล้ว และตรวจ Future ยังไม่จบขณะ transaction แรกถือ lock
หลังปล่อย commit ตรวจ status ของ exception และค่าจริงใน PostgreSQL
waits มี timeout และ executor ถูกปิดใน finally; ไม่มีการใช้ sleep เป็นวิธีเรียงผลธุรกิจ
กรณีสุดท้ายไม่คาดว่า advance ซ้ำจะเหลือหนึ่งขั้น เพราะ API ไม่ได้มี idempotency/expected-state
นี่เป็นหลักฐานเฉพาะ schedule และขอบเขตที่ทดสอบ ไม่ใช่การพิสูจน์ทุก race หรือ performance/load benchmark

## การรันทดสอบซ้ำ

รันที่ project root โดย Maven ใช้ JDK21:

```bash
mvn clean verify
```

ต้องให้เครื่องรัน embedded PostgreSQL ที่ใช้ใน tests ได้ ไม่จำเป็นต้องอ้างผลจากฐานข้อมูล production
อ่านรายละเอียดจาก `target/surefire-reports` เมื่อมี failure; ส่งผลและ diff ก่อน commit ตาม workflow ของทีม
tests ใช้ credentials/ข้อมูลเฉพาะ test context ไม่ควรแทนด้วย secrets จริง

Workspace ที่เตรียมชุดไฟล์ compile PostgreSQL tests แต่รัน embedded PostgreSQL ไม่ได้ใน sandbox
หลักฐาน PostgreSQL pass ข้างต้นมาจาก full verify บนเครื่อง owner ไม่ได้มาจากการรัน local focused tests
local focused regression ล่าสุดก่อนส่ง C15 ผ่าน 30 tests และ compile 103 main / 71 test sources

## งาน acceptance ที่ยังไม่ได้ยืนยันด้วยรายงานนี้

- Browser จริง: login/logout, token storage, UI state และการ refresh CSRF
- การแจ้งเตือนจริงบน browser/OS ผ่าน HTTPS และ VAPID/provider configuration
- deployment environment, session cookie/HTTPS settings และการจัดการ secrets
- load/performance และ races นอกกรณี automated tests ที่ระบุ

C16 ไม่ได้ทำ deploy, merge ไป main หรือยืนยัน production readiness
การครบจำนวน commits เป็นหลักฐาน workflow ของทีม ส่วนการยอมรับระบบใช้งานจริงต้องมีผล acceptance แยก
