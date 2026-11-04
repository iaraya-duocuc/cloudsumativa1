FROM eclipse-temurin:21-jdk AS buildstage

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src /app/src

RUN sh ./mvnw -B clean package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=buildstage /app/target/cloudsumativa1-0.0.1-SNAPSHOT.jar /app/cloudsumativa1.jar
EXPOSE 8080

ENTRYPOINT [ "java", "-jar", "/app/cloudsumativa1.jar" ]
