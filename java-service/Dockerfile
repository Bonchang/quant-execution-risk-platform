FROM node:22-alpine AS frontend-builder
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend ./
RUN npm run build

FROM gradle:8.14.3-jdk17 AS builder
WORKDIR /workspace
COPY java-service ./java-service
COPY --from=frontend-builder /workspace/frontend/dist ./frontend/dist
WORKDIR /workspace/java-service
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/java-service/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
