# =======================================
# Stage 1: Build Frontend (React + Vite)
# =======================================
FROM node:20-alpine AS frontend-build

WORKDIR /app/frontend

# Copiar package.json y lock primero (cache de capas)
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

# Copiar código fuente del frontend y compilar
COPY frontend/ .
RUN npm run build

# =======================================
# Stage 2: Build Backend (Spring Boot)
# =======================================
FROM eclipse-temurin:21-jdk-alpine AS backend-build

WORKDIR /app

# Copiar Maven wrapper y pom.xml primero (cache de capas)
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline --batch-mode

# Copiar código fuente del backend
COPY backend/src/ src/

# Copiar el build del frontend a los recursos estáticos
COPY --from=frontend-build /app/frontend/dist src/main/resources/static/

# Compilar el JAR (sin activar perfil frontend, ya copiamos los estáticos)
RUN ./mvnw package -DskipTests --batch-mode --no-transfer-progress

# =======================================
# Stage 3: Runtime
# =======================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear usuario no-root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=backend-build /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
