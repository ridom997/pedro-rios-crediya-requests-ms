FROM eclipse-temurin:21-jdk-alpine

COPY applications/app-service/build/libs/*.jar app.jar

# Variables de entorno típicas
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC"
ENV TZ=UTC

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]