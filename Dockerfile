# Multi-stage build for Java 21 application
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/log-monitoring-1.0.jar /app/log-monitoring.jar

# Create a directory for log files
RUN mkdir -p /app/logs

ENTRYPOINT ["java", "-jar", "/app/log-monitoring.jar"]

# Default command (can be overridden)
CMD ["/app/logs/sample.log"]
