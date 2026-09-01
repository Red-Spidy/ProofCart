# Multi-stage build — Render doesn't auto-detect Java/Maven the way Railway's Nixpacks does,
# so this is the reliable path there (and portable to any other Docker-capable host).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Cache dependencies in their own layer before copying source, so a source-only change
# doesn't force a full dependency re-download on every image build.
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Render (and most PaaS hosts) inject PORT at runtime; application.yml already reads
# ${PORT:8080}, so no extra config is needed here.
CMD ["java", "-jar", "app.jar"]
