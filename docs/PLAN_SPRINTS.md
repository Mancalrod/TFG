# Plan de Sprints - Sistema de Gestion de Entregables

## Informacion del Proyecto

| Campo | Valor |
|-------|-------|
| **Proyecto** | Sistema de Gestion de Entregables (TFG) |
| **Horas Totales** | 660 horas |
| **Equipo** | 2 personas |
| **Horas por Persona/Semana** | 30 horas |
| **Horas por Semana (total)** | 60 horas |
| **Duracion Sprint** | 2 semanas (excepto Sprint 5) |
| **Horas por Sprint** | 120 horas (60h/persona), excepto Sprint 5 (60h total) |
| **Total Sprints** | 5 sprints (+ Sprint 0) |
| **Inicio Sprint 0** | Septiembre 2025 |
| **Fin Sprint 0** | 22 Febrero 2026 |
| **Inicio Sprints 1-5** | 17 Febrero 2026 |
| **Fecha Fin Estimada** | 12 Mayo 2026 |

---

## Definicion del MVP

El MVP del sistema debe cubrir obligatoriamente:

1. **Gestion de cursos y tareas** - CRUD completo de cursos, grupos y actividades.
2. **Subida y gestion de entregables por parte del alumno** - El alumno puede subir entregas, ver historial y estado.
3. **Panel de evaluacion y notas** - El profesor puede calificar entregas y dar feedback.
4. **Vista de profesor "Usable"** - Gestion agil de correcciones, descarga de archivos, busqueda/filtros, exportacion de calificaciones. Mejora sustancial respecto a Blackboard/ensenanza virtual.

### Fuera del alcance MVP (trabajo futuro)
- OAuth2 (Google / ULL institucional)
- Autenticacion 2FA (TOTP/SMS)
- Notificaciones de feedback
- Calendario de fechas limite
- Monitorizacion avanzada
- Foto de perfil de usuario

---

## Estrategia de Testing

El testing se distribuye como tarea integrada en cada sprint de funcionalidades:

| Sprint | Testing incluido |
|--------|-----------------|
| Sprint 1 | Tests unitarios backend (controllers, services, security) - 18 clases |
| Sprint 2 | Tests entregables + entregas (ampliar cobertura backend) |
| Sprint 3 | Tests feedback + calificacion + integracion |
| Sprint 4 | Tests E2E frontend + documentacion de pruebas + pruebas finales |
| Sprint 5 | Revision final, regression, correccion de incidencias y verificacion documental |

---

## Resumen de Sprints

| Sprint | Fechas | Horas | Objetivo Principal | Estado |
|--------|--------|-------|---------------------|--------|
| Sprint 0 | Sep 2025 - 22 Feb 2026 | 120h | Documentacion: ERS y DAS | COMPLETADO |
| Sprint 1 | 17 Feb - 2 Mar 2026 | 120h | Infraestructura + Auth + Cursos + Actividades + CI/CD + Deploy | COMPLETADO |
| Sprint 2 | 3 Mar - 16 Mar 2026 | 120h | Gestion Entregables + Entregas + Integracion OneDrive | EN CURSO |
| Sprint 3 | 17 Mar - 30 Mar 2026 | 120h | Panel Evaluacion + Feedback + UX Profesor | PENDIENTE |
| Sprint 4 | 31 Mar - 13 Abr 2026 | 120h | Memoria TFG + Polish + Presentacion | PENDIENTE |
| Sprint 5 | 14 Abr - 12 May 2026 | 60h | Revision final + correccion de errores + ajustes de memoria | PENDIENTE |

**Total horas:** 120h (Sprint 0) + 480h (Sprints 1-4) + 60h (Sprint 5) = **660 horas**

---

## Sprint 0: Documentacion y Analisis - COMPLETADO
**Estado:** COMPLETADO
**Fechas:** Septiembre 2025 - 22 Febrero 2026
**Horas:** ~120h

### Objetivos
- Elaborar documento de requisitos del sistema (ERS)
- Elaborar documento de arquitectura del sistema (DAS)
- Definir casos de uso y modelo de datos
- Planificar sprints de desarrollo

### Entregables
- [x] ERS - Especificacion de Requisitos del Sistema
- [x] DAS - Documento de Arquitectura del Sistema
- [x] Casos de uso (CU-001 a CU-025)
- [x] Requisitos funcionales (RQF-001 a RQF-041)
- [x] Requisitos no funcionales (RQI-001 a RQI-015)
- [x] Modelo entidad-relacion
- [x] Product Backlog inicial
- [x] Plan de Sprints

### Desglose de Horas
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| Analisis inicial y reuniones | 10h | 10h | 20h |
| Elaboracion ERS | 15h | 15h | 30h |
| Elaboracion DAS | 15h | 15h | 30h |
| Modelo de datos | 8h | 8h | 16h |
| Casos de uso | 6h | 6h | 12h |
| Planificacion y backlog | 6h | 6h | 12h |
| **Total** | **60h** | **60h** | **120h** |

---

## Sprint 1: Infraestructura + Auth + Cursos + Actividades + CI/CD + Deploy - COMPLETADO
**Estado:** COMPLETADO
**Fechas:** 17 Febrero - 2 Marzo 2026
**Horas:** 120h (60h/persona)

### Objetivos
- Configurar proyecto backend Spring Boot completo
- Implementar todas las entidades JPA, repositorios, servicios y controladores
- Configurar autenticacion JWT completa (access + refresh tokens)
- Desarrollar frontend: Login, Dashboard, Cursos, Actividades (CRUD completo)
- Visualizacion de entregables (solo lectura)
- Configurar CI/CD con GitHub Actions (7 workflows)
- Desplegar aplicacion en Render + Neon PostgreSQL
- Dockerizar la aplicacion (multi-stage builds)
- Implementar modo oscuro/claro
- Testing de funcionalidades implementadas (18 clases de test)

### Epicas/PBIs Completados
- EP-02: Arquitectura y Configuracion (PBI-007 a PBI-011) - COMPLETA
- EP-03: Gestion de Usuarios (PBI-012 a PBI-016) - COMPLETA
- EP-04: Gestion de Cursos (PBI-018 a PBI-023) - COMPLETA
- EP-05: Gestion de Actividades (PBI-024 a PBI-030) - COMPLETA
- EP-06 parcial: Backend entregables completo (PBI-031 a PBI-035 backend)
- EP-07 parcial: Backend entregas completo (PBI-036 a PBI-042 backend)
- EP-09 parcial: Backend feedback completo (PBI-050 a PBI-053 backend)
- EP-10: Frontend UI Base (PBI-055 a PBI-062) - COMPLETA
- EP-11 parcial: Login, Dashboard, Cursos, Actividades, Vista entregables (PBI-063 a PBI-067)
- EP-12 parcial: JWT completo (PBI-073 a PBI-076, PBI-081)
- EP-13 parcial: 18 clases de test (controllers, services, security)
- EP-14: Despliegue y DevOps (PBI-087 a PBI-091) - COMPLETA

### Funcionalidades Backend Implementadas
- **10 entidades JPA** con relaciones completas (Usuario, Profesor, Estudiante, Curso, Grupo, Actividad, Entregable, Entrega, Feedback, Material)
- **4 enums** (EstadoEntrega, TipoActividad, TipoMaterial, Visibilidad)
- **10 repositorios** Spring Data con queries JPQL personalizadas
- **7 servicios** (UsuarioService, CursoService, ActividadService, EntregableService, EntregaService, FeedbackService, EntityMapper)
- **8 controladores REST** con ~60 endpoints
- **19 DTOs** con patron mapper (EntityMapper)
- Autenticacion JWT (access tokens 24h + refresh tokens 7 dias)
- Roles: ROLE_ADMIN, ROLE_PROFESOR, ROLE_ESTUDIANTE, ROLE_USER
- Spring Security con proteccion de rutas por rol
- GlobalExceptionHandler (404, 400, 409, 500)
- DataSeeder con datos de desarrollo (10 usuarios, cursos, actividades, entregables, entregas, feedback)
- Upload de archivos multipart hasta 50MB
- SpaController para React Router

### Funcionalidades Frontend Implementadas
- **Login** con JWT y AuthContext
- **Dashboard** con cursos filtrados por rol y busqueda
- **CRUD completo de actividades** (vista profesor y estudiante)
- **Vista de cursos** y detalle de curso
- **Vista de entregables** (solo lectura)
- **Navbar** con navegacion por rol
- **Modo oscuro/claro** con persistencia en localStorage
- **Rutas protegidas** con redireccion a login
- **Interceptor Axios** con refresh automatico de tokens
- **Tipos TypeScript** para todos los DTOs (22 interfaces + 4 enums)
- **8 modulos de servicios API** (auth, usuario, curso, actividad, entregable, entrega, feedback, api base)

### DevOps Implementado
- **Docker**: 3 Dockerfiles multi-stage (root, backend, frontend) + docker-compose.yml
- **CI**: backend-ci.yml, frontend-ci.yml, full-stack-ci.yml
- **CD**: cd.yml (build + push a GHCR)
- **Calidad**: sonarcloud.yml (analisis SonarCloud)
- **Seguridad**: security-audit.yml (npm audit + OWASP Dependency Check)
- **Disponibilidad**: keep-alive.yml (ping cada 14 min a Render)
- **Despliegue**: render.yaml (Render Blueprint con PostgreSQL Neon)

### Testing Implementado (18 clases)
- 8 tests de controladores (Actividad, Auth, Curso, Entregable, Entrega, Feedback, Health, Usuario)
- 7 tests de servicios (Actividad, Curso, EntityMapper, Entregable, Entrega, Feedback, Usuario)
- 3 tests de seguridad (CustomUserDetailsService, JwtAuthenticationFilter, JwtTokenProvider)
- 1 test de configuracion (SpaController)
- 1 test de aplicacion (GestionEntregablesApplicationTests)

### Desglose de Horas
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| Config Spring Boot + React + BD | 4h | 4h | 8h |
| Entidades JPA (10) y repositorios (10) | 6h | 6h | 12h |
| DTOs (19) y EntityMapper | 3h | 3h | 6h |
| Servicios (Usuario, Curso, Actividad) | 6h | 6h | 12h |
| Servicios (Entregable, Entrega, Feedback) | 5h | 5h | 10h |
| Controladores REST (8, ~60 endpoints) | 5h | 5h | 10h |
| Autenticacion JWT + Spring Security | 5h | 5h | 10h |
| Frontend: Login + AuthContext | 3h | 3h | 6h |
| Frontend: Dashboard + Cursos | 4h | 4h | 8h |
| Frontend: Actividades CRUD completo | 5h | 5h | 10h |
| Frontend: Vista entregables + Navbar + Theme | 3h | 3h | 6h |
| Docker + CI/CD (7 workflows) + Deploy Render | 4h | 4h | 8h |
| DataSeeder + Config (perfiles, SPA, CORS) | 2h | 2h | 4h |
| Testing (18 clases de test) | 5h | 5h | 10h |
| **Total** | **60h** | **60h** | **120h** |

---

## Sprint 2: Gestion Entregables + Entregas + Integracion OneDrive - EN CURSO
**Estado:** EN CURSO
**Fechas:** 3 Marzo - 16 Marzo 2026
**Horas Planificadas:** 120h (60h/persona)

### Objetivos
- Completar frontend CRUD de entregables (crear, editar, eliminar, visibilidad)
- Implementar formulario de entrega para estudiantes (subida de archivos)
- Implementar vista de entregas del estudiante (historial, versiones, estado)
- Implementar vista de entregas del profesor (lista por entregable)
- Descarga individual de archivos
- Reenvio de entregas si esta permitido
- **Integracion con Microsoft OneDrive** (OAuth2, subida/descarga de archivos en la nube)
- Testing de funcionalidades de este sprint

### Epicas/PBIs Incluidos
- EP-06 completa: Frontend Gestion Entregables (PBI-031 a PBI-035 frontend)
- EP-07: Gestion de Entregas frontend (PBI-036 a PBI-042)
- EP-08 nueva: Integracion OneDrive (entidad OneDriveToken, OneDriveService, OneDriveController, config OAuth2)
- EP-11 parcial: Paginas entregable completa y entregas (PBI-068)
- EP-13 parcial: Testing funcionalidades Sprint 2

### Desglose de Horas (Estimado)
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| Frontend: Crear entregable (formulario profesor) | 5h | 5h | 10h |
| Frontend: Editar entregable (completar pagina) | 4h | 4h | 8h |
| Frontend: Eliminar + toggle visibilidad entregable | 3h | 3h | 6h |
| Frontend: Formulario entrega estudiante (drag & drop) | 6h | 6h | 12h |
| Frontend: Subida archivos multipart + progreso | 5h | 5h | 10h |
| Frontend: Vista entregas estudiante (historial versiones) | 5h | 5h | 10h |
| Frontend: Vista entregas profesor (por entregable) | 5h | 5h | 10h |
| Frontend: Descarga archivos individual | 3h | 3h | 6h |
| Frontend: Reenvio entregas (si permitido) | 3h | 3h | 6h |
| Mejoras UX y navegacion (breadcrumbs, toasts) | 3h | 3h | 6h |
| Backend: OneDriveToken + OneDriveTokenRepository + OneDriveConfig | 1h | 1h | 2h |
| Backend: OneDriveService (OAuth2, token exchange, upload, download) | 2h | 2h | 4h |
| Backend: OneDriveController (auth-url, callback, status, disconnect) | 1h | 1h | 2h |
| Frontend: oneDriveService.ts + integracion subida a OneDrive | 1h | 1h | 2h |
| Testing: Tests entregables (ampliar service + controller) | 4h | 4h | 8h |
| Testing: Tests entregas (ampliar service + controller) | 5h | 5h | 10h |
| Testing: Tests integracion flujo completo | 4h | 4h | 8h |
| Buffer imprevistos | 3h | 3h | 6h |
| **Total** | **60h** | **60h** | **120h** |

### Criterios de Aceptacion
- [ ] Profesor puede crear entregables dentro de una actividad
- [ ] Profesor puede editar y eliminar entregables
- [ ] Profesor puede cambiar visibilidad de entregables (VISIBLE/OCULTO)
- [ ] Estudiante puede realizar entregas subiendo archivos
- [ ] Estudiante puede ver historial de versiones de sus entregas
- [ ] Estudiante puede reenviar entregas si esta permitido y en plazo
- [ ] Profesor puede ver lista de entregas por entregable
- [ ] Se pueden descargar archivos de entregas
- [ ] Usuario puede conectar su cuenta de Microsoft OneDrive (OAuth2)
- [ ] Archivos de entregas se suben a OneDrive si el usuario tiene la cuenta conectada
- [ ] Respuesta de entrega incluye `almacenadoEnOneDrive`, `onedriveFileId` y `onedriveWebUrl`
- [ ] Tests unitarios y de integracion pasando
- [ ] Aplicacion desplegada y funcional en Render

---

## Sprint 3: Panel Evaluacion + Feedback + UX Profesor - PENDIENTE
**Estado:** PENDIENTE
**Fechas:** 17 Marzo - 30 Marzo 2026
**Horas Planificadas:** 120h (60h/persona)

### Objetivos
- Implementar panel de calificacion para profesores
- Implementar sistema completo de feedback (CRUD)
- Implementar vista de calificaciones y feedback para estudiantes
- Descarga masiva de entregas para profesores
- Busqueda y filtros en listados
- Exportacion de calificaciones a CSV/Excel
- Mejorar UX del flujo de trabajo docente
- Testing de funcionalidades de este sprint
- Iniciar estructura de la memoria TFG
- Revisar ERS y DAS del TFG

### Epicas/PBIs Incluidos
- EP-09: Feedback y Evaluacion completa (PBI-050 a PBI-053 frontend)
- EP-07 parcial: Estadisticas entregas (PBI-043)
- EP-11 parcial: Pagina evaluacion (PBI-069), exportar CSV (PBI-071), busqueda/filtros (PBI-072)
- EP-13 parcial: Testing Sprint 3
- EP-15 parcial: Inicio memoria TFG

### Desglose de Horas (Estimado)
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| Frontend: Panel calificacion profesor | 6h | 6h | 12h |
| Frontend: Formulario feedback (crear/editar/eliminar) | 5h | 5h | 10h |
| Frontend: Vista feedback estudiante | 4h | 4h | 8h |
| Frontend: Vista calificaciones estudiante | 4h | 4h | 8h |
| Frontend: Descarga masiva entregas (ZIP) | 4h | 4h | 8h |
| Backend: Endpoint descarga masiva (ZIP) | 3h | 3h | 6h |
| Frontend: Busqueda y filtros en listados | 4h | 4h | 8h |
| Frontend: Exportar calificaciones CSV/Excel | 3h | 3h | 6h |
| Frontend: Estadisticas entregas (profesor) | 3h | 3h | 6h |
| Mejoras UX profesor (flujo agil correcciones) | 3h | 3h | 6h |
| Testing: Tests feedback (service + controller) | 3h | 3h | 6h |
| Testing: Tests calificacion + integracion | 4h | 4h | 8h |
| Memoria: Estructura + Introduccion + Objetivos | 5h | 5h | 10h |
| Memoria: Metodologia | 4h | 4h | 8h |
| Buffer imprevistos | 3h | 3h | 6h |
| Revisar ERS y DAS del TFG| 2h | 2h | 4h
| **Total** | **60h** | **60h** | **120h** |

### Criterios de Aceptacion
- [ ] Profesor puede calificar entregas con nota numerica
- [ ] Profesor puede escribir, editar y eliminar feedback
- [ ] Estudiante puede ver sus calificaciones por entregable
- [ ] Estudiante puede leer feedback de los profesores
- [ ] Profesor puede descargar todas las entregas de un entregable (ZIP)
- [ ] Listados tienen busqueda y filtros funcionales
- [ ] Profesor puede exportar calificaciones a CSV
- [ ] Profesor puede ver estadisticas de entregas por entregable
- [ ] Flujo de correccion del profesor es fluido y eficiente
- [ ] Tests pasando para nuevas funcionalidades
- [ ] Estructura de la memoria TFG iniciada (introduccion, objetivos, metodologia)

---

## Sprint 4: Memoria TFG + Polish + Presentacion - PENDIENTE
**Estado:** PENDIENTE
**Fechas:** 31 Marzo - 13 Abril 2026
**Horas Planificadas:** 120h (60h/persona)

### Objetivos
- Completar la memoria del TFG
- Elaborar manuales de usuario y tecnico
- Polish final de UX/UI
- Testing E2E y pruebas finales
- Correccion de bugs
- Preparar y ensayar la presentacion de defensa

### Epicas/PBIs Incluidos
- EP-15: Documentacion Final y Memoria (PBI-092 a PBI-099)
- EP-13 parcial: Tests E2E frontend (PBI-084), documentar pruebas (PBI-085)
- Polish y correccion de bugs

### Desglose de Horas (Estimado)
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| Memoria: Desarrollo y resultados | 8h | 8h | 16h |
| Memoria: Conclusiones y trabajo futuro | 5h | 5h | 10h |
| Memoria: Revision y formato segun normativa | 5h | 5h | 10h |
| Manual de usuario | 5h | 5h | 10h |
| Manual tecnico / guia de instalacion | 5h | 5h | 10h |
| Testing: E2E frontend | 5h | 5h | 10h |
| Testing: Pruebas integracion finales | 4h | 4h | 8h |
| Testing: Documentar casos de prueba | 3h | 3h | 6h |
| Polish UX/UI + responsive | 5h | 5h | 10h |
| Correccion bugs | 4h | 4h | 8h |
| Preparar presentacion defensa | 5h | 5h | 10h |
| Ensayo presentacion | 3h | 3h | 6h |
| Buffer imprevistos | 3h | 3h | 6h |
| **Total** | **60h** | **60h** | **120h** |

### Criterios de Aceptacion
- [ ] Memoria TFG completa segun normativa
- [ ] Manual de usuario completo
- [ ] Manual tecnico / guia de instalacion completo
- [ ] Tests E2E del frontend pasando
- [ ] Documentacion de casos de prueba
- [ ] UX pulida y responsive
- [ ] Bugs criticos corregidos
- [ ] Presentacion preparada y ensayada
- [ ] Aplicacion desplegada y estable en produccion

---

## Sprint 5: Revision final + cierre documental - PENDIENTE
**Estado:** PENDIENTE
**Fechas:** 14 Abril - 12 Mayo 2026
**Horas Planificadas:** 60h (30h/persona)

### Objetivos
- Revision integral de la aplicacion (flujo completo y regresion)
- Correccion de errores detectados y ajustes de estabilidad
- Retoques finales de la memoria TFG y documentacion asociada
- Verificacion de despliegue y checklist de defensa

### Desglose de Horas (Estimado)
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| Revision funcional y smoke tests | 6h | 6h | 12h |
| Correccion de errores detectados | 8h | 8h | 16h |
| Ajustes de memoria y coherencia documental | 6h | 6h | 12h |
| Repaso de manuales y anexos | 4h | 4h | 8h |
| Verificacion de despliegue + checklist defensa | 3h | 3h | 6h |
| Buffer imprevistos | 3h | 3h | 6h |
| **Total** | **30h** | **30h** | **60h** |

### Criterios de Aceptacion
- [ ] Errores criticos corregidos y sin regresiones
- [ ] Memoria ajustada y coherente con el estado final
- [ ] Verificacion de despliegue y checklist final completados

---

## Calendario Visual

```
2026
         FEBRERO                    MARZO                     ABRIL                     MAYO
    L  M  X  J  V  S  D       L  M  X  J  V  S  D       L  M  X  J  V  S  D
                         1                         1          1  2  3  4  5
     2  3  4  5  6  7  8       2  3  4  5  6  7  8       6  7  8  9 10 11 12       4  5  6  7  8  9 10
     9 10 11 12 13 14 15       9 10 11 12 13 14 15      13 14 15 16 17 18 19      11 12 13 14 15 16 17
   16 17 18 19 20 21 22      16 17 18 19 20 21 22      20 21 22 23 24 25 26      18 19 20 21 22 23 24
   [Sprint 0 fin: 22]        23 24 25 26 27 28 29      27 28 29 30                25 26 27 28 29 30 31
   [Sprint 1: 17-28 Feb]     30 31
   [Sprint 1: hasta 2 Mar]
                             [Sprint 2: 3-16 Mar]
                             [Sprint 3: 17-30 Mar]
                                                           [Sprint 4: 31 Mar-13 Abr]
                                                           [Sprint 5: 14 Abr-12 May]
```

---

## Metricas de Seguimiento

### Velocidad por Sprint
| Metrica | Sprint 0 | Sprint 1 | Sprint 2 | Sprint 3 | Sprint 4 | Sprint 5 |
|---------|----------|----------|----------|----------|----------|----------|
| Horas Planificadas | 120h | 120h | 120h | 120h | 120h | 60h |
| Horas Completadas | 120h | 120h | - | - | - | - |
| % Completado | 100% | 100% | - | - | - | - |
| PBIs Completados | 6 | ~40 | - | - | - | - |

### Progreso Acumulado
| Sprint | Horas Acumuladas | % del Total (660h) |
|--------|-----------------|---------------------|
| Sprint 0 | 120h | 18% |
| Sprint 1 | 240h | 36% |
| Sprint 2 | 360h | 55% |
| Sprint 3 | 480h | 73% |
| Sprint 4 | 600h | 91% |
| Sprint 5 | 660h | 100% |

---

## Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigacion |
|--------|--------------|---------|------------|
| Complejidad UX panel evaluacion profesor | Media | Alto | Prototipar flujo antes de implementar |
| Tiempo memoria TFG | Alta | Alto | Iniciar en Sprint 3, documentar durante desarrollo |
| Bugs en integracion frontend-backend entregas | Media | Medio | Tests de integracion por sprint |
| Disponibilidad 30h/persona/semana | Media | Alto | Buffer en cada sprint |
| Complejidad descarga masiva archivos | Baja | Medio | Implementar con ZIP en backend |

---

## Historial de Cambios

| Fecha | Version | Cambios |
|-------|---------|---------|
| Feb 2026 | 1.0 | Creacion inicial |
| 18 Feb 2026 | 2.0 | Reestructuracion: 20h/persona/semana, 6 sprints de 80h |
| 3 Mar 2026 | 3.0 | Reestructuracion mayor: 30h/persona/semana, 4 sprints de 120h. Sprint 1 completado con toda la infraestructura, auth, cursos, actividades, CI/CD, deploy y testing. MVP priorizado. OAuth/2FA/Cloud storage movidos a trabajo futuro. Testing distribuido por sprint. |
|16 Mar 2026 | 4.0 | Fechas ajustadas: Sprint 1 → 17 Feb-2 Mar, Sprint 2 → 3-16 Mar, Sprint 3 → 17-30 Mar, Sprint 4 → 31 Mar-13 Abr. Integracion OneDrive anadida a Sprint 2 (implementada y funcional). |
|18 Mar 2026 | 5.0 | Anadido apartado de revision de documentacion existente en el sprint 3. |
|07 May 2026 | 6.0 | Anade Sprint 5 de revision final, ajuste de horas a 660h y fecha fin 12 May. |

---

*Ultima actualizacion: 07 Mayo 2026*
