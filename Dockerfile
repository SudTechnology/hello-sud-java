FROM maven:3.9.9-eclipse-temurin-8 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B verify
FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /src/target/hello-sud-java-1.0.0.jar /app/app.jar
ENV HOST=0.0.0.0 PORT=8000
EXPOSE 8000
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
