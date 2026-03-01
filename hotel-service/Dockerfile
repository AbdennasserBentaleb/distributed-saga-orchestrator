# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY . .
RUN mvn -B -DskipTests clean package -pl hotel-service -am

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup -g 65532 && adduser -S appuser -G appgroup -u 65532
VOLUME /tmp
COPY --from=builder /app/hotel-service/target/*.jar /app.jar
RUN chown appuser:appgroup /app.jar
USER 65532
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app.jar"]
