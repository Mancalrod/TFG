# =======================================
# Stage 1: Build
# =======================================
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiar Maven wrapper y pom.xml primero (para aprovechar cache de capas)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline --batch-mode

# Copiar código fuente y compilar
COPY src/ src/
RUN ./mvnw package -DskipTests --batch-mode --no-transfer-progress

# =======================================
# Stage 2: Runtime
# =======================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
