# Step 1: Build inside a Java 17 container
FROM maven:3.8.8-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run inside a lightweight Java 17 runtime container
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/document-summarizer-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
