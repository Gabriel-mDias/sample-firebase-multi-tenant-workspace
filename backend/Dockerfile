# syntax=docker/dockerfile:1
# Build the reactor with JDK 21 and external Maven registry configuration first.
# Only the executable artifact enters the Docker context; no settings or credentials.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY app-deploy/target/app-deploy-*.jar /app/app.jar
RUN addgroup -S app && adduser -S app -G app
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1
# The local demo mounts a generated external database secret, never a build argument.
# Real-provider use can supply DB_PASSWORD externally without this optional mount.
ENTRYPOINT ["sh", "-c", "if [ -f /run/secrets/gems_demo_password ]; then export DB_PASSWORD=\"$(cat /run/secrets/gems_demo_password)\"; fi; exec java -jar /app/app.jar"]
