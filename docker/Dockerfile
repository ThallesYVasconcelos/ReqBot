FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Debian-based: ONNX Runtime precisa de libstdc++.so.6
FROM eclipse-temurin:17-jre

WORKDIR /app

# ONNX Runtime (AllMiniLmL6V2EmbeddingModel) requer libstdc++.so.6
RUN apt-get update && apt-get install -y --no-install-recommends libstdc++6 \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Flags otimizadas para cold start rápido em Cloud Run (512Mi RAM):
#   -XX:TieredStopAtLevel=1  — desliga compilação C2 no startup (JIT mais leve)
#   -Xss256k                 — stack menor por thread (economiza memória)
#   -XX:MaxRAMPercentage=75  — deixa 25% livre para o SO e ONNX
#   -XX:+UseSerialGC         — GC leve para instâncias com 1 vCPU / pouca RAM
ENTRYPOINT ["java", \
  "-XX:TieredStopAtLevel=1", \
  "-Xss256k", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseSerialGC", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
