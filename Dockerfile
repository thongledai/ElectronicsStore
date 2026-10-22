# ==============================================================================
# Stage 1: Build Application
# Tối ưu bộ nhớ Maven để không bị OOM khi Render build (Render giới hạn RAM)
# ==============================================================================
FROM maven:3.9-eclipse-temurin-22 AS builder

WORKDIR /build

# Giới hạn RAM cho tiến trình build Maven (tránh bị Render kill lúc build)
ENV MAVEN_OPTS="-Xms128m -Xmx384m -XX:+UseSerialGC"

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy mã nguồn và đóng gói JAR (bỏ qua unit tests)
COPY src ./src
RUN mvn clean package -DskipTests

# ==============================================================================
# Stage 2: Runtime Environment (Tối ưu đặc biệt cho gói Free 512MB RAM của Render)
# ==============================================================================
FROM eclipse-temurin:22-jre

WORKDIR /app

# Tạo user không phải root để bảo mật
RUN groupadd -r spring && useradd -r -g spring spring

# Copy file jar từ build stage
COPY --from=builder /build/target/*.jar app.jar

RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080 10000

# Cấu hình JVM tối ưu tuyệt đối cho giới hạn 512MB:
# 1. -Xms96m -Xmx200m: Khống chế Heap tối đa 200MB (chừa ~300MB cho OS + Metaspace + Native)
# 2. -Xss256k: Giảm kích thước stack mỗi thread từ 1MB xuống 256KB
# 3. -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=128m: Khống chế Metaspace
# 4. -XX:ReservedCodeCacheSize=48m: Giảm bộ nhớ CodeCache (mặc định 240MB)
# 5. -XX:+UseSerialGC: GC tốn ít RAM overhead nhất, phù hợp nhất cho 1 vCPU / 512MB
# 6. -XX:TieredStopAtLevel=1: Tắt C2 compiler -> tránh spike RAM đột biến lúc khởi động
# 7. -XX:+ExitOnOutOfMemoryError: Thoát ngay khi lỗi bộ nhớ để Render restart sạch sẽ
ENV JAVA_OPTS="-Xms96m -Xmx200m -Xss256k -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"

# Tự động nhận biến $PORT từ Render (nếu có), fallback về $SERVER_PORT hoặc 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-${SERVER_PORT:-8080}} -jar app.jar"]
