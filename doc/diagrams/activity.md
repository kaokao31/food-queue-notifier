# Activity

```mermaid
flowchart TD
  M[เลือกเมนู] --> B[ตะกร้า]
  B --> O[Order API]
  O --> P{มี token provider จริง?}
  P -->|ยังไม่มี| U[503 ยังไม่พร้อม]
  P -->|มี| D[transaction: snapshots และเลขคิว]
  D --> Q[ติดตามคิวด้วย token]
  Q --> C[รอ C: เปลี่ยนสถานะ]
  C --> T[รอ T: แจ้งเตือนเมื่อพร้อม]
```

เส้นที่ระบุรอ C/T เป็นงานที่ยังไม่รวมในระบบ; deployment เป็น configuration ที่ต้องตรวจรันจริง
