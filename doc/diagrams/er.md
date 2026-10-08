# ER — TEAM-HANDOFF V1–V5

```mermaid
erDiagram
 ORDERS ||--|| QUEUE : "shared PK id"
 ORDERS ||--|{ ORDER_ITEM : owns
 MENU_ITEM ||--o{ ORDER_ITEM : referenced
 MENU_ITEM ||--o| MENU_ITEM_IMAGE : picture
 PUSH_SUBSCRIPTION o|--o{ ORDERS : optional
 QUEUE ||--o{ NOTIFICATION_LOG : attempts
 CUSTOMER ||--o| NOTIFICATION_PREFERENCE : legacy_setting
 CUSTOMER o|--o{ ORDERS : legacy_customer_id
 ORDERS {
   bigint id PK
   bigint customer_id FK "nullable legacy"
   string status "nullable legacy"
   decimal total_amount
   timestamp created_at
   timestamp updated_at
   bigint push_subscription_id FK
 }
 QUEUE {
   bigint id PK,FK
   int queue_number
   date queue_date
   string status
   string token_hash UK
   timestamp status_changed_at
 }
 ORDER_ITEM {
   bigint id PK
   bigint order_id FK
   bigint menu_item_id FK
   int quantity
   decimal unit_price
   decimal subtotal
   string menu_item_name
 }
 MENU_ITEM {
   bigint id PK
   string name
   string category
   decimal price
   int prep_time_minutes
   boolean is_available
   string image_key
 }
 MENU_ITEM_IMAGE {
   bigint menu_item_id PK,FK
   bytea image_data
   string content_type
 }
 PUSH_SUBSCRIPTION {
   bigint id PK
   string endpoint
   string endpoint_hash UK
   string p256dh
   string auth
   boolean is_active
   timestamp created_at
   timestamp updated_at
 }
 NOTIFICATION_LOG {
   bigint id PK
   bigint queue_id FK
   string channel
   string message
   boolean success
   timestamp sent_at
   timestamp attempted_at
   string delivery_status
   int http_status
   string event_type
 }
 CUSTOMER {
   bigint id PK
   string name
   string phone
   string email
   timestamp created_at
 }
 NOTIFICATION_PREFERENCE {
   bigint id PK,FK
   string channel
   string contact_value
   boolean is_active
 }
 QUEUE_DAILY_COUNTER {
   date queue_date PK
   int last_number
 }

```

`QUEUE` ใช้ UNIQUE(queue_date, queue_number) ร่วมกัน ไม่ใช่ queue_number เพียงตัวเดียว. ตารางเทคนิค `QUEUE_DAILY_COUNTER` เก็บ queue_date DATE PK และ last_number INT ต่อวัน ไม่ใช่ FK ของคิว; ภายใน transaction ใช้ row lock เพื่อออกเลขพร้อมกันโดยไม่ซ้ำ.

รวม 10 ตารางแอป: 6 หลัก + 2 legacy + ภาพเมนู + counter; ไม่นับ Flyway history.
ORDER_ITEM มี UNIQUE(order_id,menu_item_id); NOTIFICATION_LOG มี partial UNIQUE(queue_id,event_type) เฉพาะ READY.
Customer/status เก่ายังคงใน DB แม้ anonymous Order ไม่ map relation นี้. queue.status เป็น operational state.
