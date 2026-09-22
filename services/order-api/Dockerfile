# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY services/order-api/pom.xml ./services/order-api/pom.xml
RUN mvn -q -pl services/order-api -am dependency:go-offline
COPY services/order-api/src ./services/order-api/src
RUN mvn -q -pl services/order-api -am package -DskipTests

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
COPY --from=build /workspace/services/order-api/target/order-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
