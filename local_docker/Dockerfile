
# ===== Build stage (Gradle) =====
FROM gradle:7.2.0 AS build
WORKDIR /workspace

# Copy gradle files & resolve deps
COPY build.gradle* settings.gradle* gradle.properties* /workspace/
COPY ../gradle /workspace/gradle
RUN gradle --no-daemon build -x test || true

# Copy sources and build
COPY .. /workspace
RUN gradle --no-daemon clean bootJar -x test

# Find the fat jar (Spring Boot) and rename
RUN JAR=$(find /workspace/build/libs -maxdepth 1 -type f -name "*.jar" | head -n1) &&     cp "$JAR" /workspace/app.jar

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre
ENV APP_HOME=/opt/app     JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"     SERVER_PORT=8080
WORKDIR ${APP_HOME}

RUN useradd -u 1001 spring && mkdir -p ${APP_HOME} && chown -R spring:spring ${APP_HOME}
USER spring

COPY --from=build /workspace/app.jar app.jar
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3   CMD wget -qO- http://127.0.0.1:${SERVER_PORT}/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh","-c","java ${JAVA_OPTS} -Dserver.port=${SERVER_PORT} -jar app.jar"]
