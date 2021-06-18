FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY target/tkm-ms-acquirer-manager-*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]