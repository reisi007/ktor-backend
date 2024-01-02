FROM gradle:8-jdk17-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

FROM openjdk:17-alpine
EXPOSE 8080:8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/ktor-app.jar
ENTRYPOINT ["java","-jar","/app/ktor-app.jar","-config=application.conf","-config=application-prod.conf"]
