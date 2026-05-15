FROM gradle:8.13-jdk17 AS build

WORKDIR /app

# Copy only backend standalone build first (better Docker cache)
COPY settings.gradle.kts build.gradle.kts /app/
COPY src /app/src

# Build a runnable distribution
RUN gradle --no-daemon clean installDist

FROM eclipse-temurin:17-jre

WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/build/install/backend /app

EXPOSE 8080

# Ktor uses PORT env
ENV PORT=8080

CMD ["/app/bin/backend"]

