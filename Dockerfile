FROM openjdk:22-jdk
COPY target/zeebe-lambda-worker.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]