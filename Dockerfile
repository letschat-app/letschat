# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-focal AS build
WORKDIR /app
# ----------------------------------------------------
# NEW STEP: Install Maven
# We use 'apt-get' because the base image is Ubuntu/Focal-based
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*
# ----------------------------------------------------
# Copy the pom.xml first to download dependencies and cache the layer
COPY pom.xml .
RUN mvn dependency:go-offline
# Copy the source code
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Create the final, smaller runtime image (JRE Alpine)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Set the port that the application will listen on (Render uses 10000 by default)
ENV PORT 10000
EXPOSE $PORT
# Copy the built jar from the build stage, renaming it to app.jar
COPY --from=build /app/target/*.jar app.jar
# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
