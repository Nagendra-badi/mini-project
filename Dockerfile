# Build stage: Compile code using Maven & Temurin JDK 17
FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies (speeds up consecutive builds on Render)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage: Run the built JAR file using a lightweight JRE
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/ddms-0.0.1-SNAPSHOT.jar app.jar

# Define port 8080 (Render maps this port automatically)
EXPOSE 8080

# Run the spring boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
