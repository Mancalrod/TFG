# Plan de Sprints - Sistema de Gestion de Entregables

## Informacion del Proyecto

| Campo | Valor |
|-------|-------|
| **Proyecto** | Sistema de Gestion de Entregables (TFG) |
| **Horas Totales** | 600 horas |
| **Equipo** | 2 personas |
| **Horas por Persona/Semana** | 20 horas |
| **Horas por Semana (total)** | 40 horas |
| **Duracion Sprint** | 2 semanas |
| **Horas por Sprint** | 80 horas (40h/persona) |
| **Total Sprints** | 6 sprints (+ Sprint 0) |
| **Inicio Sprint 0** | Septiembre 2025 |
| **Fin Sprint 0** | 22 Febrero 2026 |
| **Inicio Sprints 1-6** | 24 Febrero 2026 |
| **Fecha Fin Estimada** | 18 Mayo 2026 |

---

## Resumen de Sprints

| Sprint | Fechas | Horas | Objetivo Principal | Estado |
|--------|--------|-------|---------------------|--------|
| Sprint 0 | Sep 2025 - 22 Feb 2026 | ~120h | Documentacion: ERS y DAS | COMPLETADO |
| Sprint 1 | 24 Feb - 9 Mar 2026 | 80h | Backend base + Autenticacion basica | PENDIENTE |
| Sprint 2 | 10 Mar - 23 Mar 2026 | 80h | Gestion usuarios, cursos + Frontend base | PENDIENTE |
| Sprint 3 | 24 Mar - 6 Abr 2026 | 80h | Actividades, entregables, entregas | PENDIENTE |
| Sprint 4 | 7 Abr - 20 Abr 2026 | 80h | Almacenamiento cloud + Feedback | PENDIENTE |
| Sprint 5 | 21 Abr - 4 May 2026 | 80h | OAuth, 2FA + Testing | PENDIENTE |
| Sprint 6 | 5 May - 18 May 2026 | 80h | Despliegue + Memoria TFG | PENDIENTE |

**Total horas:** 120h (Sprint 0) + 480h (Sprints 1-6) = **600 horas**

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

## Sprint 1: Backend Base + Autenticacion - PENDIENTE
**Estado:** PENDIENTE  
**Fechas:** 24 Febrero - 9 Marzo 2026  
**Horas Planificadas:** 80h (40h/persona)

### Objetivos
- Configurar proyecto backend Spring Boot
- Implementar entidades JPA y repositorios
- Configurar base de datos
- Implementar autenticacion basica JWT
- Iniciar estructura frontend

### Epicas/PBIs Incluidos
- EP-02: Arquitectura y Configuracion (PBI-007 a PBI-011)
- EP-03 parcial: Gestion de Usuarios (PBI-012 a PBI-014)
- EP-12 parcial: Autenticacion JWT (PBI-073, PBI-074, PBI-076)
- EP-10 parcial: Estructura frontend basica

### Desglose de Horas (Estimado)
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| Configuracion Spring Boot + React | 4h | 4h | 8h |
| Entidades JPA (5 cada uno) | 8h | 8h | 16h |
| Repositorios Spring Data | 4h | 4h | 8h |
| DTOs y EntityMapper | 5h | 5h | 10h |
| Configuracion BD H2/MySQL | 3h | 3h | 6h |
| Autenticacion JWT basica | 8h | 8h | 16h |
| Estructura React + routing | 4h | 4h | 8h |
| Servicios Usuario | 4h | 4h | 8h |
| **Total** | **40h** | **40h** | **80h** |

---

## Sprint 2: Usuarios, Cursos + Frontend Base - PENDIENTE
**Estado:** PENDIENTE  
**Fechas:** 10 Marzo - 23 Marzo 2026  
**Horas Planificadas:** 80h (40h/persona)

### Objetivos
- Completar servicios y controladores de usuarios
- Implementar gestion completa de cursos y grupos
- Desarrollar componentes UI base
- Implementar dashboard por rol

### Epicas/PBIs Incluidos
- EP-03 completa: Gestion de Usuarios (PBI-015 a PBI-017)
- EP-04: Gestion de Cursos (PBI-018 a PBI-023)
- EP-10: Frontend UI Base (PBI-055 a PBI-062)
- EP-11 parcial: Pagina login y cursos

### Desglose de Horas (Estimado)
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| Servicios y controladores usuarios | 4h | 4h | 8h |
| Servicios cursos y grupos | 6h | 6h | 12h |
| Controladores cursos | 4h | 4h | 8h |
| Layout principal (Navbar, Sidebar) | 6h | - | 6h |
| Componentes reutilizables | - | 8h | 8h |
| Pagina Login | 4h | 4h | 8h |
| Dashboard por rol | 6h | 6h | 12h |
| Pagina Cursos | 5h | 5h | 10h |
| Estilos responsive | 5h | 3h | 8h |
| **Total** | **40h** | **40h** | **80h** |

---

## Sprint 3: Actividades, Entregables, Entregas - PENDIENTE
**Estado:** PENDIENTE  
**Fechas:** 24 Marzo - 6 Abril 2026  
**Horas Planificadas:** 80h (40h/persona)

### Objetivos
- Implementar gestion de actividades
- Implementar gestion de entregables
- Implementar gestion de entregas
- Desarrollar frontend de actividades y entregas

### Epicas/PBIs Incluidos
- EP-05: Gestion de Actividades (PBI-024 a PBI-030)
- EP-06: Gestion de Entregables (PBI-031 a PBI-035)
- EP-07: Gestion de Entregas (PBI-036 a PBI-043)
- EP-11 parcial: Paginas actividades, entregables, entregas

### Desglose de Horas (Estimado)
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| Servicios actividades | 5h | 5h | 10h |
| Controladores actividades | 3h | 3h | 6h |
| Servicios entregables | 4h | 4h | 8h |
| Controladores entregables | 2h | 2h | 4h |
| Servicios entregas | 6h | 6h | 12h |
| Controladores entregas | 3h | 3h | 6h |
| Frontend: Pagina actividades | 5h | 5h | 10h |
| Frontend: Pagina entregable | 5h | 5h | 10h |
| Frontend: Formulario entrega | 4h | 4h | 8h |
| Pruebas integracion | 3h | 3h | 6h |
| **Total** | **40h** | **40h** | **80h** |

---

## Sprint 4: Almacenamiento Cloud + Feedback - PENDIENTE
**Estado:** PENDIENTE  
**Fechas:** 7 Abril - 20 Abril 2026  
**Horas Planificadas:** 80h (40h/persona)

### Objetivos
- Integrar HDVirtual ULL para almacenamiento
- Integrar OneDrive como alternativa
- Implementar sistema de feedback y calificaciones
- Optimizar subida/descarga de archivos

### Epicas/PBIs Incluidos
- EP-08: Almacenamiento Cloud (PBI-044 a PBI-049)
- EP-09: Feedback y Evaluacion (PBI-050 a PBI-054)
- EP-11 parcial: Pagina evaluacion entregas

### Desglose de Horas (Estimado)
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| Integracion API HDVirtual | 8h | 8h | 16h |
| Integracion API OneDrive | 6h | 6h | 12h |
| Servicio almacenamiento unificado | 4h | 4h | 8h |
| Gestion cuotas y sincronizacion | 4h | 4h | 8h |
| Servicios feedback | 4h | 4h | 8h |
| Controladores feedback | 2h | 2h | 4h |
| Frontend: Subida archivos cloud | 4h | 4h | 8h |
| Frontend: Pagina evaluacion | 4h | 4h | 8h |
| Pruebas almacenamiento | 4h | 4h | 8h |
| **Total** | **40h** | **40h** | **80h** |

---

## Sprint 5: OAuth, 2FA + Testing - PENDIENTE
**Estado:** PENDIENTE  
**Fechas:** 21 Abril - 4 Mayo 2026  
**Horas Planificadas:** 80h (40h/persona)

### Objetivos
- Implementar OAuth2 con Google
- Implementar OAuth2 con cuenta ULL institucional
- Implementar autenticacion de doble factor (2FA)
- Testing completo del sistema

### Epicas/PBIs Incluidos
- EP-12 completa: OAuth y 2FA (PBI-077 a PBI-081)
- EP-13: Testing (PBI-082 a PBI-086)

### Desglose de Horas (Estimado)
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| OAuth2 Google | 6h | 6h | 12h |
| OAuth2 ULL institucional | 6h | 6h | 12h |
| Implementacion 2FA (TOTP) | 5h | 5h | 10h |
| UI configuracion 2FA | 3h | 3h | 6h |
| Tests unitarios backend | 8h | 8h | 16h |
| Tests integracion API | 5h | 5h | 10h |
| Tests E2E frontend | 4h | 4h | 8h |
| Documentacion pruebas | 3h | 3h | 6h |
| **Total** | **40h** | **40h** | **80h** |

---

## Sprint 6: Despliegue + Memoria TFG - PENDIENTE
**Estado:** PENDIENTE  
**Fechas:** 5 Mayo - 18 Mayo 2026  
**Horas Planificadas:** 80h (40h/persona)

### Objetivos
- Dockerizar aplicacion
- Configurar CI/CD
- Desplegar en produccion
- Completar memoria TFG
- Preparar presentacion defensa

### Epicas/PBIs Incluidos
- EP-14: Despliegue y DevOps (PBI-087 a PBI-091)
- EP-15: Documentacion Final y Memoria (PBI-092 a PBI-099)

### Desglose de Horas (Estimado)
| Tarea | Manuel Maria Calderon Rodriguez | Jose Manuel Marquez Gutierrez | Total |
|-------|-----------|-----------|-------|
| Dockerfiles y docker-compose | 4h | 4h | 8h |
| CI/CD GitHub Actions | 4h | 4h | 8h |
| Despliegue produccion | 4h | 4h | 8h |
| SSL y dominio | 2h | 2h | 4h |
| Manual de usuario | 4h | 4h | 8h |
| Manual tecnico | 4h | 4h | 8h |
| Memoria: Intro, objetivos, metodologia | 6h | 6h | 12h |
| Memoria: Desarrollo y resultados | 6h | 6h | 12h |
| Memoria: Conclusiones y formato | 4h | 4h | 8h |
| Presentacion defensa | 2h | 2h | 4h |
| **Total** | **40h** | **40h** | **80h** |

---

## Calendario Visual

```
2026
         FEBRERO                    MARZO                     ABRIL                      MAYO
    L  M  X  J  V  S  D       L  M  X  J  V  S  D       L  M  X  J  V  S  D       L  M  X  J  V  S  D
                         1                         1          1  2  3  4  5                   1  2  3
    2  3  4  5  6  7  8       2  3  4  5  6  7  8       6  7  8  9 10 11 12       4  5  6  7  8  9 10
    9 10 11 12 13 14 15       9 10 11 12 13 14 15      13 14 15 16 17 18 19      11 12 13 14 15 16 17
   16 17 18 19 20 21 22      16 17 18 19 20 21 22      20 21 22 23 24 25 26      18 19 20 21 22 23 24
   [Sprint 0 fin: 22]        23 24 25 26 27 28 29      27 28 29 30               25 26 27 28 29 30 31
   [Sprint 1: 24-9 Mar]      30 31
                             [Sprint 2: 10-23]
                             [Sprint 3: 24-6 Abr]
                                                       [Sprint 4: 7-20]
                                                       [Sprint 5: 21-4 May]
                                                                                 [Sprint 6: 5-18]
                                                                                 [ENTREGA TFG: 18]
```

---

## Metricas de Seguimiento

### Velocidad Planificada
- **Sprint 0:** 55 SP (completado)
- **Sprints 1-6:** ~100 SP/sprint estimado
- **Total:** 616 SP

### Indicadores de Progreso
| Metrica | Sprint 0 | Sprint 1 | Sprint 2 | Sprint 3 | Sprint 4 | Sprint 5 | Sprint 6 |
|---------|----------|----------|----------|----------|----------|----------|----------|
| SP Planificados | 55 | 100 | 95 | 105 | 100 | 100 | 116 |
| SP Completados | 55 | - | - | - | - | - | - |
| % Completado | 100% | - | - | - | - | - | - |
| Horas Usadas | 120h | - | - | - | - | - | - |

---

## Riesgos Identificados

| Riesgo | Probabilidad | Impacto | Mitigacion |
|--------|--------------|---------|------------|
| Complejidad OAuth ULL | Media | Alto | Contactar con soporte IT ULL temprano |
| Integracion HDVirtual | Media | Alto | Tener OneDrive como backup |
| Tiempo memoria TFG | Alta | Medio | Documentar durante desarrollo |
| Disponibilidad 20h/persona | Media | Alto | Buffer de tiempo en Sprint 6 |

---

## Historial de Cambios

| Fecha | Version | Cambios |
|-------|---------|---------|
| Feb 2026 | 1.0 | Creacion inicial |
| 18 Feb 2026 | 2.0 | Reestructuracion: 20h/persona/semana, 6 sprints de 80h, anadidos OAuth/2FA/HDVirtual |

---

*Ultima actualizacion: 18 Febrero 2026*
