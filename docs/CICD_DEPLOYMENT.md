# Documentación CI/CD y Despliegue — TFG: Sistema de Gestión de Entregables

## Índice

1. [Visión General de la Arquitectura](#1-visión-general-de-la-arquitectura)
2. [Stack Tecnológico](#2-stack-tecnológico)
3. [Pipelines de Integración Continua (CI)](#3-pipelines-de-integración-continua-ci)
4. [Pipeline de Despliegue Continuo (CD)](#4-pipeline-de-despliegue-continuo-cd)
5. [Análisis de Calidad de Código — SonarCloud](#5-análisis-de-calidad-de-código--sonarcloud)
6. [Auditoría de Seguridad](#6-auditoría-de-seguridad)
7. [Gestión Automática de Dependencias — Dependabot](#7-gestión-automática-de-dependencias--dependabot)
8. [Keep-Alive](#8-keep-alive)
9. [Containerización con Docker](#9-containerización-con-docker)
10. [Despliegue en Render](#10-despliegue-en-render)
11. [Base de Datos en Producción — PostgreSQL Gestionado](#11-base-de-datos-en-producción--postgresql-gestionado)
12. [Problemas Encontrados y Soluciones](#12-problemas-encontrados-y-soluciones)
13. [Diagrama Completo del Pipeline](#13-diagrama-completo-del-pipeline)
14. [Secrets y Variables de Entorno](#14-secrets-y-variables-de-entorno)

---

## 1. Visión General de la Arquitectura

El proyecto sigue una arquitectura monorepo con dos componentes principales desplegados de forma independiente:

```
TFG/
├── backend/          → Spring Boot (Java 21) — API REST
├── frontend/         → React + Vite (TypeScript) — SPA
├── .github/
│   ├── workflows/    → 7 pipelines de GitHub Actions
│   └── dependabot.yml
├── docker-compose.yml
├── render.yaml       → Blueprint de despliegue en Render
└── sonar-project.properties
```

La filosofía es **automatizar todo el ciclo de vida del software**: desde la validación del código con tests y linters, pasando por el análisis de calidad y seguridad, hasta el despliegue automático en producción y la monitorización de disponibilidad.

---

## 2. Stack Tecnológico

### ¿Por qué estas herramientas?

| Herramienta | Propósito | Justificación |
|---|---|---|
| **GitHub Actions** | CI/CD | Integración nativa con GitHub, gratuito para repositorios públicos, configuración declarativa en YAML |
| **Docker** | Containerización | Entornos reproducibles, independencia del sistema operativo, facilita el despliegue |
| **Render** | PaaS (despliegue) | Tier gratuito, soporte nativo para Docker y sitios estáticos, auto-deploy desde GitHub, configuración mediante Blueprint |
| **Neon** | Base de datos PostgreSQL | Tier gratuito permanente, PostgreSQL serverless, escalado automático, compatible con Hibernate |
| **SonarCloud** | Análisis de calidad | Detección de bugs, code smells, vulnerabilidades y cobertura de código. Gratuito para proyectos open source |
| **GHCR** | Registro de imágenes Docker | Integrado en GitHub, autenticación con `GITHUB_TOKEN`, sin configuración adicional |
| **UptimeRobot** | Monitorización y keep-alive | Pings cada 5 minutos al backend para evitar cold starts del tier gratuito de Render |

---

## 3. Pipelines de Integración Continua (CI)

### 3.1 Backend CI (`backend-ci.yml`)

**Propósito:** Validar que el backend compila correctamente y supera todos los tests unitarios e integración.

**Disparadores:**
- Push a la rama `main` cuando hay cambios en `backend/`
- Pull Requests hacia `main` cuando hay cambios en `backend/`

**Pipeline:**

```
┌─────────────────────────────────────────────────┐
│                Backend CI                        │
├─────────────────────────────────────────────────┤
│ 1. Checkout del código                          │
│ 2. Configurar Java 21 (Temurin) con caché Maven│
│ 3. Ejecutar ./mvnw verify                       │
│    ├── Compilación                              │
│    ├── Tests unitarios                          │
│    └── Empaquetado (JAR)                        │
└─────────────────────────────────────────────────┘
```

**Detalles técnicos:**
- **Runner:** `ubuntu-latest`
- **JDK:** Eclipse Temurin 21
- **Comando principal:** `./mvnw verify --batch-mode` — ejecuta todas las fases de Maven hasta `verify` (compile → test → package → verify)
- **Caché:** Se cachea el repositorio local de Maven usando `cache-dependency-path: backend/pom.xml` para acelerar builds posteriores
- **Working directory:** `backend/`

---

### 3.2 Frontend CI (`frontend-ci.yml`)

**Propósito:** Verificar que el frontend pasa el linter (ESLint) y compila correctamente el bundle de producción.

**Disparadores:**
- Push a la rama `main` cuando hay cambios en `frontend/`
- Pull Requests hacia `main` cuando hay cambios en `frontend/`

**Pipeline:**

```
┌─────────────────────────────────────────────────┐
│               Frontend CI                        │
├─────────────────────────────────────────────────┤
│ 1. Checkout del código                          │
│ 2. Configurar Node 20 con caché npm             │
│ 3. npm ci (instalación limpia de dependencias)  │
│ 4. npm run lint (ESLint)                        │
│ 5. npm run build (compilación de producción)    │
└─────────────────────────────────────────────────┘
```

**Detalles técnicos:**
- **Runner:** `ubuntu-latest`
- **Node.js:** versión 20 LTS
- **`npm ci` vs `npm install`:** Se usa `npm ci` porque instala exactamente las versiones del `package-lock.json`, garantizando builds reproducibles y es más rápido en CI
- **ESLint:** Configurado con `eslint.config.js` (formato flat config de ESLint 9) para React + TypeScript + Vite
- **Caché:** Se cachea `node_modules` usando `cache-dependency-path: frontend/package-lock.json`

---

### 3.3 Full Stack CI (`full-stack-ci.yml`)

**Propósito:** Ejecutar las validaciones de backend y frontend en paralelo en cada push a `main`, independientemente de qué archivos cambien. Garantiza que un cambio en un componente no rompa el otro.

**Disparadores:**
- Push a la rama `main` (sin filtro de paths)
- Pull Requests hacia `main` (sin filtro de paths)

**Pipeline:**

```
┌─────────────────────────────────────────────────────────────┐
│                     Full Stack CI                            │
├──────────────────────────┬──────────────────────────────────┤
│   Build & Test Backend   │   Lint & Build Frontend          │
│   (en paralelo)          │   (en paralelo)                  │
├──────────────────────────┼──────────────────────────────────┤
│ 1. Checkout              │ 1. Checkout                      │
│ 2. Setup Java 21         │ 2. Setup Node 20                 │
│ 3. mvnw verify           │ 3. npm ci                        │
│                          │ 4. npm run lint                   │
│                          │ 5. npm run build                  │
└──────────────────────────┴──────────────────────────────────┘
```

**¿Por qué existe si ya hay backend-ci y frontend-ci?**
- `backend-ci.yml` y `frontend-ci.yml` solo se disparan cuando hay cambios en sus respectivos directorios (filtro de paths)
- `full-stack-ci.yml` se ejecuta **siempre**, asegurando que ambos componentes siguen funcionando juntos tras cada commit

---

## 4. Pipeline de Despliegue Continuo (CD)

### 4.1 Build & Push Docker Images (`cd.yml`)

**Propósito:** Construir las imágenes Docker de backend y frontend y publicarlas en GitHub Container Registry (GHCR) para su distribución.

**Disparadores:**
- Push a la rama `main`

**Pipeline:**

```
┌─────────────────────────────────────────────────────────────────┐
│              CD - Build & Push Docker Images                      │
├─────────────────────────────────────────────────────────────────┤
│ 1. Checkout del código                                          │
│ 2. Login en GHCR con GITHUB_TOKEN                               │
│ 3. Extraer metadatos para imagen del backend                    │
│ 4. Build + Push imagen backend → ghcr.io/mancalrod/tfg/backend │
│    Tags: SHA del commit + latest                                │
│ 5. Extraer metadatos para imagen del frontend                   │
│ 6. Build + Push imagen frontend → ghcr.io/mancalrod/tfg/frontend│
│    Tags: SHA del commit + latest                                │
└─────────────────────────────────────────────────────────────────┘
```

**Detalles técnicos:**
- **Registry:** `ghcr.io` (GitHub Container Registry)
- **Autenticación:** `GITHUB_TOKEN` (automático, sin necesidad de secrets adicionales)
- **Permisos:** `contents: read` + `packages: write`
- **Tags de imagen:** Se generan dos tags por imagen:
  - `sha-<commit>` — para trazabilidad exacta de la versión
  - `latest` — para obtener siempre la última versión
- **Actions utilizadas:**
  - `docker/login-action@v3` — login en el registry
  - `docker/metadata-action@v5` — generación automática de tags y labels
  - `docker/build-push-action@v5` — build multi-plataforma y push

**Nota:** Este pipeline publica las imágenes en GHCR para su distribución. El despliegue en Render se hace de forma independiente (Render construye desde el código fuente, no consume estas imágenes).

---

## 5. Análisis de Calidad de Código — SonarCloud

### 5.1 SonarCloud Analysis (`sonarcloud.yml`)

**Propósito:** Analizar la calidad del código Java del backend: detección de bugs, code smells, vulnerabilidades, code coverage y duplicaciones.

**Disparadores:**
- Push a la rama `main`
- Pull Requests hacia `main`

**Pipeline:**

```
┌─────────────────────────────────────────────────────────────┐
│                  SonarCloud Analysis                         │
├─────────────────────────────────────────────────────────────┤
│ 1. Checkout con fetch-depth: 0 (historial completo)        │
│ 2. Configurar Java 21 con caché Maven                       │
│ 3. mvnw verify + sonar-maven-plugin:sonar                   │
│    ├── Compilación y tests                                  │
│    └── Envío de resultados a SonarCloud                     │
└─────────────────────────────────────────────────────────────┘
```

**Detalles técnicos:**
- **`fetch-depth: 0`:** Necesario para que SonarCloud pueda analizar el historial de cambios y calcular métricas de "New Code"
- **Plugin Maven vs CLI Scanner:** Se usa el plugin Maven (`org.sonarsource.scanner.maven:sonar-maven-plugin`) en lugar del CLI Scanner porque:
  1. Es el método recomendado por SonarSource para proyectos Java/Maven
  2. No requiere descargar binarios externos (evita fallos de CDN)
  3. Tiene acceso directo a los resultados de compilación y tests
- **Configuración del proyecto:**
  - `sonar.projectKey=Mancalrod_TFG`
  - `sonar.organization=mancalrod`
  - `sonar.host.url=https://sonarcloud.io`
- **Secrets requeridos:**
  - `SONAR_TOKEN` — token de autenticación de SonarCloud
  - `GITHUB_TOKEN` — para decorar Pull Requests con resultados

**Quality Gate:** SonarCloud evalúa automáticamente el código contra un conjunto de reglas (Quality Gate). Si el código nuevo no cumple los umbrales definidos (ej. Security Rating ≥ A), el check falla en GitHub.

**Configuración inicial (una sola vez):**
1. Registrarse en [sonarcloud.io](https://sonarcloud.io) con GitHub
2. Importar el repositorio TFG
3. Desactivar "Automatic Analysis" (Administration → Analysis Method)
4. Generar un SONAR_TOKEN y añadirlo como secret en GitHub

---

## 6. Auditoría de Seguridad

### 6.1 Security Audit (`security-audit.yml`)

**Propósito:** Detectar vulnerabilidades conocidas (CVEs) en las dependencias del proyecto, tanto en el backend (Java/Maven) como en el frontend (Node/npm).

**Disparadores:**
- Push a la rama `main`
- Pull Requests hacia `main`
- **Cada lunes a las 9:00 UTC** (programación semanal con cron)

**Pipeline:**

```
┌────────────────────────────────────────────────────────────────────┐
│                        Security Audit                              │
├──────────────────────────┬─────────────────────────────────────────┤
│   npm audit (Frontend)   │   OWASP Dependency Check (Backend)     │
│   (en paralelo)          │   (en paralelo)                        │
├──────────────────────────┼─────────────────────────────────────────┤
│ 1. Checkout              │ 1. Checkout                            │
│ 2. Setup Node 20         │ 2. Setup Java 21                       │
│ 3. npm ci                │ 3. OWASP dependency-check:check        │
│ 4. npm audit --high      │ 4. Upload reporte HTML (artefacto)     │
└──────────────────────────┴─────────────────────────────────────────┘
```

**Frontend — npm audit:**
- Ejecuta `npm audit --audit-level=high` para identificar dependencias con CVEs de severidad alta o crítica
- `continue-on-error: true` — no bloquea el pipeline si hay vulnerabilidades, solo las reporta

**Backend — OWASP Dependency Check:**
- Utiliza el plugin Maven `org.owasp:dependency-check-maven:check`
- Descarga la base de datos NVD (National Vulnerability Database) y compara contra las dependencias del proyecto
- **NVD API Key:** Se usa `-DnvdApiKey=${{ secrets.NVD_API_KEY }}` para acelerar la descarga (de ~60 min a ~2-3 min)
- Genera un **reporte HTML** que se sube como artefacto de GitHub Actions descargable durante 30 días
- `continue-on-error: true` — reporta pero no bloquea

**¿Por qué la ejecución semanal?**
Nuevas vulnerabilidades se descubren constantemente. La ejecución semanal (cron) garantiza que se detecten CVEs nuevos incluso si no hay commits recientes.

---

## 7. Gestión Automática de Dependencias — Dependabot

### 7.1 Configuración (`dependabot.yml`)

**Propósito:** Crear Pull Requests automáticos cuando hay versiones nuevas de las dependencias, manteniendo el proyecto actualizado y seguro.

**Ecosistemas monitorizados:**

| Ecosistema | Directorio | Frecuencia | Máx PRs |
|---|---|---|---|
| **Maven** (backend) | `/backend` | Semanal (lunes) | 5 |
| **npm** (frontend) | `/frontend` | Semanal (lunes) | 5 |
| **GitHub Actions** | `/` | Semanal (lunes) | 3 |

**Etiquetas automáticas:**
- Cada PR se etiqueta con `dependencies` + el componente (`backend`, `frontend`, o `ci`)
- Facilita el filtrado y revisión de las PRs

**Flujo de trabajo con Dependabot:**
1. Dependabot detecta una nueva versión de una dependencia
2. Crea una PR con la actualización
3. Los workflows de CI se ejecutan sobre la PR automáticamente
4. Si los checks pasan → se puede hacer merge con confianza
5. Si fallan → investigar el breaking change antes de mergear

**Interacción por comentarios:**
- `@dependabot merge` — mergear la PR
- `@dependabot close` — cerrar sin mergear
- `@dependabot ignore this major version` — ignorar actualizaciones major

---

## 8. Keep-Alive

### 8.1 Keep-Alive (`keep-alive.yml`)

**Propósito:** Evitar que el backend se duerma en el tier gratuito de Render, que suspende los servicios tras 15 minutos de inactividad.

**Disparadores:**
- **Cada 14 minutos** (cron: `*/14 * * * *`)
- Ejecución manual (`workflow_dispatch`)

**Pipeline:**

```
┌─────────────────────────────────────────────┐
│              Keep Alive                      │
├─────────────────────────────────────────────┤
│ 1. curl al endpoint /api/health del backend │
│    → Mantiene el servicio despierto         │
└─────────────────────────────────────────────┘
```

**Nota:** Este workflow se complementa con **UptimeRobot**, un servicio externo gratuito que hace pings cada 5 minutos. Ambos garantizan que el backend nunca entre en cold start.

---

## 9. Containerización con Docker

### 9.1 Backend Dockerfile

**Estrategia:** Build multi-stage para minimizar el tamaño de la imagen final.

```
┌────────────────────────────────────────────┐
│  Stage 1: BUILD (maven:3.9-eclipse-temurin)│
│  ├── Copiar pom.xml + resolver dependencias│
│  ├── Copiar código fuente                  │
│  └── mvnw package → genera .jar            │
├────────────────────────────────────────────┤
│  Stage 2: RUN (eclipse-temurin:21-jre-alpine)│
│  ├── Crear usuario no-root (appuser)       │
│  ├── Copiar .jar desde Stage 1             │
│  └── java -jar app.jar                     │
└────────────────────────────────────────────┘
```

**Seguridad:** El contenedor ejecuta la aplicación como usuario no-root (`appuser`), siguiendo las mejores prácticas de seguridad de Docker.

### 9.2 Frontend Dockerfile

```
┌────────────────────────────────────────────┐
│  Stage 1: BUILD (node:20-alpine)           │
│  ├── npm ci (dependencias)                 │
│  └── npm run build → genera /dist          │
├────────────────────────────────────────────┤
│  Stage 2: SERVE (nginx:alpine)             │
│  ├── Copiar /dist al directorio de Nginx   │
│  ├── Copiar nginx.conf personalizado       │
│  └── Servir SPA + proxy reverso a /api/    │
└────────────────────────────────────────────┘
```

### 9.3 Docker Compose

Orquesta ambos servicios para desarrollo local:
- **Backend:** Puerto 8080, healthcheck via wget
- **Frontend:** Puerto 80, depende del backend
- Ambos contenedores se comunican a través de la red interna de Docker

### 9.4 Archivos .dockerignore

Tanto `backend/.dockerignore` como `frontend/.dockerignore` excluyen archivos innecesarios del contexto de build (node_modules, target, .git, etc.), acelerando la construcción de las imágenes.

---

## 10. Despliegue en Render

### 10.1 ¿Por qué Render?

Se eligió Render como plataforma de despliegue por:

1. **Tier gratuito generoso:** Incluye servicios web, sitios estáticos y PostgreSQL
2. **Soporte nativo de Docker:** El backend se despliega directamente desde el Dockerfile
3. **Sitios estáticos siempre activos:** El frontend no sufre cold starts
4. **Auto-deploy desde GitHub:** Render detecta nuevos commits y redespliega automáticamente
5. **Blueprint (IaC):** La infraestructura se define como código en `render.yaml`
6. **Rewrites/proxying:** El frontend puede proxy-ar peticiones `/api/*` al backend
7. **Simplicidad:** No requiere gestionar servidores, networking ni certificados SSL

### 10.2 Blueprint (`render.yaml`)

El Blueprint define la infraestructura de forma declarativa:

**Backend — Web Service (Docker):**
- **Runtime:** Docker (usa el Dockerfile del backend)
- **Root Directory:** `backend/`
- **Plan:** Free
- **Variables de entorno:**
  - `SPRING_PROFILES_ACTIVE=prod` — activa el perfil de producción
  - `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` — credenciales de Neon (`sync: false` para introducirlas manualmente)

**Frontend — Static Site:**
- **Runtime:** Static
- **Build Command:** `npm ci && npm run build`
- **Publish Path:** `dist/` (output de Vite)
- **Rewrites:**
  - `/api/*` → `https://tfg-backend-jdjl.onrender.com/api/*` (proxy al backend)
  - `/*` → `/index.html` (SPA routing para React Router)

### 10.3 ¿Por qué el frontend como Static Site?

Se eligió desplegar el frontend como **Static Site** en lugar de como servicio Docker por:

1. **Sin cold starts:** Los sitios estáticos de Render están siempre disponibles
2. **Rendimiento:** Los archivos estáticos se sirven desde CDN
3. **Coste:** Los sitios estáticos son gratuitos sin limitaciones de actividad
4. **Simplicidad:** No necesita Nginx ni configuración de servidor

### 10.4 URLs de Producción

| Servicio | URL |
|---|---|
| Frontend | `https://tfg-frontend-zmc3.onrender.com` |
| Backend API | `https://tfg-backend-jdjl.onrender.com` |

---

## 11. Base de Datos en Producción — PostgreSQL Gestionado

### 11.1 Proveedor recomendado

Se recomienda **Render Postgres** como primera opción para producción por simplicidad operativa (mismo proveedor que el backend) y menor riesgo de incidencias de cuota entre plataformas.

Neon puede seguir usándose como alternativa, pero si se usa tier gratuito es importante vigilar límites de cómputo.

1. **PostgreSQL serverless:** Se escala automáticamente
2. **Tier gratuito permanente:** 512 MB de almacenamiento, sin límite de tiempo
3. **Compatible con Hibernate/JPA:** Se usa el dialecto `PostgreSQLDialect`
4. **Conexión SSL:** Todas las conexiones son cifradas (`sslmode=require`)

### 11.2 Perfiles de Spring Boot

| Perfil | Base de datos | Uso |
|---|---|---|
| **default** (sin perfil) | H2 en memoria | Desarrollo local |
| **prod** | PostgreSQL gestionado | Producción (Render) |

**`application.properties`** (desarrollo):
- H2 en memoria con `ddl-auto=create-drop`
- Consola H2 habilitada en `/h2-console`

**`application-prod.properties`** (producción):
- PostgreSQL vía variables de entorno
- `ddl-auto=update` (actualiza esquema sin borrar datos)
- Consola H2 deshabilitada
- Logging reducido

### 11.3 Seeding de Datos

El `DataSeeder.java` es un `CommandLineRunner` con `@Profile("!prod")` que crea datos de prueba automáticamente al arrancar la aplicación en perfiles no-productivos.

**Para seedear PostgreSQL de producción:** Se ejecutó el backend local apuntando al proveedor PostgreSQL mediante variables de entorno:
```
SPRING_DATASOURCE_URL → URL JDBC PostgreSQL
SPRING_DATASOURCE_USERNAME → usuario PostgreSQL
SPRING_DATASOURCE_PASSWORD → contraseña PostgreSQL
```

El seeder tiene protección anti-duplicados: `if (usuarioRepository.count() > 0) return;`

---

## 12. Problemas Encontrados y Soluciones

### 12.1 ESLint 9 — Configuración incompatible

**Problema:** El frontend CI fallaba en `npm run lint` porque la configuración de ESLint no era compatible con ESLint 9 (formato flat config).

**Error:** ESLint no encontraba la configuración válida.

**Solución:** Se creó `eslint.config.js` usando el nuevo formato "flat config" de ESLint 9, compatible con React + TypeScript + Vite.

---

### 12.2 Render Blueprint — `plan: free` en Static Sites

**Problema:** El Blueprint de Render fallaba al sincronizar porque se especificó `plan: free` para el servicio de tipo Static Site.

**Error:** `plan: free` no es un campo válido para Static Sites en Render.

**Solución:** Se eliminó la línea `plan: free` de la configuración del frontend. Los Static Sites de Render son gratuitos por defecto.

---

### 12.3 SonarCloud — Conflicto con Automatic Analysis

**Problema:** El workflow de SonarCloud fallaba con el error: `You are running CI analysis while Automatic Analysis is enabled`.

**Error:** SonarCloud tiene un modo de "Automatic Analysis" que analiza el código automáticamente. Cuando se ejecuta también un análisis desde CI (GitHub Actions), entran en conflicto.

**Solución:** Desactivar "Automatic Analysis" en SonarCloud: Administration → Analysis Method → desactivar.

---

### 12.4 SonarCloud — Acción deprecada (v5)

**Problema:** `SonarSource/sonarcloud-github-action@master` estaba deprecada y mostraba un warning de seguridad. La siguiente versión (`sonarqube-scan-action@v5`) también tenía una vulnerabilidad conocida.

**Error:** Warning de deprecación + vulnerabilidad de seguridad.

**Solución:** Se migró primero a `@v5`, luego a `@v6`. Finalmente, se optó por usar el **plugin Maven de SonarSource** directamente (`sonar-maven-plugin:sonar`), eliminando la dependencia del CLI Scanner.

---

### 12.5 SonarCloud — Error 403 al descargar CLI Scanner

**Problema:** La acción `sonarqube-scan-action@v6` intentaba descargar el SonarScanner CLI desde `binaries.sonarsource.com` y recibía un HTTP 403.

**Error:** `Unexpected HTTP response: 403` al descargar `sonar-scanner-cli-7.2.0.5079-linux-x64.zip`.

**Causa:** Problema temporal en el CDN de SonarSource.

**Solución:** Se reestructuró el workflow para usar exclusivamente el plugin Maven de SonarCloud, que no requiere descargar el CLI Scanner. Este enfoque es además el recomendado por SonarSource para proyectos Java.

---

### 12.6 SonarCloud — Project Not Found

**Problema:** El análisis de SonarCloud fallaba con `Project not found. Please check the 'sonar.projectKey'`.

**Error:** Los project keys iniciales (`Mancalrod_TFG_backend`, `Mancalrod_TFG_frontend`) no coincidían con el proyecto creado en SonarCloud.

**Solución:** Se verificó el project key real en SonarCloud (`Mancalrod_TFG`) y se actualizaron todos los archivos de configuración.

---

### 12.7 OWASP — Descarga lenta de NVD

**Problema:** El OWASP Dependency Check tardaba 30-60 minutos en descargar la base de datos NVD sin API Key.

**Error:** Warning: `An NVD API Key was not provided`.

**Solución:** Se obtuvo una API Key gratuita de [nvd.nist.gov](https://nvd.nist.gov/developers/request-an-api-key) y se añadió como secret `NVD_API_KEY`. La descarga pasó de ~60 min a ~2-3 min.

---

### 12.8 Neon — Password authentication failed

**Problema:** Al ejecutar el backend local apuntando a Neon, la autenticación fallaba.

**Error:** `password authentication failed for user 'neondb_owner'`.

**Causa:** La contraseña de Neon había sido reseteada y el valor utilizado en la variable de entorno no era el actual.

**Solución:** Se obtuvo la contraseña actualizada desde la consola de Neon (Connection Details) y se actualizó la variable de entorno.

---

### 12.9 CORS — 403 en Frontend desplegado

**Problema:** El login desde el frontend desplegado en Render devolvía HTTP 403.

**Error:** `Failed to load resource: the server responded with a status of 403`.

**Causa:** La configuración CORS del backend (`SecurityConfig.java`) solo permitía orígenes de `localhost:3000` y `localhost:5173`. El frontend en Render (`https://tfg-frontend-zmc3.onrender.com`) no estaba en la lista de orígenes permitidos.

**Solución:** Se añadió `https://tfg-frontend-zmc3.onrender.com` a la lista de `allowedOrigins` en el bean `corsConfigurationSource()`.

---

### 12.10 Render — URLs con sufijo inesperado

**Problema:** Las URLs de los servicios en Render tenían sufijos aleatorios (`-jdjl`, `-zmc3`) que no coincidían con las configuradas en `render.yaml`.

**Causa:** Los nombres base (`tfg-backend`, `tfg-frontend`) ya estaban ocupados por otros usuarios de Render, por lo que Render asignó sufijos únicos.

**Solución:** Se actualizó el rewrite en `render.yaml` para apuntar a la URL real del backend: `https://tfg-backend-jdjl.onrender.com/api/*`.

---

### 12.11 Render — Servicio dormido (Cold Start)

**Problema:** El backend en el tier gratuito de Render se suspendía tras 15 minutos de inactividad, causando tiempos de respuesta de 30-50 segundos en la primera petición.

**Solución:** Se implementaron dos mecanismos de keep-alive:
1. **UptimeRobot** — servicio externo que hace ping al backend cada 5 minutos
2. **GitHub Actions workflow** (`keep-alive.yml`) — cron cada 14 minutos como respaldo

---

### 12.12 SonarCloud — Security Hotspot (CSRF disabled)

**Problema:** El Quality Gate de SonarCloud fallaba por un Security Hotspot: "Make sure disabling Spring Security's CSRF protection is safe here".

**Causa:** SonarCloud marca la desactivación de CSRF como un punto sensible que requiere revisión manual.

**Solución:** Se marcó como "Safe" en SonarCloud. La desactivación de CSRF es correcta porque la API usa autenticación JWT (stateless), no cookies de sesión. CSRF solo es relevante cuando se usan cookies de sesión que el navegador envía automáticamente.

---

### 12.13 Neon — Cuota gratuita de cómputo agotada

**Problema:** El backend en Render fallaba al arrancar con error JDBC al abrir conexión para DDL/migraciones.

**Error:** `ERROR: Your account or project has exceeded the compute time quota. Upgrade your plan to increase limits.`

**Causa:** La base de datos Neon alcanzó el límite de cómputo del plan gratuito.

**Solución:** Migrar a una alternativa PostgreSQL gestionada (recomendado: Render Postgres) y actualizar variables `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` en Render. Ver guía detallada en `docs/MIGRACION_NEON_A_RENDER_POSTGRES.md`.

---

## 13. Diagrama Completo del Pipeline

```
                    ┌─────────────┐
                    │  git push   │
                    │  a main     │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────────┐
              │            │                │
              ▼            ▼                ▼
     ┌────────────┐ ┌────────────┐ ┌──────────────┐
     │ Backend CI │ │Frontend CI │ │Full Stack CI │
     │  (Maven)   │ │  (npm)     │ │ (paralelo)   │
     └────────────┘ └────────────┘ └──────────────┘
              │            │                │
              ▼            ▼                ▼
     ┌────────────┐ ┌────────────┐ ┌──────────────┐
     │ SonarCloud │ │  Security  │ │  CD: Docker  │
     │  Analysis  │ │   Audit    │ │  Build+Push  │
     └────────────┘ └────────────┘ └──────────────┘
                                          │
                              ┌───────────┼───────────┐
                              ▼                       ▼
                    ┌──────────────┐         ┌──────────────┐
                    │ GHCR Backend │         │GHCR Frontend │
                    │   :latest    │         │   :latest    │
                    └──────────────┘         └──────────────┘

     ┌─────────────────────────────────────────────────────┐
     │                 RENDER (Auto-deploy)                 │
     │                                                     │
     │  ┌─────────────┐    rewrite     ┌───────────────┐  │
     │  │  Frontend    │──  /api/*  ──▶│   Backend     │  │
     │  │  Static Site │               │   Docker      │  │
     │  │  (React SPA) │               │  (Spring Boot)│  │
     │  └─────────────┘               └───────┬───────┘  │
     │                                         │          │
     └─────────────────────────────────────────┼──────────┘
                                               │
                                               ▼
                                    ┌──────────────────┐
                                    │ Neon PostgreSQL   │
                                    │  (Serverless)    │
                                    └──────────────────┘

     ┌─────────────────────────────────────────────────────┐
     │              MONITORIZACIÓN                         │
     │  UptimeRobot (5 min) + Keep-Alive GHA (14 min)    │
     └─────────────────────────────────────────────────────┘

     ┌─────────────────────────────────────────────────────┐
     │              DEPENDABOT (semanal, lunes)            │
     │  Maven + npm + GitHub Actions                       │
     └─────────────────────────────────────────────────────┘
```

---

## 14. Secrets y Variables de Entorno

### 14.1 GitHub Actions Secrets

| Secret | Propósito | Dónde obtenerlo |
|---|---|---|
| `GITHUB_TOKEN` | Autenticación GHCR, decoración PRs | Automático (GitHub lo provee) |
| `SONAR_TOKEN` | Autenticación SonarCloud | SonarCloud → My Account → Security |
| `NVD_API_KEY` | Acelerar descarga NVD (OWASP) | [nvd.nist.gov](https://nvd.nist.gov/developers/request-an-api-key) |

### 14.2 Variables de Entorno en Render

| Variable | Servicio | Valor |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Backend | `prod` |
| `DATABASE_URL` | Backend | `jdbc:postgresql://<host>:<port>/<database>?sslmode=require` |
| `DATABASE_USER` | Backend | `<usuario_postgresql>` |
| `DATABASE_PASSWORD` | Backend | `<password_postgresql>` |
| `PORT` | Backend | Asignado automáticamente por Render |

---

> **Documento generado:** 25 de febrero de 2026  
> **Proyecto:** TFG — Sistema de Gestión de Entregables  
> **Autor:** Configuración realizada colaborativamente durante el desarrollo del TFG
