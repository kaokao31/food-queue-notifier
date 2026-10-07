# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# copy pom.xml ก่อนเพื่อให้ Docker cache dependency layer ได้ (เร็วขึ้นตอน build ซ้ำ)
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Stage 2: Run ----------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# รันด้วย non-root user เพื่อความปลอดภัย
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# ใช้ ${PORT} เผื่อ platform (เช่น Render/Railway) กำหนด port มาให้ผ่าน env
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
