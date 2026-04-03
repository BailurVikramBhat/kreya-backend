FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY . .

RUN ./gradlew bootJar --no-daemon

EXPOSE 8080

CMD ["java", "-Duser.timezone=Asia/Kolkata", "-jar", "build/libs/kreya-backend-0.0.1-SNAPSHOT.jar"]
