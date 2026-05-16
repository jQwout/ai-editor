FROM gradle:8.13-jdk17@sha256:ec973fb6579488ecba8cd24f4728cd819eda45d6b26f7f44a81682e1c034898f AS build

WORKDIR /app

# Standalone backend Gradle project files.
COPY settings.gradle.kts build.gradle.kts /app/
COPY src /app/src

# Build runnable distribution with launch scripts in build/install/backend.
RUN gradle --no-daemon clean installDist

FROM eclipse-temurin:17-jre@sha256:0d79988c68791ce864fe39d149ab1dc84f680539dca77ee7f6f3b041ad7f2f43

WORKDIR /opt/backend
COPY --from=build /app/build/install/backend /opt/backend

# Run app as non-root user inside container.
RUN useradd --system --uid 10001 backend \
    && chown -R backend:backend /opt/backend
USER backend

EXPOSE 8080
ENV PORT=8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD ["/bin/sh", "-c", "curl --fail --silent http://127.0.0.1:${PORT:-8080}/health > /dev/null || exit 1"]

ENTRYPOINT ["/opt/backend/bin/backend"]

