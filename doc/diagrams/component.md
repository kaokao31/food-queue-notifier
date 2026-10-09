# Component

```mermaid
flowchart LR
  B[Browser UI] --> W[Spring MVC/API]
  W --> S[Services/Mapper]
  S --> R[Repositories]
  R --> P[(PostgreSQL)]
  S -. รอรวม .-> C[C: State/token/security]
  S -. รอรวม .-> T[T: Subscription/Push/Log]
  W --> Q[Local ZXing QR]
```

เส้นที่ระบุรอ C/T เป็นงานที่ยังไม่รวมในระบบ; deployment เป็น configuration ที่ต้องตรวจรันจริง
