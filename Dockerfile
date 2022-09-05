FROM openjdk:17-alpine

WORKDIR /app

EXPOSE 8080

ADD /target/CloudFileStorage-1.0.0-SNAPSHOT.jar ./app.jar

ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "app.jar"]