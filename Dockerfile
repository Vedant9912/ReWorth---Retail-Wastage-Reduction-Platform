# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom.xml from nearsave folder and download dependencies
COPY nearsave/pom.xml .
RUN mvn dependency:go-offline -B

# Copy src from nearsave folder and build the package
COPY nearsave/src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built jar from builder stage
COPY --from=builder /app/target/nearsave-*.jar app.jar

# Expose server port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
