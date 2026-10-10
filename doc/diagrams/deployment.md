# Deployment / runtime

อ้างอิง deployment ของ `develop` revision `c07292e` หลัง PR #81 วันที่ 10 ตุลาคม 2026 ตั้งค่า Neon/Render แล้ว และผู้ใช้ยืนยันการสร้างออเดอร์กับรับ Web Push เลขคิวจริงถูกต้องบน URL ด้านล่าง

## Local runtime ที่ใช้ทดสอบ

```mermaid
flowchart LR
  B[Browser localhost:8080] --> A[Spring Boot - Maven / JDK 21 on host]
  A -->|localhost:5433| DB[Docker Compose db - PostgreSQL 16]
  DB --> VOL[(queue_restart_data)]
  A -->|webpush mode / VAPID| PUSH[External Web Push provider]
  PUSH --> SW[Browser Service Worker / OS notification]
```

การทดสอบในเครื่องใช้ Maven รันแอปและ Compose รัน db โดยส่ง STAFF/VAPID/NOTIFICATION_MODE ผ่าน environment ของ Git Bash provider/Service Worker แสดงเส้นทาง Push ซึ่งต้องตั้งค่าและสมัครจริง ไม่ได้ยืนยัน popup ทุกอุปกรณ์ ดู [README](../../README.md)

## Compose app ที่มี configuration อยู่

```mermaid
flowchart LR
  B[Browser localhost:PORT] -->|127.0.0.1 published port| APP[Compose app - Java 21 image]
  APP -->|db:5432 on Compose network| DB[Compose db - PostgreSQL 16]
  DB --> VOL[(queue_restart_data)]
```

Compose app รับ DB/PORT แต่ยังไม่ส่ง STAFF/VAPID/mode/profile ให้ container จึงต้องปรับก่อนใช้ทดสอบ STAFF/Web Push แบบครบ ระบบเก็บข้อมูลใน named volume; down เก็บ volume ไว้ Docker image build ข้าม tests

## CI ที่กำหนดไว้

```mermaid
flowchart LR
  PR[PR to develop / main] --> CI[GitHub Actions verify job]
  PUSH[Push to develop / main] --> CI
  CI --> JS[JavaScript suites - Node.js 22]
  CI --> JAVA[Maven clean verify - Java 21 / isolated PostgreSQL]
  JAVA --> REPORT[(Surefire reports artifact)]
```

Workflow อยู่ใน [.github/workflows/ci-cd.yml](../../.github/workflows/ci-cd.yml) ภาพนี้แสดง configuration ไม่ได้อ้างว่า CI run ล่าสุดผ่านโดยไม่มีผล run ให้ตรวจ workflow run ของ revision ที่ส่งมอบด้วย GitHub Actions ยังไม่มี publish/deploy job; Render เป็นบริการ deploy แยกกัน และสถานะ Auto-Deploy ของ service ไม่ได้ยืนยันจากหลักฐานรอบนี้

## Cloud runtime ที่ทดสอบแล้ว

```mermaid
flowchart LR
  B[Browser - Render HTTPS URL] -->|HTTPS| EDGE[Render TLS endpoint]
  EDGE --> APP[Render Docker - Java 21 Spring Boot]
  APP -->|JDBC TLS direct connection| DB[Neon PostgreSQL 16 - Singapore]
  ENV[Render Environment - DB / STAFF / VAPID] --> APP
  APP -->|webpush / VAPID| PUSH[Browser Push provider]
  PUSH --> SW[Browser Service Worker]
  SW --> DEVICE[OS notification - actual queue number]
```

URL: [food-queue-notifier-1.onrender.com](https://food-queue-notifier-1.onrender.com/)

Render Web Service ใช้ Free plan, Singapore, Git branch `develop`, Dockerfile ที่ราก repo และ Root Directory ว่าง แอปรับ PORT จาก Render; log ที่ตรวจแสดง 10000 หน้าเว็บรับ HTTPS จาก Render endpoint

Neon ใช้ Free plan, Singapore, PostgreSQL 16, branch `production` และ database `neondb` ชื่อ branch ฐานข้อมูลไม่ได้หมายความว่า Git revision เป็น final release Direct connection เปิด TLS และ Flyway ใช้ datasource เดียวกับแอป V1–V6 บนฐานใหม่ผ่านแล้ว

DB credentials, STAFF password และ VAPID key pair ตั้งใน Environment ของ Render ไม่รวม secrets ลง diagram หรือ repository การทดสอบด้วย Maven ในเครื่องที่ชี้ฐาน Neon เดียวกันใช้ข้อมูลร่วมกับ Render ไม่ใช่ข้อมูลใน Docker เดิม

ผู้ใช้ยืนยันสร้างออเดอร์และรับแจ้งเตือนเลขคิวจริงบน URL นี้แล้ว หลักฐานเป็นผลรอบที่ทดสอบ ไม่รับรองทุกอุปกรณ์หรือแปลผล provider ACCEPTED ว่า popup แสดงเสมอ ดู [Deployment test report](../deployment-test-report.md)

## การส่งมอบที่ยังเหลือ

Render Free อาจพักเมื่อไม่ได้ใช้งาน จึงเปิดเว็บล่วงหน้าก่อนสาธิต เก็บภาพ acceptance และข้อมูลอุปกรณ์/เบราว์เซอร์โดยปิดบัง credentials และ owner tokens

หลังตรวจเอกสาร/สไลด์และ final acceptance ให้ merge `develop` → `main` ผ่าน PR แล้วเปลี่ยน Render branch เป็น `main` และ deploy revision นั้น ตรวจผลและบันทึก SHA ใหม่ก่อนส่งมอบ การเปลี่ยน Git branch ไม่ได้ย้ายหรือลบฐานข้อมูล Neon
