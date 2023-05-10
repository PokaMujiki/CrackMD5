FROM gradle AS build
WORKDIR /worker
COPY . .
RUN gradle build --no-daemon

FROM openjdk
WORKDIR /worker-app
COPY --from=build /worker/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]