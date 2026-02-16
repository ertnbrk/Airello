# ================================
# Stage 1: Build
# ================================
FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /build

# Copy Gradle wrapper and build files first (for better layer caching)
COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .

# Download dependencies (cached if build.gradle doesn't change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and config
COPY src src
COPY config config

# Build the application (skip tests for Docker build, run in CI)
RUN ./gradlew bootJar --no-daemon -x test && \
    ls -lh build/libs/ && \
    echo "Build completed successfully"

# ================================
# Stage 2: Runtime
# ================================
FROM eclipse-temurin:21-jre-alpine

# Install dumb-init for proper signal handling and wget for health checks
RUN apk add --no-cache dumb-init wget

WORKDIR /app

# Create non-root user
RUN addgroup -S planmate && adduser -S planmate -G planmate

# Copy JAR from builder stage
COPY --from=builder /build/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R planmate:planmate /app

# Switch to non-root user
USER planmate:planmate

# Expose Spring Boot port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Use dumb-init to handle signals properly (graceful shutdown)
ENTRYPOINT ["dumb-init", "--"]

# Run the application with production-ready JVM options
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:+UseG1GC", \
     "-XX:+OptimizeStringConcat", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-jar", "app.jar"]
