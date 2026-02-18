# Product Backlog - Sistema de Gestion de Entregables

## Informacion del Proyecto

| Campo | Valor |
|-------|-------|
| **Proyecto** | Sistema de Gestion de Entregables (TFG) |
| **Product Owner** | [Por asignar] |
| **Horas Totales Estimadas** | 600 horas |
| **Equipo** | 2 personas |
| **Horas Semanales Disponibles** | 40 horas/semana (20h/persona) |
| **Sprint 0** | Sept 2025 - 22 Feb 2026 (~120h) |
| **Sprints 1-6** | 24 Feb - 18 May 2026 (480h) |
| **Duracion Sprint** | 2 semanas (80h/sprint) |

---

## Leyenda de Prioridades

| Prioridad | Descripcion | Criterio |
|-----------|-------------|----------|
| **Must Have** | Imprescindible | Sin esto el sistema no funciona |
| **Should Have** | Importante | Necesario para funcionalidad completa |
| **Could Have** | Deseable | Mejora la experiencia pero no es critico |
| **Won't Have** | Futuro | Fuera del alcance actual |

---

## Resumen por Epicas

| Epica | Items | Story Points | Estado |
|-------|-------|--------------|--------|
| EP-01: Documentacion y Analisis | 6 | 55 | COMPLETADA (Sprint 0) |
| EP-02: Arquitectura y Configuracion | 5 | 35 | PENDIENTE |
| EP-03: Gestion de Usuarios | 6 | 25 | PENDIENTE |
| EP-04: Gestion de Cursos | 6 | 24 | PENDIENTE |
| EP-05: Gestion de Actividades | 7 | 27 | PENDIENTE |
| EP-06: Gestion de Entregables | 5 | 19 | PENDIENTE |
| EP-07: Gestion de Entregas | 8 | 46 | PENDIENTE |
| EP-08: Almacenamiento Cloud (HDVirtual/OneDrive) | 6 | 45 | PENDIENTE |
| EP-09: Feedback y Evaluacion | 5 | 26 | PENDIENTE |
| EP-10: Frontend - UI Base | 8 | 42 | PENDIENTE |
| EP-11: Frontend - Modulos | 10 | 63 | PENDIENTE |
| EP-12: Autenticacion, OAuth y 2FA | 9 | 64 | PENDIENTE |
| EP-13: Testing | 5 | 35 | PENDIENTE |
| EP-14: Despliegue y DevOps | 5 | 30 | PENDIENTE |
| EP-15: Documentacion Final y Memoria TFG | 8 | 80 | PENDIENTE |

**Total Story Points:** 616 SP  
**Velocidad Estimada:** ~100 SP/Sprint (6 sprints de 80h)

---

## Product Backlog Items (PBIs)

### EP-01: Documentacion y Analisis - COMPLETADA
*Sprint 0 - Completada (Sept 2025 - 22 Feb 2026)*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-001 | Como equipo, necesito un documento de requisitos (ERS) para definir el alcance del sistema | Must | 15 | COMPLETADO | - |
| PBI-002 | Como equipo, necesito un documento de arquitectura (DAS) para definir la estructura tecnica | Must | 15 | COMPLETADO | - |
| PBI-003 | Como equipo, necesito definir los casos de uso del sistema | Must | 10 | COMPLETADO | CU-001 a CU-025 |
| PBI-004 | Como equipo, necesito especificar los requisitos funcionales | Must | 5 | COMPLETADO | RQF-001 a RQF-041 |
| PBI-005 | Como equipo, necesito especificar los requisitos no funcionales | Should | 5 | COMPLETADO | RQI-001 a RQI-015 |
| PBI-006 | Como equipo, necesito crear el modelo de datos del sistema | Must | 5 | COMPLETADO | ENT-001 a ENT-012 |

**Total Sprint 0:** 55 SP (~120 horas)

---

### EP-02: Arquitectura y Configuracion - PENDIENTE
*Sprint 1*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-007 | Como desarrollador, necesito configurar el proyecto backend Spring Boot | Must | 5 | PENDIENTE | RQI-001 |
| PBI-008 | Como desarrollador, necesito configurar el proyecto frontend React | Must | 5 | PENDIENTE | RQI-001 |
| PBI-009 | Como desarrollador, necesito configurar la base de datos H2/MySQL | Must | 5 | PENDIENTE | RQI-003 |
| PBI-010 | Como desarrollador, necesito implementar las entidades JPA | Must | 13 | PENDIENTE | ENT-001 a ENT-012 |
| PBI-011 | Como desarrollador, necesito crear los repositorios Spring Data | Must | 7 | PENDIENTE | - |

---

### EP-03: Gestion de Usuarios - PENDIENTE
*Sprint 1-2*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-012 | Como administrador, quiero crear usuarios en el sistema | Must | 5 | PENDIENTE | RQF-001, SYSOP-001 |
| PBI-013 | Como usuario, quiero ver mi perfil con mis datos | Must | 3 | PENDIENTE | RQF-002, SYSOP-002 |
| PBI-014 | Como administrador, quiero asignar roles a los usuarios | Must | 5 | PENDIENTE | RQF-003 |
| PBI-015 | Como usuario, quiero actualizar mi informacion de perfil | Should | 5 | PENDIENTE | RQF-004 |
| PBI-016 | Como administrador, quiero listar todos los usuarios | Should | 3 | PENDIENTE | RQF-002 |
| PBI-017 | Como sistema, necesito gestionar la foto de perfil del usuario | Could | 4 | PENDIENTE | - |

---

### EP-04: Gestion de Cursos - PENDIENTE
*Sprint 2*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-018 | Como profesor, quiero crear un nuevo curso | Must | 5 | PENDIENTE | RQF-005, SYSOP-003 |
| PBI-019 | Como profesor, quiero ver la lista de mis cursos | Must | 3 | PENDIENTE | RQF-006, SYSOP-004 |
| PBI-020 | Como estudiante, quiero ver los cursos en los que estoy matriculado | Must | 3 | PENDIENTE | RQF-007, SYSOP-005 |
| PBI-021 | Como profesor, quiero editar la informacion de un curso | Should | 3 | PENDIENTE | RQF-008 |
| PBI-022 | Como profesor, quiero crear grupos dentro de un curso | Must | 5 | PENDIENTE | RQF-009 |
| PBI-023 | Como profesor, quiero asignar estudiantes a grupos | Must | 5 | PENDIENTE | RQF-010 |

---

### EP-05: Gestion de Actividades - PENDIENTE
*Sprint 2-3*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-024 | Como profesor, quiero crear una actividad en un curso | Must | 5 | PENDIENTE | RQF-011, SYSOP-006 |
| PBI-025 | Como profesor, quiero ver todas las actividades de un curso | Must | 3 | PENDIENTE | RQF-012, SYSOP-007 |
| PBI-026 | Como estudiante, quiero ver las actividades visibles de mi grupo | Must | 5 | PENDIENTE | RQF-013, SYSOP-008 |
| PBI-027 | Como profesor, quiero editar una actividad existente | Should | 3 | PENDIENTE | RQF-014 |
| PBI-028 | Como profesor, quiero cambiar la visibilidad de una actividad | Must | 3 | PENDIENTE | RQF-015, SYSOP-009 |
| PBI-029 | Como profesor, quiero asignar actividades a grupos especificos | Should | 5 | PENDIENTE | RQF-016 |
| PBI-030 | Como profesor, quiero eliminar una actividad | Should | 3 | PENDIENTE | RQF-017, SYSOP-010 |

---

### EP-06: Gestion de Entregables - PENDIENTE
*Sprint 3*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-031 | Como profesor, quiero crear entregables dentro de una actividad | Must | 5 | PENDIENTE | RQF-018, SYSOP-011 |
| PBI-032 | Como profesor, quiero definir fecha limite y tipos de archivo permitidos | Must | 5 | PENDIENTE | RQF-019 |
| PBI-033 | Como estudiante, quiero ver los entregables de una actividad | Must | 3 | PENDIENTE | RQF-020, SYSOP-012 |
| PBI-034 | Como profesor, quiero editar un entregable | Should | 3 | PENDIENTE | RQF-021 |
| PBI-035 | Como profesor, quiero configurar si un entregable permite reenvios | Should | 3 | PENDIENTE | RQF-022, SYSOP-013 |

---

### EP-07: Gestion de Entregas - PENDIENTE
*Sprint 3-4*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-036 | Como estudiante, quiero realizar una entrega de un entregable | Must | 8 | PENDIENTE | RQF-023, SYSOP-014 |
| PBI-037 | Como estudiante, quiero subir archivos en mi entrega | Must | 8 | PENDIENTE | RQF-024, RQI-002 |
| PBI-038 | Como estudiante, quiero ver el detalle de mis entregas | Must | 5 | PENDIENTE | RQF-025, SYSOP-015 |
| PBI-039 | Como profesor, quiero ver las entregas de un entregable | Must | 5 | PENDIENTE | SYSOP-016 |
| PBI-040 | Como estudiante, quiero ver el historial de versiones de mis entregas | Should | 5 | PENDIENTE | SYSOP-017 |
| PBI-041 | Como estudiante, quiero reenviar una entrega si esta permitido | Should | 5 | PENDIENTE | RQF-026 |
| PBI-042 | Como profesor, quiero descargar los archivos de una entrega | Must | 5 | PENDIENTE | SYSOP-018 |
| PBI-043 | Como profesor, quiero ver estadisticas de entregas | Could | 5 | PENDIENTE | - |

---

### EP-08: Almacenamiento Cloud (HDVirtual/OneDrive) - PENDIENTE (NUEVO)
*Sprint 4*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-044 | Como sistema, necesito integracion con HDVirtual de la ULL para almacenamiento | Must | 10 | PENDIENTE | RQF-036 |
| PBI-045 | Como sistema, necesito integracion con OneDrive como alternativa de almacenamiento | Should | 10 | PENDIENTE | RQF-037 |
| PBI-046 | Como estudiante, quiero subir archivos directamente a la nube | Must | 8 | PENDIENTE | RQF-038 |
| PBI-047 | Como profesor, quiero descargar archivos desde el almacenamiento cloud | Must | 5 | PENDIENTE | RQF-039 |
| PBI-048 | Como sistema, necesito gestionar cuotas de almacenamiento por usuario | Should | 5 | PENDIENTE | RQI-011 |
| PBI-049 | Como sistema, necesito sincronizacion automatica de archivos con la nube | Could | 7 | PENDIENTE | - |

---

### EP-09: Feedback y Evaluacion - PENDIENTE
*Sprint 4-5*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-050 | Como profesor, quiero calificar una entrega | Must | 5 | PENDIENTE | RQF-027, SYSOP-019 |
| PBI-051 | Como profesor, quiero anadir feedback/comentarios a una entrega | Must | 5 | PENDIENTE | RQF-028, SYSOP-020 |
| PBI-052 | Como estudiante, quiero ver el feedback de mis entregas | Must | 5 | PENDIENTE | RQF-029, SYSOP-021 |
| PBI-053 | Como profesor, quiero modificar un feedback existente | Should | 3 | PENDIENTE | SYSOP-022 |
| PBI-054 | Como estudiante, quiero recibir notificaciones de nuevo feedback | Could | 8 | PENDIENTE | RQF-030 |

---

### EP-10: Frontend - UI Base - PENDIENTE
*Sprint 1-2*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-055 | Como usuario, quiero un diseno responsive que funcione en movil y desktop | Must | 8 | PENDIENTE | RQI-004 |
| PBI-056 | Como usuario, quiero una navegacion clara y consistente | Must | 5 | PENDIENTE | RQI-005 |
| PBI-057 | Como usuario, quiero ver mensajes de error claros | Must | 3 | PENDIENTE | RQI-006 |
| PBI-058 | Como usuario, quiero un dashboard personalizado segun mi rol | Must | 8 | PENDIENTE | RQF-031 |
| PBI-059 | Como usuario, quiero indicadores de carga mientras se procesan datos | Should | 3 | PENDIENTE | RQI-007 |
| PBI-060 | Como usuario, quiero breadcrumbs para saber donde estoy | Could | 2 | PENDIENTE | - |
| PBI-061 | Como usuario, quiero modo oscuro/claro | Won't | 5 | PENDIENTE | - |
| PBI-062 | Como desarrollador, necesito componentes reutilizables | Should | 8 | PENDIENTE | - |

---

### EP-11: Frontend - Modulos - PENDIENTE
*Sprint 2-4*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-063 | Como usuario, quiero una pagina de login | Must | 5 | PENDIENTE | RQF-032 |
| PBI-064 | Como usuario, quiero una pagina de mis cursos | Must | 8 | PENDIENTE | RQF-006, RQF-007 |
| PBI-065 | Como usuario, quiero una pagina de detalle de curso | Must | 8 | PENDIENTE | - |
| PBI-066 | Como usuario, quiero una pagina de actividades | Must | 8 | PENDIENTE | RQF-012, RQF-013 |
| PBI-067 | Como usuario, quiero una pagina de detalle de actividad | Must | 5 | PENDIENTE | - |
| PBI-068 | Como usuario, quiero una pagina de entregable con formulario de entrega | Must | 8 | PENDIENTE | RQF-023 |
| PBI-069 | Como profesor, quiero una pagina de evaluacion de entregas | Must | 8 | PENDIENTE | RQF-027 |
| PBI-070 | Como usuario, quiero un calendario de fechas limite | Could | 5 | PENDIENTE | - |
| PBI-071 | Como profesor, quiero exportar calificaciones a CSV/Excel | Could | 3 | PENDIENTE | - |
| PBI-072 | Como usuario, quiero busqueda y filtros en listados | Should | 5 | PENDIENTE | - |

---

### EP-12: Autenticacion, OAuth y 2FA - PENDIENTE (ACTUALIZADO)
*Sprint 1, 4-5*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-073 | Como usuario, quiero iniciar sesion de forma segura | Must | 8 | PENDIENTE | RQF-033, RQI-008 |
| PBI-074 | Como usuario, quiero cerrar sesion | Must | 2 | PENDIENTE | RQF-034 |
| PBI-075 | Como sistema, necesito proteger rutas segun rol | Must | 8 | PENDIENTE | RQI-009 |
| PBI-076 | Como sistema, necesito implementar JWT para autenticacion | Must | 8 | PENDIENTE | RQI-008 |
| PBI-077 | Como usuario, quiero iniciar sesion con Google (OAuth2) | Should | 10 | PENDIENTE | RQF-035, RQI-012 |
| PBI-078 | Como usuario, quiero iniciar sesion con cuenta ULL (OAuth2 institucional) | Should | 10 | PENDIENTE | RQF-040, RQI-013 |
| PBI-079 | Como usuario, quiero activar autenticacion de doble factor (2FA) | Should | 8 | PENDIENTE | RQF-041, RQI-014 |
| PBI-080 | Como sistema, necesito validar 2FA mediante codigo TOTP o SMS | Should | 5 | PENDIENTE | RQI-014 |
| PBI-081 | Como sistema, necesito validar permisos en cada endpoint | Must | 5 | PENDIENTE | RQI-009 |

---

### EP-13: Testing - PENDIENTE
*Sprint 5*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-082 | Como equipo, necesito tests unitarios del backend (>70% cobertura) | Must | 13 | PENDIENTE | RQI-010 |
| PBI-083 | Como equipo, necesito tests de integracion de la API | Must | 8 | PENDIENTE | RQI-010 |
| PBI-084 | Como equipo, necesito tests E2E del frontend | Should | 8 | PENDIENTE | RQI-010 |
| PBI-085 | Como equipo, necesito documentar casos de prueba | Should | 3 | PENDIENTE | - |
| PBI-086 | Como equipo, necesito pruebas de rendimiento basicas | Could | 3 | PENDIENTE | - |

---

### EP-14: Despliegue y DevOps - PENDIENTE
*Sprint 5-6*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-087 | Como equipo, necesito dockerizar la aplicacion | Must | 8 | PENDIENTE | - |
| PBI-088 | Como equipo, necesito configurar CI/CD con GitHub Actions | Should | 8 | PENDIENTE | - |
| PBI-089 | Como equipo, necesito desplegar en servidor de produccion | Must | 8 | PENDIENTE | - |
| PBI-090 | Como equipo, necesito configurar dominio y SSL | Should | 3 | PENDIENTE | RQI-008 |
| PBI-091 | Como equipo, necesito monitorizacion basica | Could | 3 | PENDIENTE | - |

---

### EP-15: Documentacion Final y Memoria TFG - PENDIENTE
*Sprint 5-6*

| ID | Historia de Usuario | Prioridad | SP | Estado | Requisito ERS |
|----|---------------------|-----------|----|---------|----|
| PBI-092 | Como usuario, necesito un manual de usuario completo | Must | 10 | PENDIENTE | - |
| PBI-093 | Como equipo, necesito un manual tecnico/instalacion | Must | 10 | PENDIENTE | - |
| PBI-094 | Como equipo, necesito completar la memoria del TFG (introduccion, objetivos, metodologia) | Must | 15 | PENDIENTE | - |
| PBI-095 | Como equipo, necesito documentar el desarrollo y resultados en la memoria | Must | 15 | PENDIENTE | - |
| PBI-096 | Como equipo, necesito incluir conclusiones y trabajo futuro | Must | 8 | PENDIENTE | - |
| PBI-097 | Como equipo, necesito revisar y formatear la memoria segun normativa | Must | 10 | PENDIENTE | - |
| PBI-098 | Como equipo, necesito preparar la presentacion de defensa | Must | 8 | PENDIENTE | - |
| PBI-099 | Como equipo, necesito ensayar la presentacion | Should | 4 | PENDIENTE | - |

---

## Mapeo Requisitos Funcionales ERS -> PBIs

| Requisito ERS | Descripcion | PBI Relacionados |
|---------------|-------------|------------------|
| RQF-001 | Crear usuarios | PBI-012 |
| RQF-002 | Ver perfil/listar usuarios | PBI-013, PBI-016 |
| RQF-003 | Asignar roles | PBI-014 |
| RQF-004 | Actualizar perfil | PBI-015 |
| RQF-005 | Crear curso | PBI-018 |
| RQF-006 | Listar cursos profesor | PBI-019, PBI-064 |
| RQF-007 | Listar cursos estudiante | PBI-020, PBI-064 |
| RQF-008 | Editar curso | PBI-021 |
| RQF-009 | Crear grupos | PBI-022 |
| RQF-010 | Asignar estudiantes a grupos | PBI-023 |
| RQF-011 | Crear actividad | PBI-024 |
| RQF-012 | Ver actividades (profesor) | PBI-025, PBI-066 |
| RQF-013 | Ver actividades (estudiante) | PBI-026, PBI-066 |
| RQF-014 | Editar actividad | PBI-027 |
| RQF-015 | Cambiar visibilidad | PBI-028 |
| RQF-016 | Asignar actividades a grupos | PBI-029 |
| RQF-017 | Eliminar actividad | PBI-030 |
| RQF-018 | Crear entregable | PBI-031 |
| RQF-019 | Definir fecha limite y tipos archivo | PBI-032 |
| RQF-020 | Ver entregables | PBI-033 |
| RQF-021 | Editar entregable | PBI-034 |
| RQF-022 | Configurar reenvios | PBI-035 |
| RQF-023 | Realizar entrega | PBI-036, PBI-068 |
| RQF-024 | Subir archivos | PBI-037 |
| RQF-025 | Ver detalle entrega | PBI-038 |
| RQF-026 | Reenviar entrega | PBI-041 |
| RQF-027 | Calificar entrega | PBI-050, PBI-069 |
| RQF-028 | Anadir feedback | PBI-051 |
| RQF-029 | Ver feedback | PBI-052 |
| RQF-030 | Notificaciones feedback | PBI-054 |
| RQF-031 | Dashboard por rol | PBI-058 |
| RQF-032 | Pagina login | PBI-063 |
| RQF-033 | Login seguro | PBI-073 |
| RQF-034 | Cerrar sesion | PBI-074 |
| RQF-035 | OAuth Google | PBI-077 |
| RQF-036 | Integracion HDVirtual | PBI-044 |
| RQF-037 | Integracion OneDrive | PBI-045 |
| RQF-038 | Subir archivos a nube | PBI-046 |
| RQF-039 | Descargar archivos de nube | PBI-047 |
| RQF-040 | OAuth ULL | PBI-078 |
| RQF-041 | Autenticacion 2FA | PBI-079 |

---

## Mapeo Operaciones Sistema (SYSOP) -> PBIs

| SYSOP | Descripcion | PBI | Estado |
|-------|-------------|-----|--------|
| SYSOP-001 | Crear usuario | PBI-012 | PENDIENTE |
| SYSOP-002 | Obtener usuario | PBI-013 | PENDIENTE |
| SYSOP-003 | Crear curso | PBI-018 | PENDIENTE |
| SYSOP-004 | Listar cursos profesor | PBI-019 | PENDIENTE |
| SYSOP-005 | Listar cursos estudiante | PBI-020 | PENDIENTE |
| SYSOP-006 | Crear actividad | PBI-024 | PENDIENTE |
| SYSOP-007 | Listar actividades curso | PBI-025 | PENDIENTE |
| SYSOP-008 | Listar actividades grupo | PBI-026 | PENDIENTE |
| SYSOP-009 | Cambiar visibilidad | PBI-028 | PENDIENTE |
| SYSOP-010 | Eliminar actividad | PBI-030 | PENDIENTE |
| SYSOP-011 | Crear entregable | PBI-031 | PENDIENTE |
| SYSOP-012 | Ver entregables | PBI-033 | PENDIENTE |
| SYSOP-013 | Configurar reenvio | PBI-035 | PENDIENTE |
| SYSOP-014 | Realizar entrega | PBI-036 | PENDIENTE |
| SYSOP-015 | Ver detalle entrega | PBI-038 | PENDIENTE |
| SYSOP-016 | Listar entregas (profesor) | PBI-039 | PENDIENTE |
| SYSOP-017 | Historial versiones | PBI-040 | PENDIENTE |
| SYSOP-018 | Descargar archivos | PBI-042 | PENDIENTE |
| SYSOP-019 | Calificar entrega | PBI-050 | PENDIENTE |
| SYSOP-020 | Anadir feedback | PBI-051 | PENDIENTE |
| SYSOP-021 | Listar feedbacks | PBI-052 | PENDIENTE |
| SYSOP-022 | Modificar feedback | PBI-053 | PENDIENTE |
| SYSOP-023 | Subir archivo a HDVirtual | PBI-044 | PENDIENTE |
| SYSOP-024 | Subir archivo a OneDrive | PBI-045 | PENDIENTE |
| SYSOP-025 | Autenticar OAuth | PBI-077, PBI-078 | PENDIENTE |
| SYSOP-026 | Validar 2FA | PBI-079 | PENDIENTE |

---

## Historial de Cambios

| Fecha | Version | Cambios |
|-------|---------|---------|
| Sep 2025 | 1.0 | Creacion inicial del backlog |
| Oct 2025 | 1.1 | Anadidos PBIs de documentacion |
| Nov 2025 | 1.2 | Refinamiento epicas EP-03 a EP-06 |
| Ene 2026 | 1.3 | Anadidos PBIs de frontend |
| Feb 2026 | 2.0 | Reestructuracion completa: 20h/persona/semana, 12 semanas, 6 sprints |
| Feb 2026 | 2.1 | Anadida EP-08 (HDVirtual/OneDrive), actualizada EP-12 (OAuth/2FA), ampliada EP-15 (Memoria 80 SP) |

---

*Ultima actualizacion: 18 Febrero 2026*  
*Product Owner: [Por asignar]*
