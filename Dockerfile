FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY services ./services
ARG MODULE
RUN mvn -pl services/${MODULE} -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG MODULE
COPY --from=build /workspace/services/${MODULE}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

