FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY services/gateway/pom.xml services/gateway/pom.xml
COPY services/diagnosis-service/pom.xml services/diagnosis-service/pom.xml
COPY services/repairer-service/pom.xml services/repairer-service/pom.xml
COPY services/media-service/pom.xml services/media-service/pom.xml
COPY services/auth-service/pom.xml services/auth-service/pom.xml
COPY e2e-tests/pom.xml e2e-tests/pom.xml
RUN mvn -N -q install
COPY common/src common/src
COPY services/auth-service/src services/auth-service/src
RUN mvn -q -pl services/auth-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/auth-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
