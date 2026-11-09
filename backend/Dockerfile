# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
# Tuned for Render's free instance (512 MB, tiny CPU): serial GC, small thread stacks, quick JIT, and exit loudly on out-of-memory
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=65", "-Xss512k", "-XX:TieredStopAtLevel=1", "-XX:+ExitOnOutOfMemoryError", "-Dspring.output.ansi.enabled=never", "-jar", "app.jar"]
