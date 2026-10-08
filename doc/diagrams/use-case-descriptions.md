# Use Case Descriptions

| Use case | Preconditions | Main flow | Alternative | Postcondition |
|---|---|---|---|---|
| สร้างออเดอร์ | เปิดเว็บและมีเมนูขาย | เลือก items → confirm → backend calculates → transaction create → token/queue | validation 400, missing menu404, unavailable409, rollback all | order/items/queue persisted together |
| เปิดรับ Push | มี order token บนเครื่องเดิม, HTTPS, Android Chrome | permission → register SW → subscribe → attach via token | permission denied/unsupported → polling; malformed subscription400 | subscription persisted and attached only to authorized order |
| ติดตามคิว | token ถูกต้องหรือ staff login | GET order/queue → render current status → poll while visible | token missing/wrong403; order404 | ไม่ expose ข้อมูลออเดอร์อื่น |
| เปลี่ยนเป็น READY | staff logged in, CSRF, queue PREPARING | lock → State transition → commit → Observer claim → Strategy → log | no active sub skips; provider error logsFAILED | READY remains even if Push fails |
| แก้ออเดอร์ | owner/staff, queue WAITING | lock queue → replace items → calculate current prices → commit | state409, invalid items400, rollback preserves original | updated order lines/total consistent |
| ลบออเดอร์ | staff, WAITING, no notification log | delete owned queue/items/order in transaction | not staff401/403; state/history409 | order removed; subscriptions/menu preserved |
| จัดการเมนู | staff login and CSRF | create/read/update/delete via DTO and service | historical references409 → close sales instead | menu changes never alter existing snapshots |
