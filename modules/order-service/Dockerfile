# syntax=docker/dockerfile:1.7
#
# Build context: repository root (so we can see settings.gradle, the wrapper,
# the common module, and this service's sources in one place).
#   docker build -f modules/order-service/Dockerfile -t order-service:dev .

# ---------- Stage 1: build the boot jar ----------
FROM eclipse-temurin:23-jdk AS builder
WORKDIR /workspace

# Copy the wrapper + root build files first. These change rarely, so this layer
# stays cached across most edits.
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY settings.gradle build.gradle ./

# Per-module build files only (no sources yet). Settings.gradle references all
# three modules, so Gradle needs all three build.gradle files to even initialise.
COPY modules/common/build.gradle           modules/common/build.gradle
COPY modules/order-service/build.gradle    modules/order-service/build.gradle
COPY modules/stock-service/build.gradle    modules/stock-service/build.gradle

# Warm the Gradle dependency cache. BuildKit cache mount keeps ~/.gradle across
# builds without baking it into the image. The `|| true` keeps the build moving
# if a transient resolve fails — the real build below will fail loudly anyway.
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && \
    ./gradlew :order-service:dependencies --no-daemon -q || true

# Now copy sources. Only the modules we actually need for this image.
COPY modules/common         modules/common
COPY modules/order-service  modules/order-service

# Build the executable boot jar. Skip tests — CI runs them separately, and
# baking them into the image build slows the inner loop.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :order-service:bootJar --no-daemon -x test

# ---------- Stage 2: explode the layered jar ----------
# Spring Boot 4 ships a `tools` jarmode (replaces the older `layertools`).
# `extract --layers --launcher` writes one directory per layer, plus a
# separate directory for the loader classes. Copying them in the right order
# in the runtime stage gives Docker per-layer caching: dependency churn is
# rare, application-class churn is constant.
FROM eclipse-temurin:23-jdk AS extractor
WORKDIR /extract
COPY --from=builder /workspace/modules/order-service/build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination layers

# ---------- Stage 3: runtime ----------
FROM eclipse-temurin:23-jre
WORKDIR /app

# curl is needed for the actuator healthcheck (compose + K8s HTTP probes).
# Non-root user: K8s PodSecurity "restricted" profile rejects containers that
# run as root, so we set this up now rather than fighting it later in Helm.
# Fixed numeric uid/gid keeps things predictable when the image is pulled into
# clusters that enforce runAsNonRoot + runAsUser.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system --gid 10001 spring \
 && useradd  --system --uid 10001 --gid spring --home-dir /app --shell /sbin/nologin spring \
 && chown -R spring:spring /app
USER spring:spring

# Copy layers least- to most-frequently-changing. Dependencies almost never
# change between builds; application classes change on every commit.
COPY --from=extractor --chown=spring:spring /extract/layers/dependencies/          ./
COPY --from=extractor --chown=spring:spring /extract/layers/spring-boot-loader/    ./
COPY --from=extractor --chown=spring:spring /extract/layers/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring:spring /extract/layers/application/           ./

EXPOSE 8080

# JVM container ergonomics:
#   UseContainerSupport     — read cgroup memory/cpu limits (on by default in
#                             modern JDKs; left explicit for documentation).
#   MaxRAMPercentage=75     — heap = 75% of the cgroup memory limit. Prefer
#                             this over a hardcoded -Xmx so K8s `resources.limits`
#                             is the single source of truth.
#   ExitOnOutOfMemoryError  — die fast on OOM; let the orchestrator restart us
#                             instead of limping along with a corrupted heap.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# JarLauncher reads classpath.idx / layers.idx the bootJar wrote into the
# application layer and assembles the classpath at startup.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
