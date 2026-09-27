FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/notification-gateway-service.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
