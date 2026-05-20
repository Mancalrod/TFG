# Sistema de Gestion de Entregables

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6.2-blue.svg)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-6.0.7-purple.svg)](https://vitejs.dev/)

> Trabajo de Fin de Grado - Universidad de Sevilla

## Descripcion

Sistema web para la gestion de entregables academicos que permite a profesores crear cursos, actividades y entregables, y a estudiantes realizar entregas con retroalimentacion. Incluye integracion con almacenamiento en la nube (HDVirtual/OneDrive), conexion a OneDrive mediante OAuth2 y autenticacion de doble factor (2FA).

## Caracteristicas Principales

- **Gestion de Cursos y Grupos**: Creacion de cursos, asignacion de estudiantes a grupos
- **Gestion de Actividades**: Actividades con visibilidad configurable por grupo
- **Gestion de Entregables**: Entregables con fecha limite, tipos de archivo y opciones de reenvio
- **Sistema de Entregas**: Subida de archivos con historial de versiones
- **Feedback y Evaluacion**: Calificaciones y comentarios de profesores
- **Almacenamiento Cloud**: Integracion con HDVirtual de la ULL y OneDrive
- **Autenticacion Segura**: JWT, OAuth2 para OneDrive y 2FA
- **Dashboard por Rol**: Interfaz adaptada a profesor/estudiante/administrador

## Stack Tecnologico

### Backend
| Tecnologia | Version |
|------------|---------|
| Java | 21 |
| Spring Boot | 4.0.2 |
| Spring Security | 6.x |
| Spring Data JPA | 3.x |
| Maven | 3.9+ |
| H2 / MySQL | 8.x |

### Frontend
| Tecnologia | Version |
|------------|---------|
| React | 18.3.1 |
| TypeScript | 5.6.2 |
| Vite | 6.0.7 |
| React Router | 7.1.1 |
| Axios | 1.7.9 |
| Node.js | 18+ |

## Estructura del Proyecto

```
TFG/
├── backend/                    # API REST con Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tfg/entregables/
│   │   │   │   ├── controller/    # Controladores REST
│   │   │   │   ├── dto/           # Data Transfer Objects
│   │   │   │   ├── model/         # Entidades JPA
│   │   │   │   ├── repository/    # Repositorios Spring Data
│   │   │   │   └── service/       # Logica de negocio
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
├── frontend/                   # Aplicacion React + TypeScript
│   ├── src/
│   │   ├── components/         # Componentes reutilizables
│   │   ├── pages/              # Paginas de la aplicacion
│   │   ├── services/           # Servicios API
│   │   ├── types/              # Tipos TypeScript
│   │   └── App.tsx
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
├── docs/                       # Documentacion Scrum y TFG
│   ├── ERS TFG.pdf             # Especificacion de Requisitos
│   ├── DAS TFG.pdf             # Documento de Arquitectura
│   ├── PRODUCT_BACKLOG.md      # Product Backlog (99 PBIs)
│   ├── PLAN_SPRINTS.md         # Plan de Sprints
│   └── SPRINT_BACKLOG.md       # Sprint Backlog
└── README.md
```

## Instalacion y Ejecucion

### Prerrequisitos

- Java JDK 21
- Maven 3.9+
- Node.js 18+
- npm 9+
- PostgreSQL. Consulta el [manual de instalación](docs/MANUAL_INSTALACION.md) para ver la configuración y conexión.

### Backend

```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

El servidor estara disponible en `http://localhost:8080`

#### Endpoints API principales

| Metodo | Endpoint | Descripcion |
|--------|----------|-------------|
| GET | /api/health | Estado del servidor |
| GET | /api/usuarios | Listar usuarios |
| GET | /api/cursos | Listar cursos |
| GET | /api/actividades | Listar actividades |
| GET | /api/entregables | Listar entregables |
| POST | /api/entregas | Crear entrega |

### Frontend

```bash
cd frontend
npm install
npm run dev
```

La aplicacion estara disponible en `http://localhost:5173`

### Build de Produccion

```bash
# Backend
cd backend
./mvnw clean package -DskipTests
java -jar target/entregables-0.0.1-SNAPSHOT.jar

# Frontend
cd frontend
npm run build
```

## Planificacion del Proyecto

| Fase | Periodo | Horas |
|------|---------|-------|
| Sprint 0 (Documentacion) | Sept 2025 - 22 Feb 2026 | 120h |
| Sprint 1 | 17 Feb - 2 Mar 2026 | 120h |
| Sprint 2 | 3 Mar - 16 Mar 2026 | 120h |
| Sprint 3 | 17 Mar - 30 Mar 2026 | 120h |
| Sprint 4 | 31 Mar - 13 Abr 2026 | 120h |
| Sprint 5 | 14 Abr - 12 May 2026 | 60h |
| **Total** | | **660h** |

## Documentacion

- [ERS - Especificacion de Requisitos de Software](docs/ERS%20TFG.pdf)
- [DAS - Documento de Arquitectura del Sistema](docs/DAS%20TFG.pdf)
- [Manual de instalacion](docs/MANUAL_INSTALACION.md)
- [Product Backlog](docs/PRODUCT_BACKLOG.md)
- [Plan de Sprints](docs/PLAN_SPRINTS.md)
- [Sprint Backlog](docs/SPRINT_BACKLOG.md)
- [Seguridad Preproduccion y Auditoria de Permisos](docs/SECURITY_PREPROD_AUDIT.md)

## Equipo de Desarrollo

| Nombre | Rol |
|--------|-----|
| Manuel Maria Calderon Rodriguez | Desarrollador Full Stack |
| Jose Manuel Marquez Gutierrez | Desarrollador Full Stack |

**Universidad de Sevilla**  
Escuela Tecnica Superior de Ingenieria Informatica  
Grado en Ingenieria Informatica - Ingenieria del Software  
Curso 2025-2026

## Licencia

Este proyecto forma parte de un Trabajo de Fin de Grado de la Universidad de Sevilla y su uso esta sujeto a las normativas academicas correspondientes.

---

*Ultima actualizacion: Mayo 2026*
