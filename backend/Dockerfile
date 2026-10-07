# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build

ARG GEMS_REGISTRY_USER
WORKDIR /workspace
COPY . .

RUN --mount=type=secret,id=gems_registry_token,required=true \
    set -eu; \
    printf '%s\n' \
      '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">' \
      '  <servers><server><id>github</id>' \
      "    <username>${GEMS_REGISTRY_USER}</username>" \
      "    <password>$(cat /run/secrets/gems_registry_token)</password>" \
      '  </server></servers>' \
      '</settings>' > /tmp/gems-settings.xml; \
    mvn -B -s /tmp/gems-settings.xml -DskipTests clean package; \
    rm /tmp/gems-settings.xml

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/app-deploy/target/app-deploy-*.jar /app/app.jar
RUN addgroup -S app && adduser -S app -G app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
