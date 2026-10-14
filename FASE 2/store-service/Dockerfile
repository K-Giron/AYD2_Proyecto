FROM eclipse-temurin:25.0.4_7-jdk-alpine-3.23 AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw --batch-mode dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode clean package -DskipTests

FROM eclipse-temurin:25.0.4_7-jre-alpine-3.23 AS runtime
RUN addgroup -S store && adduser -S -G store -h /app store
WORKDIR /app
COPY --from=build --chown=store:store /workspace/target/store-service-0.0.1-SNAPSHOT.jar app.jar
USER store
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
