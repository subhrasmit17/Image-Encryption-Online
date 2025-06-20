# Stage 1: Build the application (compile)
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests


# Stage 2: Create the final image (run)
FROM openjdk:17.0.1-jdk-slim
COPY --from=build /target/image-encryption-0.0.1-SNAPSHOT.jar image-encryption.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","image-encryption.jar"]
