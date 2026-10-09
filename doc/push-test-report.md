# ผลทดสอบระบบ Push และรายการตรวจอุปกรณ์จริง

วันที่บันทึก 10 ตุลาคม 2026 อ้างอิง develop `76a788a` หลัง T15 merge PR #64 ข้อมูลด้านล่างมาจากผลบนเครื่องเจ้าของงานที่ส่งมา ไม่ใช่ผลทดลองบนอุปกรณ์จริง

## หลักฐานอัตโนมัติที่ยืนยันแล้ว

| ขั้น | เครื่อง/คำสั่ง | ผลและเวลาจบ (+07) |
|---|---|---|
| T13 | เครื่องธีธัช, `mvn clean verify` | 224 tests, 0 failures/errors/skips, 2026-10-10 04:14:17 |
| T14 | เครื่องธีธัช, JS/key runner แล้ว `mvn clean verify` | 239 Java tests, 0 failures/errors/skips, 2026-10-10 04:30:05 |
| T15 | เครื่องธีธัช, `mvn clean verify` | 247 tests, 0 failures/errors/skips, 2026-10-10 04:44:04 |
| T14 workspace | Node.js suites + key utility checks | 14 JS test files ผ่าน; signature check ผ่าน; existing output/.env ไม่ถูกทับ |
| T15 workspace | focused regression suite + compile | 49 existing tests ผ่าน; new native sources compile; การรัน 8 กรณีใหม่ยืนยันจากเครื่องธีธัชในผล 247 ข้างบน |

เวลาใน owner log แสดงปีพุทธศักราช 2569 ตารางใช้คริสต์ศักราช 2026 จำนวน 247 รวม Java suite ทั้งโปรเจกต์ ไม่ใช่จำนวน Push tests อย่างเดียว T16 เป็นเอกสาร; ยังไม่มีผล `mvn clean verify` สำหรับ commit T16 ณ วันที่เขียน

T15 เพิ่ม 8 กรณี full API บน PostgreSQL: owner/CSRF registration, endpoint/keys rejection, shared-browser per-order detach, terminal attachment rejection, create→attach→READY→safe STAFF log, late attach duplicate suppression, provider 410/429/500 recorded as FAILED and cancellation without delivery ใช้ security filters, BCrypt login/session, CSRF endpoint, C/A/T service, observer และ repository จริง จำลองเฉพาะ NotificationStrategy ที่ติดต่อ provider

T13 tests ตรวจ rollback/no fallback, AFTER_COMMIT, current status recheck, catch-up, duplicate claims, safe failure isolation และ provider I/O ที่ไม่มี transaction ขณะที่ T11 PostgreSQL tests ตรวจ concurrent claims/row-lock release และ T08 sender tests ตรวจ encryption/signing ผ่าน transport จำลอง

Demo tests ตรวจ profile/STAFF/CSRF, session isolation, expiry/capacity และ duplicate guard Browser JS tests ใช้ environment จำลองและไม่ใช่การตรวจ UI/OS notification บนอุปกรณ์จริง

## สิ่งที่ผลข้างบนยังไม่ยืนยัน

ยังไม่มีหลักฐาน live provider request, actual device display, การคลิก OS notification บนอุปกรณ์จริง, deployment/HTTPS บน server, หรือ CI ของ T16 ผล PREVIEW และ ACCEPTED ไม่ถือเป็นหลักฐานว่าอุปกรณ์แสดงข้อความ ห้ามเติมสถานะผ่านจากจำนวน commits

## รายการตรวจรับบนอุปกรณ์จริง

ใช้ test order และ browser ของผู้ทดสอบ บันทึกวันที่ browser/version, OS, URL และโหมด โดยไม่แนบ endpoint/key/token/private key/password

| กรณี | วิธีตรวจ/ผลที่คาด | ผลจริง |
|---|---|---|
| Permission denied | กดเปิดแจ้งเตือนแล้วปฏิเสธ; polling ยังติดตามคิวได้ | ยังไม่ได้ทดสอบ |
| Console preview | เปลี่ยน READY ใน console mode; log PREVIEW ไม่มี provider send | ยังไม่ได้ทดสอบบน UI จริง |
| Live browser subscription | HTTPS/localhost, public key ถูกต้อง, อนุญาต; attach 204 | ยังไม่ได้ทดสอบ |
| READY provider result | STAFF เลื่อน WAITING→PREPARING→READY; ตรวจ log ACCEPTED/FAILED | ยังไม่ได้ทดสอบกับ provider จริง |
| Device display | ดู OS notification และบันทึกเวลาจริง; แยกจาก ACCEPTED | ยังไม่ได้ทดสอบ |
| Notification click | คลิกแล้ว focus/open local queue URL; owner storage เดิมอ่านได้ | ยังไม่ได้ทดสอบ |
| Late subscription | READY ไม่มี subscription แล้วสมัคร; หนึ่ง attempt เมื่อยัง eligible | ยังไม่ได้ทดสอบบนอุปกรณ์จริง |
| Detach isolation | สองออเดอร์ browser เดียวกัน; ถอดหนึ่ง อีกออเดอร์ยังสมัครอยู่ | ยังไม่ได้ทดสอบบน UI จริง |
| Missing access | เปิด link ใน storage ใหม่; ไม่อ่านข้อมูลออเดอร์โดยไม่มี owner/STAFF | ยังไม่ได้ทดสอบบน UI จริง |
| Demo | เปิด push-demo, STAFF+CSRF, ส่ง explicit test; ไม่เพิ่ม production log | ยังไม่ได้ทดสอบกับ provider จริง |

## ทำซ้ำผลอัตโนมัติ

ใช้ Maven ที่แสดง JDK21 และ Node.js ใน root checkout:

```bash
for test in test/js/*.test.cjs; do node "$test" || exit 1; done
java tools/GenerateVapidKeys.java --check
mvn clean verify
```

ชุดทดสอบใช้ฐานข้อมูลแยกจากแอป ไม่ใช้ app database เพื่อล้าง fixture บันทึกผลใหม่จาก `target/surefire-reports` พร้อม SHA และเวลาหลังรวมงานคมชาญ C12–C16 และตรวจ acceptance/deployment ของทีมอีกครั้ง
