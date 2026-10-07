# Food Queue Notifier

โปรเจกต์ระบบแจ้งเตือนคิวรับอาหารและเครื่องดื่ม
รายวิชา CP353002 Principles of Software Design and Development

## Development workflow

- main: รุ่นพร้อมส่งมอบ
- develop: รวมงานระหว่างพัฒนา
- สมาชิกทำงานบน branch ส่วนตัว
- รวมงานผ่าน Pull Request และมีสมาชิกอีกคนรีวิว

## Starting point

นำเข้าโค้ดตั้งต้นจาก queue-notify-project.zip
ประกอบด้วยโครง Entity, Design Patterns, Queue API และ Docker configuration

ยังไม่ได้ยืนยันผล build และการเริ่มระบบ
จะตรวจและปรับแก้ในขั้นตอนถัดไป
## Project structure

```text
code/src/main/java/       Application source code
code/src/main/resources/  Application configuration and Flyway migrations
test/java/               Unit and integration tests
test/resources/          Test configuration and fixtures
doc/                     Documentation and diagrams
img/                     Screenshots and other media
pom.xml                  Maven build configuration
Dockerfile               Application container build
docker-compose.yml       Local application and PostgreSQL
.github/workflows/       CI/CD workflows
```

Maven source and test directories are configured in `pom.xml`. Run commands
from the repository root. Build output remains in `target/`, so the existing
CI artifact path stays unchanged.

## Build and tests

Requirements: JDK 17 or newer and Maven 3.9.x.

```bash
mvn clean verify
```

Tests belong in `test/java/`, with the Java package directory structure.
Test resources belong in `test/resources/`. These folders currently contain
placeholders only; a successful build does not yet demonstrate test coverage.

## Run locally with Docker

With Docker and Docker Compose installed, run from the repository root:

```bash
docker compose up --build
```

Swagger UI: http://localhost:8080/swagger-ui.html

The Compose database credentials are for local development only. Database
mapping and application startup still need to be verified separately.
