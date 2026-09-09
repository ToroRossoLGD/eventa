FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r eventa && useradd -r -g eventa eventa && chown eventa:eventa /app
COPY --from=build --chown=eventa:eventa /app/target/eventa-1.0.0.jar app.jar
USER eventa
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
