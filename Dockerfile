# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Install Maven
RUN apk add --no-cache maven

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
