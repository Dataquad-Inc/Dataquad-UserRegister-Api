
FROM eclipse-temurin:17-jdk AS builder

# Install Maven
RUN apt-get update && apt-get install -y maven && apt-get install -y curl

# Set the working directory in the container
WORKDIR /app

# Copy the Maven project file
COPY pom.xml .

# Download dependencies (this step is caching the dependencies layer)
RUN mvn dependency:go-offline -B

# Copy the rest of the application source code
COPY src ./src

# Build the application (the JAR file will be generated in the target folder)
RUN mvn clean package -DskipTests

# Use the official openjdk image for running the application
FROM eclipse-temurin:17-jdk

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file from the build stage into the container
COPY --from=builder /app/target/DataquadUserRegisterApi-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the app runs on
EXPOSE 8083

# Run the JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]







