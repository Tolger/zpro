FROM sbtscala/scala-sbt:eclipse-temurin-17.0.5_8_1.8.2_3.2.1 as builder
COPY . .
RUN sbt assembly

FROM eclipse-temurin:17.0.5_8-jre-alpine
COPY --from=builder "/root/target/scala-3.2.1/backend-assembly-0.1.0.jar" "/app.jar"

EXPOSE 8080
CMD [ "-jar", "/app.jar" ]
ENTRYPOINT [ "java" ]
