# Data Dictionary

ตัวเลขเงินใช้ NUMERIC(10,2)/BigDecimal บาท เวลาที่ระบบใหม่เขียนใช้ UTC. PK ที่เป็น BIGSERIAL auto increment; FK ไม่มี cascade ลบเมนูหรือ subscription ไปยังออเดอร์.

| Table | Columns / constraints | Meaning |
|---|---|---|
| menu_item | id PK; name varchar100 required; category varchar50 optional; price numeric(10,2) >=0; prep_time_minutes int optional; is_available boolean default true; image_key varchar64 nullable SHA-256 ของภาพ | เมนูราคาและสถานะขาย |
| orders | id PK; total_amount numeric(10,2) required; created_at/updated_at timestamp required; push_subscription_id optional FK | ออเดอร์ anonymous ไม่มีชื่อ/โทร/บัญชี |
| order_item | id PK; order_id FK cascade SQL; menu_item_id FK restrict; quantity int >0; unit_price/subtotal numeric(10,2); menu_item_name varchar100 snapshot; unique(order_id,menu_item_id) | รายการและราคาขณะสั่ง |
| queue | id PK+FK orders.id; queue_number int; queue_date date required; UNIQUE(queue_date,queue_number); status varchar20; status_changed_at timestamp; token_hash varchar64 UNIQUE optional only legacy | 1:1 คิว/ออเดอร์; token SHA256 ไม่ใช่เลขคิว |
| push_subscription | id PK; endpoint text; endpoint_hash varchar64 UNIQUE; p256dh/auth text; is_active boolean; created_at/updated_at timestamp | ข้อมูลส่ง Push เก็บ server-only |
| notification_log | id PK; queue_id FK cascade SQL; channel varchar20; message varchar255; success boolean; sent_at timestamp nullable; attempted_at nullable legacy; event_type varchar30; delivery_status varchar20; http_status int nullable | PENDING/ACCEPTED/FAILED/PREVIEW หรือ LEGACY; ไม่ยืนยัน browser delivery |

Legacy fields: orders.customer_id/status nullable คงค่าเดิม; customer และ notification_preference ยังอยู่แต่ระบบใหม่ไม่ map relation จาก Order ไป Customer. Entity legacy ที่แยกอยู่เพื่อ validate schema ไม่ใช่ฟังก์ชันสมัครลูกค้า.

| Legacy table | Columns / constraints | Meaning |
|---|---|---|
| customer | id BIGSERIAL PK; name varchar100 NOT NULL; phone varchar20 NOT NULL; email varchar100 nullable; created_at TIMESTAMP NOT NULL DEFAULT now() | เก็บข้อมูลเดิม ไม่สร้างลูกค้าใน anonymous flow |
| notification_preference | id BIGINT PK/FK customer.id; channel varchar20 NOT NULL; contact_value varchar150 NOT NULL; is_active BOOLEAN NOT NULL DEFAULT true | เก็บ preference เดิม ไม่ใช้แทน push_subscription ใหม่ |

รวม 10 ตารางแอป: 6 หลัก + 2 legacy + ภาพเมนู + counter; ไม่นับ flyway_schema_history.
orders.customer_id เป็น FK nullable ไป customer; orders.status varchar20 nullable เป็นค่าเก่า ไม่ใช่สถานะคิวปัจจุบัน.
push_subscription.created_at/updated_at เป็น TIMESTAMP NOT NULL default UTC; notification_log.delivery_status/event_type NOT NULL default LEGACY.
queue.status_changed_at nullable ตาม schema เดิม; token_hash nullable สำหรับ legacy แต่ flow สร้างใหม่ต้องออก token/hash.

ตารางเสริม V4: `menu_item_image` มี `menu_item_id` PK/FK → menu_item(id) แบบ 1:0..1 และ ON DELETE CASCADE, `image_data` BYTEA required ขนาด 1–2097152 bytes, `content_type` varchar32 required เฉพาะ image/jpeg หรือ image/png. แยก bytes จากตารางเมนูเพื่อไม่โหลดภาพในการอ่านรายการเมนู.

ตารางตัวนับ V5: `queue_daily_counter` มี `queue_date` DATE PK และ `last_number` INTEGER required >=0. ออกเลขด้วย INSERT ON CONFLICT DO NOTHING และ UPDATE ที่ล็อกแถวจน transaction สร้างออเดอร์สิ้นสุด แล้วอ่านเลขของ transaction นั้น. วันคิวใช้ Asia/Bangkok, เริ่ม 1 ต่อวัน, ไม่วนเลขเมื่อเกิน 999. Counter ไม่ถูกลบเมื่อลบออเดอร์ จึงไม่ใช้เลขเดิมซ้ำในวันนั้น.

A17 หลัง review: OrderServiceImpl inject QueueNumberService interface → QueueNumberServiceImpl เลือกวันจาก Clock → DailyQueueCounterRepository interface → JdbcDailyQueueCounterRepository รับผิดชอบ SQL. Service/Repository implementation ต้องอยู่ใน transaction ของออเดอร์ (MANDATORY); ไม่มีการออกเลขนอก transaction หรือ MAX+1 ขณะรับคำสั่งใหม่.

V2 เติม snapshot เก่าจากชื่อเมนูปัจจุบันและ subtotal/quantity จึงเป็น reconstruction ไม่ยืนยันชื่อในอดีต; updated_at เก่าเริ่มจาก created_at. V5 คำนวณ queue_date โดยสมมติ created_at เก่าเป็น UTC; ต้องยืนยัน timezone ของข้อมูลปลายทางก่อนใช้. created_at และค่าคอลัมน์เก่าไม่ถูกแปลงทับ.

Indexes: order_item(order_id), order_item(menu_item_id), orders(push_subscription_id), orders(created_at,id), queue(status,queue_number), notification_log(queue_id), endpoint_hash UNIQUE และ partial UNIQUE notification_log(queue_id,event_type) เฉพาะ READY กัน event/re-subscribe ซ้ำ.

JPA: Order→items ALL+orphanRemoval; Order→Queue ALL+orphanRemoval เพราะเป็น owned lifecycle. Relation ไป MenuItem/Subscription ไม่มี cascade delete. ManyToOne และ OneToOne กำหนด LAZY แต่ inverse OneToOne ของ Hibernate อาจมี eager lookup เพื่อ resolve null; ตรวจ query จริงก่อนอ้างว่าไม่มี N+1.

Validation API มีเพดาน quantity 99, items 50, menu price 8 integer digits +2 decimal, prepTime 0–240. DB constraint ไม่ได้แทน DTO validation ทั้งหมด.
