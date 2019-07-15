FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG target/customer.jar
COPY target/customer.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]