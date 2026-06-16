# Step 1: Build inside an official Eclipse Temurin Maven container
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run inside a lightweight JRE runtime container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/document-summarizer-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
