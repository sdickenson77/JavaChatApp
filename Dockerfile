
# Use OpenJDK 21 as the base image
FROM openjdk:21-slim

# Set working directory in container
WORKDIR /app

# Copy the pom.xml and source code, following symlinks
COPY --link pom.xml .
COPY --link src ./src

# Install Maven
RUN apt-get update && apt-get install -y maven

# Build the application
RUN mvn clean package

# Expose the port that the server will run on
EXPOSE ${SERVER_PORT:-5000}

# Run the application using the correct main class from pom.xml
CMD ["java", "-jar", "target/JavaChatApp-1.0-SNAPSHOT.jar"]
