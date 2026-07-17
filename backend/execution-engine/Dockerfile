# Use an official Java runtime as a parent image
FROM eclipse-temurin:21-jdk

# Install the Docker CLI inside container
RUN apt-get update && \
    apt-get install -y docker.io && \
    rm -rf /var/lib/apt/lists/*

# Set the working directory inside the container
WORKDIR /app

# Copy the compiled Java executable (.jar) into the container
COPY target/*.jar app.jar

# Expose port 8080 so the React frontend can reach the API
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]