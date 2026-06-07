# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
