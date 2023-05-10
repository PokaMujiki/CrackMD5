FROM gradle AS build
WORKDIR /manager
COPY . .
RUN gradle build --no-daemon

FROM openjdk
WORKDIR /manager-app
COPY --from=build /manager/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]