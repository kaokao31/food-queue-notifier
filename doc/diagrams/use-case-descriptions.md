# Use-case descriptions

อ้างอิง `develop` baseline `64b3ce7` วันที่ 10 ตุลาคม 2026

| Use case | Actor / เงื่อนไข | ผลและข้อจำกัด |
|---|---|---|
| Browse menu | ลูกค้าหรือพนักงาน | ลูกค้าเห็นเมนูเปิดขาย; STAFF ขอ includeUnavailable ได้ UI filter/sort/paging และ API ส่ง imageUrl ที่บันทึกไว้ |
| Create order | ลูกค้า, CSRF, รายการถูกต้อง | Server ตรวจเมนู/ราคา/จำนวน สร้าง snapshots, token hash และเลขคิวรายวันใน transaction คืน ID/เลขคิว/token ให้หน้าเว็บ |
| History / track order | มี ID และ token ของออเดอร์ หรือ STAFF | ประวัติ ID/token อยู่ในเบราว์เซอร์ การอ่านข้อมูลผ่าน API ตรวจ owner/STAFF และ polling หยุดเมื่อ terminal |
| Edit order | owner token หรือ STAFF, CSRF, WAITING | เปลี่ยนรายการและคำนวณยอดใหม่จาก server ไม่แก้เมื่อ PREPARING/READY/terminal |
| Cancel queue | owner token หรือ STAFF, CSRF, WAITING/PREPARING | เปลี่ยนเป็น CANCELLED; READY/COMPLETED/CANCELLED ปฏิเสธการยกเลิก |
| Manage menus/images | STAFF session + CSRF สำหรับ mutation | JSON/multipart validation, คงรูปเมื่อเปลี่ยนชื่อ/หมวดหมู่, อัปโหลดรูปได้ เมนูที่มีประวัติออเดอร์ลบไม่ได้ให้ปิดขาย |
| List orders / advance queue | STAFF; advance ต้อง CSRF | ดูรายการ/สถานะ และ advance WAITING → PREPARING → READY → COMPLETED ภายใต้ lock/State; terminal mutation ถูกปฏิเสธ |
| Generate QR | STAFF, URL HTTPS ของหน้าเมนูราก | สร้าง PNG ด้วย ZXing ในแอป ไม่ใส่ credentials/query/fragment หรือ path ออเดอร์ใน QR |
| Attach/detach subscription | owner token หรือ STAFF + CSRF; attach ต้องไม่ terminal | ตรวจ endpoint/keys และเชื่อมกับออเดอร์ รองรับสมัครเมื่อ READY แล้ว ปิดเฉพาะออเดอร์ไม่ถอด subscription ร่วมของทุกออเดอร์ |
| Notify READY | หลัง commit, delivery ตรวจ READY/claim และ active subscription เมื่อ webpush | console เป็น PREVIEW; webpush ส่งหนึ่งครั้งและบันทึกผล ไม่มี automatic retry ACCEPTED ไม่ยืนยันว่า OS แสดง popup |
| View notification log | STAFF | ดู status/channel/HTTP/เวลาโดยไม่คืน private keys หรือ endpoint ใน log response |
| Swagger / OpenAPI | STAFF session; เอกสาร GET เท่านั้น | ดู/ทดลอง API ในเบราว์เซอร์เดียวกับ login; mutation ต้อง CSRF และมีผลต่อข้อมูลจริง |
| Standalone Push demo | profile push-demo และ STAFF | ใช้ CSRF เป็นการทดสอบ provider แยกจาก order/log จริง |

ดู [Use-case overview](use-case.md), [State](state.md), [Security/State](../security-and-state.md) และ [Push delivery](../push-delivery.md) ผลทดสอบระบบ/อุปกรณ์ดูตาม baseline ในรายงาน ไม่ถือว่าตารางนี้เป็น acceptance ที่ตรวจครบแล้ว
