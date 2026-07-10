# Lightweight JRE run image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the pre-compiled JAR file directly from backend/target/
COPY backend/target/ddms-0.0.1-SNAPSHOT.jar app.jar

# Define port 8080
EXPOSE 8080

# Run the spring boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
