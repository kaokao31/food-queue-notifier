# Queue state and access design

อ้างอิง shared baseline `431e824` หลัง C15 merge ผ่าน PR #70
เอกสาร C16 บันทึกพฤติกรรมที่มีอยู่ ไม่เปลี่ยน API หรือ policy
ดู [State diagram](diagrams/state.md) และ [รายงานทดสอบ](state-test-report.md)

## การแยกหน้าที่

`QueueController` รับ HTTP และส่ง QueueResponse; `QueueServiceImpl` ครอบ transaction และเลือก handler;
`OrderAccessServiceImpl` ตรวจสิทธิ์พร้อมล็อก queue; State handlers กำหนด transition;
`QueueContext` เปลี่ยน entity และเผยแพร่ event; observer/delivery ของธีธัชรับผิดชอบการแจ้งเตือน
`OrderServiceImpl` ตรวจสิทธิ์ผ่าน contract เดียวกันก่อนแก้/ลบบิล จึงใช้ queue lock ร่วมกับการเปลี่ยนสถานะ

การแบ่งนี้ทำให้กฎ State ไม่ต้องทราบ HTTP หรือ Push transport (SRP)
service รับ `OrderAccessService`, `QueueStateHandler`, publisher และ Clock ผ่าน constructor (DIP)
handlers ใช้ contract เดียวกัน (LSP/ISP) และเลือกจาก registry แทนการกระจายเงื่อนไขใน controller
การเพิ่มสถานะใหม่ยังต้องแก้ enum, handler, registry coverage และ tests ให้ครบ ไม่ใช่เพิ่มคลาสเดียวแล้วใช้งานได้ทันที

## Owner token ของแต่ละบิล

`QueueToken.generate` ใช้ SecureRandom 32 bytes แล้ว encode Base64 URL-safe แบบไม่มี padding
`QueueToken.hash` ใช้ SHA-256 UTF-8 และ `matches` ใช้ MessageDigest.isEqual เปรียบเทียบ hash
ฐานข้อมูลเก็บ `queue.token_hash` ไม่เก็บ raw token; raw token ส่งกลับใน response ตอนสร้างบิลเท่านั้น
GET/update responses ไม่ส่ง token/hash กลับมาใหม่

ลูกค้าส่ง raw token ผ่าน `X-Queue-Token` สำหรับบิลที่ต้องการเข้าถึง
token เป็น bearer credential: ผู้ที่มี token ที่ถูกต้องเข้าถึงบิลนั้นได้ จึงไม่ควรใส่ใน URL, log หรือเอกสารที่แชร์
ไม่มีบัญชีลูกค้าหรือกระบวนการกู้ token ในโค้ดชุดนี้
missing/wrong token และ legacy NULL token_hash ไม่ผ่านการตรวจสิทธิ์ลูกค้า
STAFF ที่ authenticated สามารถจัดการบิลตามกฎสถานะได้แม้ไม่มี owner token

## เส้นทางและสิทธิ์

| เส้นทาง | ใครเข้าถึงได้ | การตรวจเพิ่ม |
|---|---|---|
| GET menu, public key, csrf | public | endpoint-specific validation/configuration |
| POST /api/v1/orders | public | CSRF และ payload validation |
| GET /api/v1/orders/{id}, /api/v1/queues/{id} | owner หรือ STAFF | service ตรวจ token/role |
| PUT /api/v1/orders/{id} | owner หรือ STAFF | CSRF และ WAITING |
| PATCH /api/v1/queues/{id}/cancel | owner หรือ STAFF | CSRF และ WAITING/PREPARING |
| POST/DELETE /api/v1/orders/{id}/subscription | owner หรือ STAFF | CSRF; attach ตรวจ keys/endpoint และสถานะ |
| PATCH /api/v1/queues/{id}/advance | STAFF | CSRF และ State handler |
| GET order list/notification logs | STAFF | ตรวจ role ที่ filter และ service |
| DELETE /api/v1/orders/{id} | STAFF | CSRF, WAITING และไม่มี notification log |
| Menu mutations, staff pages | STAFF | mutations ต้องมี CSRF |
| Push demo | STAFF และ active push-demo profile | mutations ต้องมี CSRF; แยกจากบิลจริง |

public routing ของ owner endpoints ไม่ได้แปลว่าข้อมูลบิลเป็น public; service ยังตรวจ owner token
CSRF token ไม่ใช่ owner token และไม่ได้ให้สิทธิ์ STAFF
การปฏิเสธเกิดตามลำดับ filter แล้ว service: missing CSRF อาจเป็น 403 ก่อนการตรวจ login/owner/state

## Staff login และ session

`SecurityConfig.staffAuthentication` ใช้ username/password จาก configuration (`STAFF_USERNAME`, `STAFF_PASSWORD`)
password ถูก encode เป็น BCrypt ใน memory เมื่อสร้าง provider ไม่ใช่การสร้างตารางบัญชีพนักงาน
blank password ปิดการ authenticate โดยไม่มี default credential
password ที่ตั้งต้องอย่างน้อย 12 characters และไม่เกิน 72 UTF-8 bytes; username ต้องไม่ว่างและไม่เกิน 100 characters

POST `/staff/login` ต้องมี CSRF; login สำเร็จเปลี่ยน session ID และไป `/staff`
HTTP Basic ถูกปิด; staff API ใช้ session authentication
login สำเร็จล้าง CSRF token เดิม จึง fetch GET `/api/v1/csrf` ใหม่ก่อน mutation
endpoint คืน headerName/parameterName/token และ `Cache-Control: no-store`
ใช้ token กับ session ที่ fetch มาเท่านั้น; token ข้าม session ไม่ผ่าน
POST `/staff/logout` ต้องมี CSRF แล้ว invalidate session, clear authentication และลบ JSESSIONID
การ logout หนึ่ง session ไม่ได้ logout ทุก session ของ STAFF

Maven ไม่อ่าน `.env` ให้อัตโนมัติ ต้องส่ง environment/configuration เข้า process ที่รันแอปจริง
เอกสาร [Push delivery](push-delivery.md) อธิบายข้อจำกัด configuration และ Compose เพิ่มเติม
ไม่ควรใส่รหัสผ่านจริงใน test fixture, commit, command history ที่แชร์ หรือรายงานทดสอบ

## Transaction, row lock และ event

`QueueRepository.lockById` ใช้ PESSIMISTIC_WRITE; `OrderAccessServiceImpl.locked` ใช้ propagation MANDATORY
ผู้เรียกต้องมี transaction ที่ถือ lock จน mutation/flush จบ
`QueueServiceImpl` มี @Transactional ทั้งการอ่านและ mutation เพราะการอ่านผ่าน access ใช้ lock ด้วย
`QueueContext` ตรวจ actual transaction, synchronization และไม่เป็น read-only ก่อนเปลี่ยน entity
เวลาเปลี่ยนสถานะใช้ Clock และ UTC; dirty checking persist เมื่อ transaction commit

ถ้า transaction rollback สถานะ/timestamp ไม่ถูก commit และ AFTER_COMMIT observer ไม่ทำงาน
`ReadyNotificationObserver` รับ QueueStatusChangedEvent เฉพาะ READY หลัง commit (`fallbackExecution=false`)
และรับ event ของการ attach subscription เพื่อ catch-up READY ตามกฎ delivery
ระบบไม่มี transactional outbox หรือ replay อัตโนมัติ; after-commit callback ไม่ได้ทำให้ business transaction กับ provider ส่งพร้อมกันแบบ atomic
`NotificationDeliveryServiceImpl` ของธีธัช claim/log ด้วย transaction แยกและส่งออกนอก DB transaction
อ่านรายละเอียด single-attempt/ACCEPTED/PREVIEW ที่ [push-delivery.md](push-delivery.md)

row lock ทำให้ผู้รอเห็นสถานะที่ commit แล้วก่อนใช้กฎ แต่ไม่ได้ deduplicate HTTP request
advance สองคำขอที่ถูกสิทธิ์อาจทำสองขั้นต่อเนื่องได้; ไม่มี expected-state/idempotency contract

## Error contract

`GlobalExceptionHandler` และ controller-specific handlers ใช้ JSON `status`, `error`, `message` พร้อม no-store
security filter 401/403 ใช้รูปแบบเดียวกัน; ข้อผิดพลาดจาก validation/parse/conversion ตอบข้อความทั่วไป 400
business exceptions เก็บ status/message ที่กำหนดไว้ เช่น owner denied 403, missing order 404, state conflict 409
DataIntegrityViolation/ConcurrencyFailure ตอบข้อความทั่วไป 409; unexpected error ตอบทั่วไป 500
response ไม่ serialize exception cause, stack trace, SQL หรือ rejected request values
405/415 และ protocol headers ยังคงถูกส่งตาม HTTP semantics
กรณีไม่มีสิทธิ์อาจถูกปฏิเสธก่อนสถานะ/การค้นหาบิล จึงไม่ควรใช้ status code เป็นวิธีคาดเดาเจ้าของข้อมูล

## แผนที่โค้ด

ไฟล์ใต้ `code/src/main/java/com/kku/queuenotify/`:

| ไฟล์/เมธอด | หน้าที่ |
|---|---|
| common/QueueToken.java: generate/hash/matches | token และ hash comparison |
| config/SecurityConfig.java: staffAuthentication/security/json | BCrypt, filters, session, CSRF, error JSON |
| repository/QueueRepository.java: lockById | pessimistic row lock |
| service/impl/OrderAccessServiceImpl.java: isStaff/locked | role และ owner check |
| service/impl/QueueServiceImpl.java: get/advance/cancel | transaction และ handler selection |
| service/impl/QueueContext.java: requireMutation/transitionTo | transaction invariant, UTC, event |
| service/impl/*State.java: next/cancel | กฎ 5 สถานะ |
| service/impl/OrderServiceImpl.java: create/get/update/delete | token ตอนสร้างและกฎ CRUD |
| exception/GlobalExceptionHandler.java | safe error responses |
| service/impl/ReadyNotificationObserver.java | after-commit bridge ไป delivery |

หลักฐานแต่ละส่วนแยก unit/filter/API/PostgreSQL tests ตาม [state-test-report.md](state-test-report.md)
ผล automated tests ไม่ใช่หลักฐานว่า deployment, browser หรือ OS notification ผ่าน acceptance แล้ว
