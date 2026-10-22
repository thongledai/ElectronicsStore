# ==========================================
# Stage 1: Build application with Maven & JDK 22
# ==========================================
FROM maven:3.9-eclipse-temurin-22 AS builder

WORKDIR /build

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Minimal Runtime Environment (JRE 22)
# ==========================================
FROM eclipse-temurin:22-jre

WORKDIR /app

# Create non-root user for security best practice
RUN groupadd -r spring && useradd -r -g spring spring

# Copy compiled JAR file from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set ownership
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose default application port
EXPOSE 8080

# Environment variables & JVM tuning (adjust if needed)
ENV SERVER_PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
