# Product Backlog - Sistema de Gestion de Entregables

## Informacion del Proyecto

| Campo | Valor |
|-------|-------|
| **Proyecto** | Sistema de Gestion de Entregables (TFG) |
| **Product Owner** | Tutor TFG |
| **Horas Totales Estimadas** | 600 horas |
| **Equipo** | 2 personas |
| **Horas Semanales Disponibles** | 60 horas/semana (30h/persona) |
| **Sprint 0** | Sept 2025 - 22 Feb 2026 (~120h) |
| **Sprints 1-4** | 24 Feb - 19 Abr 2026 (480h) |
| **Duracion Sprint** | 2 semanas (120h/sprint) |

---

## Leyenda de Prioridades

| Prioridad | Descripcion | Criterio |
|-----------|-------------|----------|
| **Must Have** | Imprescindible para MVP | Sin esto el sistema no funciona |
| **Should Have** | Importante | Necesario para funcionalidad completa |
| **Could Have** | Deseable | Mejora la experiencia pero no es critico |
| **Won't Have** | Fuera de alcance MVP | Trabajo futuro post-TFG |

---

## Definicion del MVP

1. **Gestion de cursos y tareas** - CRUD completo de cursos, grupos y actividades
2. **Subida y gestion de entregables por parte del alumno** - Entregas con archivos, historial, reenvio
3. **Panel de evaluacion y notas** - Calificacion y feedback por el profesor
4. **Vista de profesor "Usable"** - Gestion agil correcciones, descarga masiva, filtros, export CSV

---

## Resumen por Epicas

| Epica | Items | Sprint | Estado |
|-------|-------|--------|--------|
| EP-01: Documentacion y Analisis | 6 | Sprint 0 | COMPLETADA |
| EP-02: Arquitectura y Configuracion | 5 | Sprint 1 | COMPLETADA |
| EP-03: Gestion de Usuarios | 5 | Sprint 1 | COMPLETADA |
| EP-04: Gestion de Cursos | 6 | Sprint 1 | COMPLETADA |
| EP-05: Gestion de Actividades | 7 | Sprint 1 | COMPLETADA |
| EP-06: Gestion de Entregables | 5 | Sprint 1 (backend) + Sprint 2 (frontend) | EN CURSO |
| EP-07: Gestion de Entregas | 8 | Sprint 1 (backend) + Sprint 2 (frontend) | EN CURSO |
| EP-08: Almacenamiento Cloud | 6 | - | FUERA DE ALCANCE MVP |
| EP-09: Feedback y Evaluacion | 4 | Sprint 1 (backend) + Sprint 3 (frontend) | PENDIENTE |
| EP-10: Frontend - UI Base | 7 | Sprint 1 | COMPLETADA |
| EP-11: Frontend - Modulos | 10 | Sprint 1-3 | EN CURSO |
| EP-12: Autenticacion | 5 (MVP) | Sprint 1 | COMPLETADA (MVP) |
| EP-13: Testing | 5 | Sprint 1-4 (distribuido) | EN CURSO |
| EP-14: Despliegue y DevOps | 5 | Sprint 1 | COMPLETADA |
| EP-15: Documentacion Final y Memoria TFG | 8 | Sprint 3-4 | PENDIENTE |

---

## Product Backlog Items (PBIs)

### EP-01: Documentacion y Analisis - COMPLETADA
*Sprint 0 - Completada (Sept 2025 - 22 Feb 2026)*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-001 | Como equipo, necesito un documento de requisitos (ERS) | Must | COMPLETADO | 0 |
| PBI-002 | Como equipo, necesito un documento de arquitectura (DAS) | Must | COMPLETADO | 0 |
| PBI-003 | Como equipo, necesito definir los casos de uso del sistema | Must | COMPLETADO | 0 |
| PBI-004 | Como equipo, necesito especificar los requisitos funcionales | Must | COMPLETADO | 0 |
| PBI-005 | Como equipo, necesito especificar los requisitos no funcionales | Should | COMPLETADO | 0 |
| PBI-006 | Como equipo, necesito crear el modelo de datos del sistema | Must | COMPLETADO | 0 |

---

### EP-02: Arquitectura y Configuracion - COMPLETADA
*Sprint 1*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-007 | Como desarrollador, necesito configurar el proyecto backend Spring Boot | Must | COMPLETADO | 1 |
| PBI-008 | Como desarrollador, necesito configurar el proyecto frontend React + TypeScript + Vite | Must | COMPLETADO | 1 |
| PBI-009 | Como desarrollador, necesito configurar la base de datos (H2 dev / PostgreSQL Neon prod) | Must | COMPLETADO | 1 |
| PBI-010 | Como desarrollador, necesito implementar las 10 entidades JPA con relaciones | Must | COMPLETADO | 1 |
| PBI-011 | Como desarrollador, necesito crear los 10 repositorios Spring Data con queries JPQL | Must | COMPLETADO | 1 |

---

### EP-03: Gestion de Usuarios - COMPLETADA
*Sprint 1*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-012 | Como administrador, quiero crear usuarios en el sistema | Must | COMPLETADO | 1 |
| PBI-013 | Como usuario, quiero ver mi perfil con mis datos (via /api/auth/me) | Must | COMPLETADO | 1 |
| PBI-014 | Como administrador, quiero asignar roles a los usuarios | Must | COMPLETADO | 1 |
| PBI-015 | Como usuario, quiero actualizar mi informacion de perfil | Should | COMPLETADO | 1 |
| PBI-016 | Como administrador, quiero listar todos los usuarios | Should | COMPLETADO | 1 |

*Nota: PBI-017 (foto de perfil) movido a Won't Have MVP*

---

### EP-04: Gestion de Cursos - COMPLETADA
*Sprint 1*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-018 | Como profesor, quiero crear un nuevo curso | Must | COMPLETADO | 1 |
| PBI-019 | Como profesor, quiero ver la lista de mis cursos | Must | COMPLETADO | 1 |
| PBI-020 | Como estudiante, quiero ver los cursos en los que estoy matriculado | Must | COMPLETADO | 1 |
| PBI-021 | Como profesor, quiero editar la informacion de un curso | Should | COMPLETADO | 1 |
| PBI-022 | Como profesor, quiero crear grupos dentro de un curso | Must | COMPLETADO | 1 |
| PBI-023 | Como profesor, quiero asignar estudiantes a grupos | Must | COMPLETADO | 1 |

---

### EP-05: Gestion de Actividades - COMPLETADA
*Sprint 1*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-024 | Como profesor, quiero crear una actividad en un curso | Must | COMPLETADO | 1 |
| PBI-025 | Como profesor, quiero ver todas las actividades de un curso | Must | COMPLETADO | 1 |
| PBI-026 | Como estudiante, quiero ver las actividades visibles de mi grupo | Must | COMPLETADO | 1 |
| PBI-027 | Como profesor, quiero editar una actividad existente | Should | COMPLETADO | 1 |
| PBI-028 | Como profesor, quiero cambiar la visibilidad de una actividad | Must | COMPLETADO | 1 |
| PBI-029 | Como profesor, quiero asignar actividades a grupos especificos | Should | COMPLETADO | 1 |
| PBI-030 | Como profesor, quiero eliminar una actividad | Should | COMPLETADO | 1 |

---

### EP-06: Gestion de Entregables - EN CURSO
*Sprint 1 (backend completo) + Sprint 2 (frontend)*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-031 | Como profesor, quiero crear entregables dentro de una actividad | Must | EN CURSO (backend OK, frontend Sprint 2) | 1-2 |
| PBI-032 | Como profesor, quiero definir fecha limite y tipos de archivo permitidos | Must | EN CURSO (backend OK, frontend Sprint 2) | 1-2 |
| PBI-033 | Como estudiante, quiero ver los entregables de una actividad | Must | COMPLETADO (backend + frontend vista) | 1 |
| PBI-034 | Como profesor, quiero editar un entregable | Should | EN CURSO (backend OK, frontend Sprint 2) | 1-2 |
| PBI-035 | Como profesor, quiero configurar si un entregable permite reenvios | Should | EN CURSO (backend OK, frontend Sprint 2) | 1-2 |

---

### EP-07: Gestion de Entregas - EN CURSO
*Sprint 1 (backend completo) + Sprint 2 (frontend)*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-036 | Como estudiante, quiero realizar una entrega de un entregable | Must | EN CURSO (backend OK, frontend Sprint 2) | 1-2 |
| PBI-037 | Como estudiante, quiero subir archivos en mi entrega | Must | EN CURSO (backend multipart OK, frontend Sprint 2) | 1-2 |
| PBI-038 | Como estudiante, quiero ver el detalle de mis entregas | Must | EN CURSO (backend OK, frontend Sprint 2) | 1-2 |
| PBI-039 | Como profesor, quiero ver las entregas de un entregable | Must | EN CURSO (backend OK, frontend Sprint 2) | 1-2 |
| PBI-040 | Como estudiante, quiero ver el historial de versiones de mis entregas | Should | EN CURSO (backend OK, frontend Sprint 2) | 1-2 |
| PBI-041 | Como estudiante, quiero reenviar una entrega si esta permitido | Should | EN CURSO (backend OK, frontend Sprint 2) | 1-2 |
| PBI-042 | Como profesor, quiero descargar los archivos de una entrega | Must | EN CURSO (backend OK, frontend Sprint 2) | 1-2 |
| PBI-043 | Como profesor, quiero ver estadisticas de entregas | Could | PENDIENTE (backend OK, frontend Sprint 3) | 3 |

---

### EP-08: Almacenamiento Cloud - FUERA DE ALCANCE MVP
*Trabajo futuro*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-044 | Como sistema, necesito integracion con HDVirtual de la ULL | Won't | FUERA DE ALCANCE | - |
| PBI-045 | Como sistema, necesito integracion con OneDrive | Won't | FUERA DE ALCANCE | - |
| PBI-046 | Como estudiante, quiero subir archivos directamente a la nube | Won't | FUERA DE ALCANCE | - |
| PBI-047 | Como profesor, quiero descargar archivos desde almacenamiento cloud | Won't | FUERA DE ALCANCE | - |
| PBI-048 | Como sistema, necesito gestionar cuotas de almacenamiento | Won't | FUERA DE ALCANCE | - |
| PBI-049 | Como sistema, necesito sincronizacion automatica con la nube | Won't | FUERA DE ALCANCE | - |

---

### EP-09: Feedback y Evaluacion - PENDIENTE (frontend)
*Sprint 1 (backend completo) + Sprint 3 (frontend)*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-050 | Como profesor, quiero calificar una entrega | Must | EN CURSO (backend OK, frontend Sprint 3) | 1-3 |
| PBI-051 | Como profesor, quiero anadir feedback/comentarios a una entrega | Must | EN CURSO (backend OK, frontend Sprint 3) | 1-3 |
| PBI-052 | Como estudiante, quiero ver el feedback de mis entregas | Must | EN CURSO (backend OK, frontend Sprint 3) | 1-3 |
| PBI-053 | Como profesor, quiero modificar un feedback existente | Should | EN CURSO (backend OK, frontend Sprint 3) | 1-3 |

*Nota: PBI-054 (notificaciones feedback) movido a Won't Have MVP*

---

### EP-10: Frontend - UI Base - COMPLETADA
*Sprint 1*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-055 | Como usuario, quiero un diseno responsive que funcione en movil y desktop | Must | COMPLETADO | 1 |
| PBI-056 | Como usuario, quiero una navegacion clara y consistente (Navbar) | Must | COMPLETADO | 1 |
| PBI-057 | Como usuario, quiero ver mensajes de error claros (GlobalExceptionHandler) | Must | COMPLETADO | 1 |
| PBI-058 | Como usuario, quiero un dashboard personalizado segun mi rol | Must | COMPLETADO | 1 |
| PBI-059 | Como usuario, quiero indicadores de carga mientras se procesan datos | Should | COMPLETADO | 1 |
| PBI-061 | Como usuario, quiero modo oscuro/claro (ThemeContext + persistencia) | Should | COMPLETADO | 1 |
| PBI-062 | Como desarrollador, necesito componentes reutilizables | Should | COMPLETADO | 1 |

*Nota: PBI-060 (breadcrumbs) movido a Sprint 2 como mejora UX*

---

### EP-11: Frontend - Modulos - EN CURSO
*Sprint 1-3*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-063 | Como usuario, quiero una pagina de login | Must | COMPLETADO | 1 |
| PBI-064 | Como usuario, quiero una pagina de mis cursos | Must | COMPLETADO | 1 |
| PBI-065 | Como usuario, quiero una pagina de detalle de curso | Must | COMPLETADO | 1 |
| PBI-066 | Como usuario, quiero una pagina de actividades (por curso) | Must | COMPLETADO | 1 |
| PBI-067 | Como usuario, quiero una pagina de detalle de actividad | Must | COMPLETADO | 1 |
| PBI-068 | Como usuario, quiero una pagina de entregable con formulario de entrega | Must | EN CURSO (vista OK, formulario Sprint 2) | 1-2 |
| PBI-069 | Como profesor, quiero una pagina de evaluacion de entregas | Must | PENDIENTE | 3 |
| PBI-071 | Como profesor, quiero exportar calificaciones a CSV/Excel | Could | PENDIENTE | 3 |
| PBI-072 | Como usuario, quiero busqueda y filtros en listados | Should | PARCIAL (Dashboard OK, resto Sprint 3) | 1-3 |

*Nota: PBI-070 (calendario fechas limite) movido a Won't Have MVP*

---

### EP-12: Autenticacion - COMPLETADA (MVP)
*Sprint 1*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-073 | Como usuario, quiero iniciar sesion de forma segura (JWT) | Must | COMPLETADO | 1 |
| PBI-074 | Como usuario, quiero cerrar sesion | Must | COMPLETADO | 1 |
| PBI-075 | Como sistema, necesito proteger rutas segun rol | Must | COMPLETADO | 1 |
| PBI-076 | Como sistema, necesito implementar JWT (access + refresh tokens) | Must | COMPLETADO | 1 |
| PBI-081 | Como sistema, necesito validar permisos en cada endpoint | Must | COMPLETADO | 1 |

*Nota: PBI-077 (OAuth Google), PBI-078 (OAuth ULL), PBI-079 (2FA), PBI-080 (TOTP/SMS) movidos a Won't Have MVP*

---

### EP-13: Testing - EN CURSO (distribuido por sprint)
*Sprint 1-4*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-082 | Como equipo, necesito tests unitarios del backend (>70% cobertura) | Must | EN CURSO (18 clases, ampliar cada sprint) | 1-3 |
| PBI-083 | Como equipo, necesito tests de integracion de la API | Must | EN CURSO (parcial, ampliar cada sprint) | 1-3 |
| PBI-084 | Como equipo, necesito tests E2E del frontend | Should | PENDIENTE | 4 |
| PBI-085 | Como equipo, necesito documentar casos de prueba | Should | PENDIENTE | 4 |

*Nota: PBI-086 (pruebas rendimiento) movido a Won't Have MVP*

**Distribucion del testing por sprint:**
- **Sprint 1:** 18 clases de test (controllers x8, services x7, security x3) - COMPLETADO
- **Sprint 2:** Ampliar tests entregables + entregas + integracion
- **Sprint 3:** Tests feedback + calificacion + integracion
- **Sprint 4:** Tests E2E frontend + documentacion pruebas

---

### EP-14: Despliegue y DevOps - COMPLETADA
*Sprint 1*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-087 | Como equipo, necesito dockerizar la aplicacion (multi-stage builds) | Must | COMPLETADO | 1 |
| PBI-088 | Como equipo, necesito configurar CI/CD con GitHub Actions (7 workflows) | Should | COMPLETADO | 1 |
| PBI-089 | Como equipo, necesito desplegar en Render con PostgreSQL Neon | Must | COMPLETADO | 1 |
| PBI-090 | Como equipo, necesito configurar SSL (gestionado por Render) | Should | COMPLETADO | 1 |
| PBI-091 | Como equipo, necesito disponibilidad basica (keep-alive workflow) | Could | COMPLETADO | 1 |

---

### EP-15: Documentacion Final y Memoria TFG - PENDIENTE
*Sprint 3-4*

| ID | Historia de Usuario | Prioridad | Estado | Sprint |
|----|---------------------|-----------|--------|--------|
| PBI-092 | Como usuario, necesito un manual de usuario completo | Must | PENDIENTE | 4 |
| PBI-093 | Como equipo, necesito un manual tecnico/instalacion | Must | PENDIENTE | 4 |
| PBI-094 | Como equipo, necesito completar la memoria del TFG (introduccion, objetivos, metodologia) | Must | PENDIENTE | 3 |
| PBI-095 | Como equipo, necesito documentar el desarrollo y resultados en la memoria | Must | PENDIENTE | 4 |
| PBI-096 | Como equipo, necesito incluir conclusiones y trabajo futuro | Must | PENDIENTE | 4 |
| PBI-097 | Como equipo, necesito revisar y formatear la memoria segun normativa | Must | PENDIENTE | 4 |
| PBI-098 | Como equipo, necesito preparar la presentacion de defensa | Must | PENDIENTE | 4 |
| PBI-099 | Como equipo, necesito ensayar la presentacion | Should | PENDIENTE | 4 |

---

## Items Fuera de Alcance MVP (Trabajo Futuro)

| ID | Historia de Usuario | Prioridad Original | Razon |
|----|---------------------|---------------------|-------|
| PBI-017 | Gestion foto de perfil de usuario | Could | No critico para MVP |
| PBI-044 | Integracion HDVirtual ULL | Must (original) | Complejidad alta, almacenamiento local suficiente para MVP |
| PBI-045 | Integracion OneDrive | Should | Requiere API externa, no esencial para MVP |
| PBI-046 | Subir archivos a nube | Must (original) | Subida local cubre MVP |
| PBI-047 | Descargar archivos desde cloud | Must (original) | Descarga local cubre MVP |
| PBI-048 | Gestionar cuotas almacenamiento | Should | No critico para demo |
| PBI-049 | Sincronizacion automatica con nube | Could | Funcionalidad avanzada |
| PBI-054 | Notificaciones de nuevo feedback | Could | Nice-to-have, no MVP |
| PBI-060 | Breadcrumbs | Could | Reubicado en Sprint 2 como mejora UX |
| PBI-070 | Calendario de fechas limite | Could | Nice-to-have |
| PBI-077 | OAuth2 Google | Should | Complejidad OAuth, JWT cubre MVP |
| PBI-078 | OAuth2 ULL institucional | Should | Requiere coordinacion con IT ULL |
| PBI-079 | Autenticacion 2FA | Should | Seguridad extra no esencial para MVP |
| PBI-080 | Validacion TOTP/SMS | Should | Dependencia de PBI-079 |
| PBI-086 | Pruebas de rendimiento | Could | Nice-to-have |

---

## Mapeo Requisitos Funcionales ERS -> PBIs

| Requisito ERS | Descripcion | PBI | Estado |
|---------------|-------------|-----|--------|
| RQF-001 | Crear usuarios | PBI-012 | COMPLETADO |
| RQF-002 | Ver perfil/listar usuarios | PBI-013, PBI-016 | COMPLETADO |
| RQF-003 | Asignar roles | PBI-014 | COMPLETADO |
| RQF-004 | Actualizar perfil | PBI-015 | COMPLETADO |
| RQF-005 | Crear curso | PBI-018 | COMPLETADO |
| RQF-006 | Listar cursos profesor | PBI-019, PBI-064 | COMPLETADO |
| RQF-007 | Listar cursos estudiante | PBI-020, PBI-064 | COMPLETADO |
| RQF-008 | Editar curso | PBI-021 | COMPLETADO |
| RQF-009 | Crear grupos | PBI-022 | COMPLETADO |
| RQF-010 | Asignar estudiantes a grupos | PBI-023 | COMPLETADO |
| RQF-011 | Crear actividad | PBI-024 | COMPLETADO |
| RQF-012 | Ver actividades (profesor) | PBI-025, PBI-066 | COMPLETADO |
| RQF-013 | Ver actividades (estudiante) | PBI-026, PBI-066 | COMPLETADO |
| RQF-014 | Editar actividad | PBI-027 | COMPLETADO |
| RQF-015 | Cambiar visibilidad | PBI-028 | COMPLETADO |
| RQF-016 | Asignar actividades a grupos | PBI-029 | COMPLETADO |
| RQF-017 | Eliminar actividad | PBI-030 | COMPLETADO |
| RQF-018 | Crear entregable | PBI-031 | EN CURSO (Sprint 2) |
| RQF-019 | Definir fecha limite y tipos archivo | PBI-032 | EN CURSO (Sprint 2) |
| RQF-020 | Ver entregables | PBI-033 | COMPLETADO |
| RQF-021 | Editar entregable | PBI-034 | EN CURSO (Sprint 2) |
| RQF-022 | Configurar reenvios | PBI-035 | EN CURSO (Sprint 2) |
| RQF-023 | Realizar entrega | PBI-036, PBI-068 | EN CURSO (Sprint 2) |
| RQF-024 | Subir archivos | PBI-037 | EN CURSO (Sprint 2) |
| RQF-025 | Ver detalle entrega | PBI-038 | EN CURSO (Sprint 2) |
| RQF-026 | Reenviar entrega | PBI-041 | EN CURSO (Sprint 2) |
| RQF-027 | Calificar entrega | PBI-050, PBI-069 | PENDIENTE (Sprint 3) |
| RQF-028 | Anadir feedback | PBI-051 | PENDIENTE (Sprint 3) |
| RQF-029 | Ver feedback | PBI-052 | PENDIENTE (Sprint 3) |
| RQF-030 | Notificaciones feedback | PBI-054 | FUERA DE ALCANCE MVP |
| RQF-031 | Dashboard por rol | PBI-058 | COMPLETADO |
| RQF-032 | Pagina login | PBI-063 | COMPLETADO |
| RQF-033 | Login seguro | PBI-073 | COMPLETADO |
| RQF-034 | Cerrar sesion | PBI-074 | COMPLETADO |
| RQF-035 | OAuth Google | PBI-077 | FUERA DE ALCANCE MVP |
| RQF-036 | Integracion HDVirtual | PBI-044 | FUERA DE ALCANCE MVP |
| RQF-037 | Integracion OneDrive | PBI-045 | FUERA DE ALCANCE MVP |
| RQF-038 | Subir archivos a nube | PBI-046 | FUERA DE ALCANCE MVP |
| RQF-039 | Descargar archivos de nube | PBI-047 | FUERA DE ALCANCE MVP |
| RQF-040 | OAuth ULL | PBI-078 | FUERA DE ALCANCE MVP |
| RQF-041 | Autenticacion 2FA | PBI-079 | FUERA DE ALCANCE MVP |

---

## Historial de Cambios

| Fecha | Version | Cambios |
|-------|---------|---------|
| Sep 2025 | 1.0 | Creacion inicial del backlog |
| Oct 2025 | 1.1 | Anadidos PBIs de documentacion |
| Nov 2025 | 1.2 | Refinamiento epicas EP-03 a EP-06 |
| Ene 2026 | 1.3 | Anadidos PBIs de frontend |
| Feb 2026 | 2.0 | Reestructuracion completa: 20h/persona/semana, 12 semanas, 6 sprints |
| Feb 2026 | 2.1 | Anadida EP-08 (HDVirtual/OneDrive), actualizada EP-12 (OAuth/2FA) |
| 3 Mar 2026 | 3.0 | Reestructuracion mayor: 30h/persona/semana, 4 sprints de 120h. Sprint 1 completado (infraestructura completa, backend completo, auth, cursos, actividades, CI/CD, deploy, testing). MVP priorizado. EP-08 (Cloud), OAuth/2FA movidos a trabajo futuro. Testing distribuido por sprint. |

---

*Ultima actualizacion: 3 Marzo 2026*
*Product Owner: [Por asignar]*
