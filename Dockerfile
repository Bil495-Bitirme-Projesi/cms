# ============================================================
# CMS — Multi-Stage Dockerfile
# ============================================================
# Stage 1: Build the application JAR using Maven.
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy POM first for better layer caching:
# dependencies are re-downloaded only when pom.xml changes.
COPY pom.xml ./
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -q

# ============================================================
# Stage 2: Minimal runtime image (JRE only, no source/build tools)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Create config directory so Docker can bind-mount files into it
# (without this, Docker auto-creates the mount point as a directory,
#  causing "not a directory" errors when mounting a single file)
RUN mkdir -p /app/config

EXPOSE 8050
ENTRYPOINT ["java", "-jar", "app.jar"]


