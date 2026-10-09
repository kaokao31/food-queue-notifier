# ER diagram (V1–V5)

```mermaid
erDiagram
 CUSTOMER ||--o| NOTIFICATION_PREFERENCE : legacy
 CUSTOMER o|--o{ ORDERS : optional
 PUSH_SUBSCRIPTION o|--o{ ORDERS : optional
 ORDERS ||--o{ ORDER_ITEM : contains
 MENU_ITEM ||--o{ ORDER_ITEM : referenced
 MENU_ITEM ||--o| MENU_ITEM_IMAGE : image
 ORDERS ||--o| QUEUE : shared_primary_key
 QUEUE ||--o{ NOTIFICATION_LOG : history
 QUEUE_DAILY_COUNTER {
  date queue_date PK
  int last_number
 }
 QUEUE {
  bigint id PK,FK
  date queue_date
  int queue_number
  string status
  string token_hash UK
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
 MENU_ITEM_IMAGE {
  bigint menu_item_id PK,FK
  bytea image_data
  string content_type
 }
```

ชื่อ uppercase เป็นชื่อ logical ของ table SQL จริงใช้ lowercase `menu_item_image` Queue unique(queue_date,queue_number); counter ไม่ผูก FK กับคิว READY log unique แบบ partial index Subscription/Log เป็น data model รอ T implementation
