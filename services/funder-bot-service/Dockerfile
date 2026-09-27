FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/funder-bot-service.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
