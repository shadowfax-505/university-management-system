# ---------- Stage 1: build the jar with Maven + JDK ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only pom.xml first and download dependencies. Docker caches this layer,
# so later builds skip the download unless pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy the source code and build the executable jar (tests run separately)
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: small runtime image with only the JRE + jar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Do not run the application as root inside the container
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /app/target/university-management-system-1.0.0.jar app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
