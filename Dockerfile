# ================================
# Stage 1: Build the JAR
# ================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml first (for dependency caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ================================
# Stage 2: Run the JAR
# ================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/target/backend-1.0.0.jar app.jar

# Expose port (Render injects $PORT env variable)
EXPOSE 8080

# JVM flags:
#   -XX:+UseContainerSupport      — respect Docker memory limits (not host RAM)
#   -XX:MaxRAMPercentage=75.0     — use up to 75% of container RAM for heap
#   -Djava.security.egd=...       — faster startup (avoids blocking on /dev/random)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
