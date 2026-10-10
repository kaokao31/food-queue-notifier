# Push subscription และคู่มือส่งต่อระบบแจ้งเตือน

เอกสารนี้อ้างอิงระบบที่รวมถึง T15/C11 บน develop `76a788a` วันที่ 10 ตุลาคม 2026 ไม่ใช่การรับรอง release ทั้งโปรเจกต์ คมชาญยังเหลือ C12–C16 และการตรวจรับบนอุปกรณ์จริงยังต้องบันทึกผล

## ส่วนที่ใช้งานได้

หน้า `/queue/{id}` มีปุ่มสมัครรับแจ้งเตือนสำหรับออเดอร์นั้น Browser ขอ permission จากการกดของผู้ใช้ อ่าน VAPID public key ลงทะเบียน `/sw.js` และใช้ subscription เดิมหรือสร้างใหม่ จากนั้นส่ง subscription พร้อม `X-Queue-Token` และ CSRF ให้ server การปฏิเสธ permission ไม่ขัดขวางการติดตามคิวด้วย polling

`OrderSubscriptionServiceImpl` ใช้สิทธิ์และ row lock ของ C เก็บ subscription ในฐานข้อมูลและผูกกับออเดอร์ ข้อมูลที่แชร์ระหว่างหลายออเดอร์ใช้ endpoint เดียวกันและ keys เดิม การ DELETE ยกเลิกการผูกเฉพาะออเดอร์ ไม่ปิด subscription ของออเดอร์อื่น และไม่เรียก browser unsubscribe

เมื่อ STAFF เลื่อนคิวจาก PREPARING เป็น READY, C `QueueContext` เปลี่ยน entity และเผยแพร่ `QueueStatusChangedEvent` ภายใน transaction `ReadyNotificationObserver` รับเฉพาะ READY หลัง commit; rollback และ event นอก transaction ไม่ส่ง การสมัครเมื่อคิว READY แล้วเผยแพร่ `OrderSubscriptionAttachedEvent` เพื่อขอ catch-up หลัง commit เช่นกัน

`NotificationDeliveryServiceImpl` ตรวจสถานะปัจจุบันอีกครั้งและสร้าง READY claim ใน transaction ใหม่ ขณะเรียก provider ไม่ถือ transaction/row lock แล้วบันทึกผลใน transaction ใหม่อีกครั้ง READY claim เดิมทุกสถานะป้องกันการส่งซ้ำ ไม่มี retry อัตโนมัติ การแจ้งเตือนผิดพลาดไม่เปลี่ยน READY กลับหรือทำให้คำสั่งที่ commit แล้วตอบล้มเหลว

## ตั้งค่าและรันด้วย Maven ใน Git Bash

ใช้ JDK21, Maven และฐานข้อมูลสำหรับแอปที่มี migration ครบ ตรวจ `mvn -version` ก่อนรัน Maven ไม่อ่าน `.env` อัตโนมัติ และ Compose ปัจจุบันส่งให้ app เฉพาะ DB/PORT; การใส่ VAPID/STAFF ใน `.env` ยังไม่ทำให้ container app ได้รับค่าเหล่านั้น คู่มือนี้ใช้ Maven ที่รับ environment จาก shell

1. เริ่มฐานข้อมูลแอปตาม README ด้วย `docker compose up -d db` ถ้ามี container เดิมใช้ port 5433 อยู่ ให้ตรวจ container/database ก่อน ไม่ลบ volume เพื่อแก้ชื่อฐานข้อมูล
2. สำหรับ preview ให้ `export NOTIFICATION_MODE=console` การเลื่อน READY จะบันทึก PREVIEW โดยไม่มี provider request
3. สำหรับ browser Push สร้าง key แยกนอก repository:

```bash
java tools/GenerateVapidKeys.java "$USERPROFILE/Downloads/queue-local.vapid.env"
```

เครื่องมือเขียนเฉพาะไฟล์ใหม่ ไม่ทับไฟล์เดิมและปฏิเสธ `.env` ไม่มี key พิมพ์ใน console แก้ `VAPID_SUBJECT` ในไฟล์ให้เป็น contact จริง เช่น `mailto:` ที่มีอีเมลถูกต้อง หรือ HTTPS URL แล้วโหลด **ไฟล์ที่สร้างด้วยเครื่องมือนี้เอง**:

```bash
set -a
source "$USERPROFILE/Downloads/queue-local.vapid.env"
set +a
export NOTIFICATION_MODE=webpush
export STAFF_USERNAME=staff
read -r -s -p 'Staff password: ' STAFF_PASSWORD
printf '\n'
export STAFF_PASSWORD
mvn spring-boot:run
```

STAFF password ต้องมีอย่างน้อย 12 ตัวอักษรและไม่เกิน 72 UTF-8 bytes หากว่างจะไม่มีบัญชี default ที่ล็อกอินได้ Keep private key/password นอก Git และไม่แนบไฟล์ key ในรายงาน

ระบบตรวจ VAPID P-256 public/private เป็นคู่กันและตรวจ subject ก่อนส่ง Public key ออกผ่าน API ได้ แต่ private key ไม่ออกผ่าน API การเปลี่ยนคู่ key อาจต้องสมัคร browser ใหม่; อย่าใช้ demo ลบ subscription ของออเดอร์อื่นเพื่อแก้ปัญหา

## API และสิทธิ์

| API | สิทธิ์/CSRF | ผลหลัก |
|---|---|---|
| GET `/api/v1/push/public-key` | อ่าน public; ไม่ใช้ CSRF | public key ที่ผ่าน validation หรือ 503 |
| GET `/api/v1/csrf` | public; ใช้ session เดียวกับคำสั่งถัดไป | ชื่อ header และ token, no-store |
| POST `/api/v1/orders/{id}/subscription` | เจ้าของด้วย X-Queue-Token หรือ STAFF; CSRF | 204, ห้ามผูกกับ COMPLETED/CANCELLED |
| DELETE `/api/v1/orders/{id}/subscription` | เจ้าของหรือ STAFF; CSRF | 204, ถอดเฉพาะออเดอร์ |
| PATCH `/api/v1/queues/{id}/advance` | STAFF; CSRF | เปลี่ยนสถานะตามกฎ C |
| PATCH `/api/v1/queues/{id}/cancel` | เจ้าของหรือ STAFF; CSRF | WAITING/PREPARING เป็น CANCELLED |
| GET `/api/v1/orders/{id}/notifications` | STAFF เท่านั้น; ไม่ใช้ CSRF | metadata ที่ปลอดภัย, no-store |

Browser client เติม CSRF ผ่าน CoreUI; token เจ้าของเก็บแยกแต่ละออเดอร์ใน browser เครื่องเดิม Link ใน notification มีเพียง `/queue/{id}` และไม่มี token/keys ผู้ใช้ที่เปิดจากเครื่องอื่นหรือ storage ถูกล้างยังต้องมีสิทธิ์เจ้าของหรือ STAFF จึงอ่านออเดอร์ได้

Validator รองรับ HTTPS endpoint ของ `fcm.googleapis.com` เฉพาะ `/wp/...` และ `/fcm/send/...` ไม่มี query, fragment หรือ userinfo, port เป็น default/443 และ keys เป็น P-256 point 65 bytes กับ auth 16 bytes เอกสารนี้ไม่อ้างว่ารองรับทุก browser/provider

## แปลผล log ให้ถูกต้อง

| deliveryStatus | ความหมาย |
|---|---|
| PENDING | claim ถูกบันทึกแล้ว แต่ยังไม่มีผลสุดท้าย ไม่มี retry อัตโนมัติ |
| PREVIEW | โหมด console ไม่ส่ง provider และไม่ยืนยัน browser display |
| ACCEPTED | provider ตอบ 2xx; ยังไม่ยืนยันว่าอุปกรณ์แสดงข้อความ |
| FAILED | provider ปฏิเสธหรือการส่งผิดพลาด ไม่มี retry อัตโนมัติ |
| LEGACY | ข้อมูลเก่าที่ API ไม่ตีความเป็นผลสำเร็จของระบบใหม่ |

STAFF UI/API ส่งเฉพาะ metadata ไม่ส่ง endpoint, auth, token, private key หรือ raw provider error HTTP 410/429/500 เป็นผล FAILED ของ attempt นั้น ปัจจุบันยังไม่ปิด subscription อัตโนมัติหรือทำ retry/backoff

## Standalone demo

ตั้ง `export SPRING_PROFILES_ACTIVE=push-demo` ก่อนเริ่มแอป ล็อกอิน `/staff/login` แล้วเปิด `/push-demo.html` เมื่อ profile ปิดอยู่ STAFF ก็เข้า demo ไม่ได้ ทุกคำสั่ง demo ใช้ STAFF และ CSRF

Demo เรียก WebPushSender โดยตรง จึงเป็นการส่ง provider จริงเมื่อผู้ใช้กดส่งและ config ครบ แม้ `NOTIFICATION_MODE=console` ข้อมูล demo เก็บใน memory แยก session อายุ 15 นาที จำกัด 100 entries หายเมื่อ process restart และไม่เขียนออเดอร์หรือ notification log ปุ่มลบลบเฉพาะ entry ของ demo

## ลำดับและข้อจำกัดสำหรับส่งต่อ

- Observer ทำงานแบบ synchronous หลัง commit บน thread ของคำสั่งธุรกิจ Provider request จึงอาจทำให้ response รอ แต่ไม่ถือ DB lock; sender ใช้ HTTP timeouts 10 วินาทีและรอ future สูงสุด 15 วินาที
- ไม่มี durable event replay หรือ async worker หาก process หยุดระหว่าง business commit กับการรับ event อาจพลาดการแจ้งเตือน หากหยุดหลังสร้าง claim อาจค้าง PENDING และ claim เดิมกันการส่งซ้ำ
- การสมัครหลัง READY ใช้ catch-up และตรวจสถานะใหม่ ถ้าคิวจบไปแล้วจะไม่ส่ง เมื่อมี READY claim เดิมแล้ว catch-up ไม่ส่งซ้ำ
- Service worker แสดง notification และ focus/open เฉพาะ local URL ที่อนุญาต ไม่มีการ cache API, owner token หรือข้อมูลออเดอร์
- ทดสอบ browser จริงต้องใช้ secure context; `localhost` ใช้บนเครื่องเดียวกัน ส่วนโทรศัพท์ที่เปิด LAN HTTP ไม่ใช่ localhost ของเครื่อง server ต้องมี HTTPS ที่เหมาะสม

ดู [sequence READY](diagrams/sequence-ready.md), [sequence subscribe](diagrams/sequence-subscribe.md) และ [ผลทดสอบ/รายการตรวจอุปกรณ์](push-test-report.md)
