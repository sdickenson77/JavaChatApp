# Base image using OpenJDK 21 slim version for minimal footprint
FROM openjdk:21-slim

# Set the working directory for all subsequent operations
WORKDIR /app

# Copy build configuration and source code
# Using --link to optimize layer caching and reduce image size
COPY --link pom.xml .
COPY --link src ./src

# Install Maven for building the application
# Combined RUN command to reduce image layers
RUN apt-get update && apt-get install -y maven

# Build the application using Maven
RUN mvn clean package

# Configure the application port
# Uses SERVER_PORT environment variable with default value 5000
EXPOSE ${SERVER_PORT:-5000}

# Start the Java application
# Uses the jar file generated during the Maven build
CMD ["java", "-jar", "target/JavaChatApp-1.0-SNAPSHOT.jar"]

