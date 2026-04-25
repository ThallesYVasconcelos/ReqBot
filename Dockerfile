FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Debian-based: ONNX Runtime (AllMiniLmL6V2) precisa de libstdc++.so.6
# NÃO use a imagem Alpine (musl) — falha com libonnxruntime.so no Render/Cloud
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends libstdc++6 \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:TieredStopAtLevel=1", \
  "-Xss256k", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseSerialGC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
