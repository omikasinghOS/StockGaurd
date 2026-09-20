FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/ ./backend/
RUN mvn -f backend/pom.xml package -DskipTests -q
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S stockguard && adduser -S stockguard -G stockguard
WORKDIR /app
COPY --from=build /app/backend/order-service/target/order-service-1.0.0.jar app.jar
USER stockguard
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-jar", "app.jar"]
