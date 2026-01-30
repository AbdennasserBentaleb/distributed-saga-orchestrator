FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appgroup -g 65532 && adduser -S appuser -G appgroup -u 65532
VOLUME /tmp
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
RUN chown appuser:appgroup app.jar
USER 65532
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
