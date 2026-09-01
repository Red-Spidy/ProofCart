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
#
# Free-tier hosts give a fraction of a CPU, and cold JVM+Hibernate startup is CPU-bound —
# these two flags matter far more there than on a normal machine:
#   -XX:TieredStopAtLevel=1  use only the fast C1 JIT tier during startup (skips the much more
#                            expensive C2 compilation the JVM would otherwise spend cycles on
#                            before the app is even up — throughput cost is irrelevant for a demo)
#   -Djava.security.egd=...  point SecureRandom at /dev/urandom instead of the blocking /dev/random
#                            entropy pool — Spring Security, JWT verification and Postgres/HTTPS
#                            TLS handshakes all seed SecureRandom at startup and can stall for a
#                            long time waiting on entropy in a container
CMD ["java", "-XX:TieredStopAtLevel=1", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
