FROM gradle:8-jdk17-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar check --parallel --no-daemon

FROM openjdk:17-alpine
EXPOSE 8080:8080
RUN mkdir /app
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar /ktor-app.jar
ENTRYPOINT ["java","-jar","/ktor-app.jar","-config=application.conf","-config=application-prod.conf"]
