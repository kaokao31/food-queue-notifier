# Data dictionary

อ้างอิง SQL V1–V5 และ JPA entities ปัจจุบัน ชื่อ table รูปภาพคือ `menu_item_image`

| Table | Key / fields | เงื่อนไขและบทบาท |
|---|---|---|
| customer | id; name, phone, email, created_at | ข้อมูล legacy; anonymous order ไม่จำเป็นต้องมี customer |
| notification_preference | id FK customer; channel, contact_value, is_active | legacy preference |
| menu_item | id; name, category, price numeric(10,2), prep_time_minutes, is_available, image_key | price >= 0; image_key ใช้ URL/hash ของภาพ |
| menu_item_image | menu_item_id PK/FK menu_item; image_data bytea, content_type | FK ON DELETE CASCADE; image/jpeg หรือ image/png; bytes 1–2097152 |
| orders | id; customer_id nullable, status nullable, total_amount, created_at, updated_at, push_subscription_id nullable | anonymous orders; queue.status เป็นสถานะคิวที่ใช้งานใหม่ |
| order_item | id; order_id, menu_item_id, quantity, subtotal, unit_price, menu_item_name | unique(order_id,menu_item_id); quantity > 0, unit_price >= 0; snapshot ชื่อและราคา |
| queue | id PK/FK orders; queue_number, queue_date, status, status_changed_at, token_hash | unique(queue_date,queue_number), queue_date NOT NULL, token_hash unique; legacy hash อาจ null |
| queue_daily_counter | queue_date PK; last_number | last_number >= 0; increment ภายใต้ transaction/row lock |
| push_subscription | id; endpoint, endpoint_hash, p256dh, auth, is_active, created_at, updated_at | endpoint_hash unique; เตรียมให้ T ใช้งาน ไม่ได้ยืนยัน subscription แล้ว |
| notification_log | id; queue_id, channel, message, success, sent_at, delivery_status, http_status, attempted_at, event_type | unique(queue_id,event_type) เฉพาะ READY; default LEGACY สำหรับข้อมูลเก่า |

เวลา timestamp เก็บตาม UTC; วันที่เลขคิวคำนวณ Asia/Bangkok ไม่เก็บ token ดิบใน queue ตาราง counter ไม่มี FK กับคิว เพราะการลบคิวต้องไม่ทำให้เลขถูกใช้ซ้ำ

Validation API เพิ่มข้อจำกัดรายการ 1–50, quantity 1–99, ราคา 8 หลัก/2 ทศนิยม และภาพไม่เกิน 2 MiB/6000 ต่อด้าน/12 ล้าน pixels ตามโค้ด ไม่ใช่ทั้งหมดเป็น SQL constraints
