# =============================================================================
# OpenBAS Intelligence Backend Dockerfile
# Multi-stage build for minimal production image
# =============================================================================

# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy POM first for layer caching (dependencies won't re-download unless POM changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Create non-root user for security
RUN groupadd -r openbas && useradd -r -g openbas openbas

# Copy built JAR
COPY --from=build /app/target/openbas-intelligence-backend-*.jar app.jar

# Set ownership
RUN chown openbas:openbas app.jar

USER openbas

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseContainerSupport", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
