# Stage 1: Build Vue frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

# Stage 2: Build Spring Boot JAR (with frontend static files included)
FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /app
COPY backend/ .
COPY --from=frontend-build /app/dist ./src/main/resources/static
RUN chmod +x gradlew && ./gradlew bootJar -x test

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
