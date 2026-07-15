FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY cursos-service/pom.xml cursos-service/pom.xml
COPY inscripciones-service/pom.xml inscripciones-service/pom.xml
COPY bff-service/pom.xml bff-service/pom.xml
COPY bff-service/src bff-service/src
RUN mvn -pl bff-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/bff-service/target/bff-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

