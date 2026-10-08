# ขอบเขต review และพัฒนาต่อ 4 คน

ขอบเขตในตารางคือความรับผิดชอบในการตรวจและพัฒนาต่อ ไม่ถือว่าแบ่งไฟล์แล้วเปลี่ยนผู้เขียนย้อนหลังได้. สมาชิกต้องตรวจ/ทดสอบ/ปรับงานด้วยตัวเองและใช้บัญชีตน commit/push ตาม contribution จริง. จำนวน commit ต้องเกิดจากงานจริงกระจายตามเวลา ไม่ใช่โควตาที่เติมด้วย empty commits.

| คน | ขอบเขตตรวจและต่อยอด | หลักฐานที่ควรทำ |
|---|---|---|
| กานดิทัต | templates, app.js/app.css, WebController, QrController/QrService, Docker/CI | customer/staff UI, quantities/history, QR, deployment checks and tests |
| อนันต์เอกก์ | migrations, entities/repos, Menu/Image/Order service/API/Mapper, QueueNumberService | V1 upgrade, price snapshots, CRUD/uploads, daily counter/rollback/concurrency, dictionary consistency |
| คมชาญ | QueueController/QueueService/QueueContext/States, QueueToken, OrderAccessService, SecurityConfig | state matrix, cross-order token, queue/edit concurrency, login/logout |
| ธีธัช | OrderSubscriptionService, SubscriptionValidator, after-commit listener, delivery service/sender/logs, Strategy | subscription validation, provider failure/410 tests, duplicate event proof, notification log display |

โค้ดพื้นฐานบางส่วนของคมชาญและธีธัชอยู่ใน K02 ของกานดิทัตแล้ว ประวัติ commit เดิมคงไว้ตามจริง ทั้งสองคนรับช่วงตรวจ แก้ ทดสอบ และพัฒนาต่อ แล้วบันทึก contribution ใหม่ด้วยบัญชีตนเอง.

ลำดับ integration: foundation/schema → menu/order → queue/security → subscription/delivery → frontend/e2e → docs/release. หลายไฟล์มี dependencies ข้ามชุด อย่าเลือก git add ตามตารางแล้วสมมติว่าทุก commit build ได้ ให้ตรวจ staged diff และ build boundary จริงก่อนแต่ละ commit. ไม่เปลี่ยน author/date ของงานเดิม.

PR description ต้องบอกปัญหา/พฤติกรรมใหม่/ผลทดสอบ/ข้อจำกัด. Review จริงโดยเพื่อนก่อน merge develop และ release main.
