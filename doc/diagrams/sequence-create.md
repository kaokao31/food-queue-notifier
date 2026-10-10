# Create order sequence

```mermaid
sequenceDiagram
 actor Customer
 participant UI as Checkout UI
 participant API as OrderController
 participant S as OrderService
 participant T as QueueTokenGenerator (C interface)
 participant DB as PostgreSQL
 Customer->>UI: ยืนยันตะกร้า
 UI->>API: POST items (ID, quantity)
 API->>S: validated request
 S->>T: require provider
 alt ไม่มี provider จริง
  S-->>API: 503
 else มี provider
  S->>DB: transaction + lock menus ตาม ID
  S->>S: validate availability, calculate snapshots/total
  S->>T: generate raw token
  S->>DB: allocate number for Bangkok date
  S->>T: hash token
  S->>DB: persist order/items/queue; commit
  S-->>API: response + raw token เฉพาะครั้งสร้าง
  API-->>UI: 201 Location
  UI->>UI: เก็บ token ต่อบิล แล้วไป /queue/id
 end
```

เมื่อ exception ภายใน transaction การบันทึกและ daily counter rollback ไม่มี subscription หรือ notification side effect ใน create flow ปัจจุบัน
