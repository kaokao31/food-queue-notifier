# ER diagram — V1–V6

อ้างอิง SQL migrations ใน `develop` baseline `64b3ce7` วันที่ 10 ตุลาคม 2026 แสดงทั้งตารางที่ใช้ปัจจุบันและตาราง legacy โดยเลือกคอลัมน์สำคัญมาประกอบความสัมพันธ์

```mermaid
erDiagram
  CUSTOMER ||--o| NOTIFICATION_PREFERENCE : legacy
  CUSTOMER o|--o{ ORDERS : optional_legacy
  PUSH_SUBSCRIPTION o|--o{ ORDERS : optional_attachment
  ORDERS ||--o{ ORDER_ITEM : contains
  MENU_ITEM ||--o{ ORDER_ITEM : referenced
  MENU_ITEM ||--o| MENU_ITEM_IMAGE : uploaded_image
  ORDERS ||--o| QUEUE : shared_primary_key
  QUEUE ||--o{ NOTIFICATION_LOG : history
  CUSTOMER {
    bigint id PK
    string name
    string phone
    string email
  }
  NOTIFICATION_PREFERENCE {
    bigint id PK,FK
    string channel
    string contact_value
    boolean is_active
  }
  ORDERS {
    bigint id PK
    bigint customer_id FK "nullable legacy"
    string status "nullable legacy"
    bigint push_subscription_id FK "nullable"
    decimal total_amount
    timestamp created_at
    timestamp updated_at
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
    string image_key "nullable; asset or uploaded key"
  }
  MENU_ITEM_IMAGE {
    bigint menu_item_id PK,FK
    bytea image_data
    string content_type
  }
  QUEUE {
    bigint id PK,FK
    date queue_date
    int queue_number
    string status
    string token_hash UK "nullable for legacy queues"
    timestamp status_changed_at
  }
  QUEUE_DAILY_COUNTER {
    date queue_date PK
    int last_number
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
    string event_type
    string delivery_status
    string channel
    string message
    boolean success
    int http_status "nullable"
    timestamp attempted_at
    timestamp sent_at "nullable"
  }
```

ชื่อ uppercase ใน diagram เป็น logical name; SQL จริงใช้ lowercase เช่น orders และ menu_item_image

- Queue unique(queue_date,queue_number) และ token_hash unique; counter ไม่ผูก FK กับคิว
- OrderItem unique(order_id,menu_item_id) และเก็บ price/name snapshot
- READY log unique แบบ partial index บน (queue_id,event_type) เฉพาะ event_type='READY' เพื่อ durable claim ป้องกันส่งซ้ำ
- Subscription หนึ่งรายการผูกหลายออเดอร์ได้; การ detach ถอด association ของออเดอร์นั้น
- V6 เติมค่า menu_item.image_key เท่านั้น ไม่เพิ่มตาราง/คอลัมน์ และไม่เขียนทับ uploaded image
- Customer/NotificationPreference และ orders.customer_id/status เป็น legacy; flow ลูกค้าใหม่ไม่ต้องสมัครบัญชีและสถานะปัจจุบันอยู่ใน Queue

Subscription/Log มี implementation ใช้งานแล้ว ดู [Migration](../migration.md), [Domain](domain.md), [Push delivery](../push-delivery.md) และ SQL จริงใน code/src/main/resources/db/migration สำหรับรายละเอียด constraints ทั้งหมด
