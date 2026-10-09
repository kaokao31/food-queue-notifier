# Use-case descriptions

| Use case | ผลและเงื่อนไข |
|---|---|
| Browse menu | แสดงเฉพาะเปิดขายสำหรับลูกค้า; filter/sort/paging บน UI |
| Create order | ตรวจรายการ/ราคา server, snapshots, token provider จริง และ daily counter ใน transaction |
| View/edit order | ตรวจสิทธิ์ผ่าน access provider; แก้ได้เฉพาะ WAITING |
| Manage menus/images | staff provider, JSON/multipart validation และป้องกันลบประวัติ |
| Generate QR | staff provider, HTTPS root URL, สร้าง PNG local; ไม่ส่งข้อมูลให้บริการภายนอก |
| Change/cancel status | รอ C และต้องตรวจ State/security จริง |
| Subscribe/notify/log | รอ T และต้องตรวจอุปกรณ์จริงผ่าน HTTPS |
