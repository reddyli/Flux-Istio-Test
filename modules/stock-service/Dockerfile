# syntax=docker/dockerfile:1.7
#
# Build context: repository root.
#   docker build -f modules/stock-service/Dockerfile -t stock-service:dev .

# ---------- Stage 1: build the boot jar ----------
FROM eclipse-temurin:23-jdk AS builder
WORKDIR /workspace

COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY settings.gradle build.gradle ./

COPY modules/common/build.gradle           modules/common/build.gradle
COPY modules/order-service/build.gradle    modules/order-service/build.gradle
COPY modules/stock-service/build.gradle    modules/stock-service/build.gradle

RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && \
    ./gradlew :stock-service:dependencies --no-daemon -q || true

COPY modules/common         modules/common
COPY modules/stock-service  modules/stock-service

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :stock-service:bootJar --no-daemon -x test

# ---------- Stage 2: explode the layered jar ----------
FROM eclipse-temurin:23-jdk AS extractor
WORKDIR /extract
COPY --from=builder /workspace/modules/stock-service/build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers

# ---------- Stage 3: runtime ----------
FROM eclipse-temurin:23-jre
WORKDIR /app

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 spring \
 && useradd  --system --uid 10001 --gid spring --home-dir /app --shell /sbin/nologin spring \
 && chown -R spring:spring /app
USER spring:spring

COPY --from=extractor --chown=spring:spring /extract/layers/dependencies/          ./
COPY --from=extractor --chown=spring:spring /extract/layers/spring-boot-loader/    ./
COPY --from=extractor --chown=spring:spring /extract/layers/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring:spring /extract/layers/application/           ./

EXPOSE 8081

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
