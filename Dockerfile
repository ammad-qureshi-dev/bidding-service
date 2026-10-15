FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Credentials for GitHub Packages (Render passes service env vars as build args).
# Build args only exist in this stage; they never reach the runtime image.
ARG BIDDER_USERNAME
ARG BIDDER_GITHUB_TOKEN

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY bidding-model bidding-model
COPY src src

RUN chmod +x gradlew \
 && ./gradlew bootJar --no-daemon -x test \
 && cp "$(ls build/libs/*.jar | grep -v plain)" app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /workspace/app.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:${PORT:-8083}/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
