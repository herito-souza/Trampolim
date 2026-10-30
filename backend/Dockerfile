FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg curl ca-certificates && rm -rf /var/lib/apt/lists/* && useradd --uid 10001 --create-home trampolim
WORKDIR /app
RUN mkdir -p /app/storage && chown -R trampolim:trampolim /app
COPY --from=build /build/target/backend-0.0.1-SNAPSHOT.jar /app/backend.jar
USER 10001:10001
ENV CAPACITA_MEDIA_ROOT=/app/storage/capacita/videos
EXPOSE 8080 9001
HEALTHCHECK --interval=20s --timeout=4s --start-period=60s CMD curl --fail --silent http://127.0.0.1:9001/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=40", "-jar", "/app/backend.jar"]
