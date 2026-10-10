# ==========================================
# Stage 1: Build the application
# ==========================================
FROM maven:3.9.16-eclipse-temurin-25 AS builder

WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build JAR package (skip unit tests during image build)
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Minimal Runtime Environment
# ==========================================
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create non-root user for security best practice
RUN groupadd -r spring && useradd -r -g spring spring

# Copy jar from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set permissions for non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port (default 8080, can be overridden via SERVER_PORT)
EXPOSE 8080

# Container-aware JVM memory options optimized for 512MB RAM limit
ENV JAVA_OPTS="-Xms128m -Xmx240m -Xss256k -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]