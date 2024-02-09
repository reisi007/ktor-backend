FROM gradle:8.6.0-jdk21-graal AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar check --parallel --no-daemon

FROM ghcr.io/graalvm/jdk-community:21
EXPOSE 8080:8080
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar /ktor-app.jar
ENTRYPOINT ["java","-jar","/ktor-app.jar","-config=application.conf","-config=application-prod.conf"]
