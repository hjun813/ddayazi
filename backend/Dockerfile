FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY backend/build.gradle backend/build.gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew :backend:dependencies --no-daemon
COPY backend/src backend/src
RUN ./gradlew :backend:bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S certpath && adduser -S certpath -G certpath
COPY --from=build /workspace/backend/build/libs/backend-0.1.0.jar app.jar
USER certpath
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
