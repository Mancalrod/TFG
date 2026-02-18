# Sprint Backlog - Sprint 1

## Informacion del Sprint

| Campo | Valor |
|-------|-------|
| **Sprint** | 1 |
| **Objetivo** | Backend base + Autenticacion JWT |
| **Fecha Inicio** | 24 Febrero 2026 |
| **Fecha Fin** | 9 Marzo 2026 |
| **Duracion** | 2 semanas |
| **Horas Planificadas** | 80 horas (40h/persona) |
| **Horas Completadas** | 0 horas |
| **Estado** | PENDIENTE |

---

## Equipo

| Miembro | Rol | Horas Disponibles | Horas Asignadas |
|---------|-----|-------------------|-----------------|
| Manuel Maria Calderon Rodriguez | Desarrollador Full-Stack | 40h | 40h |
| Jose Manuel Marquez Gutierrez | Desarrollador Full-Stack | 40h | 40h |

---

## Objetivo del Sprint

> Establecer las bases del proyecto backend con Spring Boot, implementar las entidades JPA y repositorios necesarios, configurar la autenticacion JWT basica, e iniciar la estructura del frontend React. Al finalizar el sprint, el sistema debe permitir login/logout basico.

---

## Product Backlog Items (PBIs) del Sprint

### PBI-007: Configuracion Spring Boot
**Prioridad:** Alta | **Estimacion:** 5h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-1.1 | Crear proyecto Maven con dependencias | Manuel Maria Calderon Rodriguez | 1.5h | PENDIENTE | - |
| T-1.2 | Configurar application.properties | Manuel Maria Calderon Rodriguez | 1h | PENDIENTE | - |
| T-1.3 | Configurar perfiles dev/prod | Manuel Maria Calderon Rodriguez | 1h | PENDIENTE | - |
| T-1.4 | Crear estructura de paquetes | Manuel Maria Calderon Rodriguez | 1.5h | PENDIENTE | - |

---

### PBI-008: Configuracion React + TypeScript
**Prioridad:** Alta | **Estimacion:** 5h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-2.1 | Crear proyecto Vite + React + TS | Jose Manuel Marquez Gutierrez | 1.5h | PENDIENTE | - |
| T-2.2 | Configurar ESLint y Prettier | Jose Manuel Marquez Gutierrez | 1h | PENDIENTE | - |
| T-2.3 | Configurar routing React Router | Jose Manuel Marquez Gutierrez | 1.5h | PENDIENTE | - |
| T-2.4 | Configurar axios y servicios base | Jose Manuel Marquez Gutierrez | 1h | PENDIENTE | - |

---

### PBI-009: Configuracion Base de Datos
**Prioridad:** Alta | **Estimacion:** 5h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-3.1 | Configurar H2 para desarrollo | Manuel Maria Calderon Rodriguez | 1.5h | PENDIENTE | - |
| T-3.2 | Configurar MySQL para produccion | Manuel Maria Calderon Rodriguez | 1.5h | PENDIENTE | - |
| T-3.3 | Crear scripts de inicializacion | Jose Manuel Marquez Gutierrez | 2h | PENDIENTE | - |

---

### PBI-010: Entidades JPA
**Prioridad:** Alta | **Estimacion:** 13h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-4.1 | Crear entidad Usuario | Manuel Maria Calderon Rodriguez | 1.5h | PENDIENTE | - |
| T-4.2 | Crear entidades Profesor y Estudiante | Manuel Maria Calderon Rodriguez | 2h | PENDIENTE | - |
| T-4.3 | Crear entidad Curso | Manuel Maria Calderon Rodriguez | 1.5h | PENDIENTE | - |
| T-4.4 | Crear entidad Grupo | Jose Manuel Marquez Gutierrez | 1.5h | PENDIENTE | - |
| T-4.5 | Crear entidad Actividad | Jose Manuel Marquez Gutierrez | 2h | PENDIENTE | - |
| T-4.6 | Crear entidad Entregable | Jose Manuel Marquez Gutierrez | 1.5h | PENDIENTE | - |
| T-4.7 | Crear entidades Entrega y Material | Manuel Maria Calderon Rodriguez | 2h | PENDIENTE | - |
| T-4.8 | Crear entidad Feedback | Jose Manuel Marquez Gutierrez | 1h | PENDIENTE | - |

---

### PBI-011: Repositorios Spring Data
**Prioridad:** Alta | **Estimacion:** 7h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-5.1 | Crear UsuarioRepository | Manuel Maria Calderon Rodriguez | 1h | PENDIENTE | - |
| T-5.2 | Crear ProfesorRepository y EstudianteRepository | Manuel Maria Calderon Rodriguez | 1h | PENDIENTE | - |
| T-5.3 | Crear CursoRepository y GrupoRepository | Jose Manuel Marquez Gutierrez | 1.5h | PENDIENTE | - |
| T-5.4 | Crear ActividadRepository | Jose Manuel Marquez Gutierrez | 1h | PENDIENTE | - |
| T-5.5 | Crear EntregableRepository y EntregaRepository | Manuel Maria Calderon Rodriguez | 1.5h | PENDIENTE | - |
| T-5.6 | Crear MaterialRepository y FeedbackRepository | Jose Manuel Marquez Gutierrez | 1h | PENDIENTE | - |

---

### PBI-012: Service y Controller Usuarios
**Prioridad:** Alta | **Estimacion:** 8h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-6.1 | Crear DTOs Usuario | Manuel Maria Calderon Rodriguez | 1.5h | PENDIENTE | - |
| T-6.2 | Crear UsuarioService | Manuel Maria Calderon Rodriguez | 3h | PENDIENTE | - |
| T-6.3 | Crear UsuarioController | Manuel Maria Calderon Rodriguez | 2h | PENDIENTE | - |
| T-6.4 | Crear GlobalExceptionHandler | Jose Manuel Marquez Gutierrez | 1.5h | PENDIENTE | - |

---

### PBI-073: Login Seguro
**Prioridad:** Alta | **Estimacion:** 8h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-7.1 | Configurar Spring Security | Jose Manuel Marquez Gutierrez | 2h | PENDIENTE | - |
| T-7.2 | Implementar JwtTokenProvider | Jose Manuel Marquez Gutierrez | 2h | PENDIENTE | - |
| T-7.3 | Implementar JwtTokenFilter | Jose Manuel Marquez Gutierrez | 2h | PENDIENTE | - |
| T-7.4 | Crear AuthController (login/register) | Manuel Maria Calderon Rodriguez | 2h | PENDIENTE | - |

---

### PBI-074: Cerrar Sesion
**Prioridad:** Alta | **Estimacion:** 2h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-8.1 | Implementar logout en backend | Manuel Maria Calderon Rodriguez | 1h | PENDIENTE | - |
| T-8.2 | Implementar logout en frontend | Jose Manuel Marquez Gutierrez | 1h | PENDIENTE | - |

---

### PBI-076: JWT Autenticacion
**Prioridad:** Alta | **Estimacion:** 8h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-9.1 | Configurar secreto JWT | Jose Manuel Marquez Gutierrez | 1h | PENDIENTE | - |
| T-9.2 | Implementar generacion de tokens | Jose Manuel Marquez Gutierrez | 2h | PENDIENTE | - |
| T-9.3 | Implementar validacion de tokens | Jose Manuel Marquez Gutierrez | 2h | PENDIENTE | - |
| T-9.4 | Implementar refresh tokens | Manuel Maria Calderon Rodriguez | 3h | PENDIENTE | - |

---

### PBI-055/056: Estructura Frontend Base
**Prioridad:** Media | **Estimacion:** 8h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-10.1 | Crear tipos TypeScript base | Manuel Maria Calderon Rodriguez | 2h | PENDIENTE | - |
| T-10.2 | Crear AuthContext | Jose Manuel Marquez Gutierrez | 2h | PENDIENTE | - |
| T-10.3 | Crear servicios API base | Manuel Maria Calderon Rodriguez | 2h | PENDIENTE | - |
| T-10.4 | Crear layout basico | Jose Manuel Marquez Gutierrez | 2h | PENDIENTE | - |

---

### Buffer y Contingencia
**Estimacion:** 3h | **Estado:** RESERVADO

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-11.1 | Buffer para imprevistos | Ambos | 3h | RESERVADO | - |

---

## Resumen del Progreso

### Por Estado
| Estado | Tareas | Horas |
|--------|--------|-------|
| COMPLETADO | 0 | 0h |
| EN PROGRESO | 0 | 0h |
| PENDIENTE | 36 | 80h |
| **Total** | **36** | **80h** |

### Por Persona
| Persona | Completado | En Progreso | Pendiente | Total |
|---------|------------|-------------|-----------|-------|
| Manuel Maria Calderon Rodriguez | 0h | 0h | 40h | 40h |
| Jose Manuel Marquez Gutierrez | 0h | 0h | 40h | 40h |

---

## Burndown Chart (Horas Restantes)

```
80 |XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
75 |                                                                              (Ideal)
70 |
65 |
60 |
55 |
50 |
45 |
40 |
35 |
30 |
25 |
20 |
15 |
10 |
 5 |
 0 |________________________________________________________________________________
    D1  D2  D3  D4  D5  D6  D7  D8  D9  D10 D11 D12 D13 D14
```

**Estado:** No iniciado

---

## Daily Scrum Notes

### Dia 1-2 (24-25 Feb)
- [ ] Planificacion inicio
- [ ] Configuracion entornos

### Dia 3-4 (26-27 Feb)
- [ ] Entidades JPA
- [ ] Configuracion BD

### Dia 5-6 (28 Feb - 3 Mar)
- [ ] Repositorios
- [ ] DTOs basicos

### Dia 7-8 (4-5 Mar)
- [ ] Servicios Usuario
- [ ] Spring Security

### Dia 9-10 (6-7 Mar)
- [ ] JWT implementacion
- [ ] AuthController

### Dia 11-12 (8-9 Mar)
- [ ] Frontend base
- [ ] AuthContext
- [ ] Sprint Review

---

## Criterios de Aceptacion del Sprint

- [ ] Backend compilando sin errores
- [ ] Frontend compilando sin errores
- [ ] Login funcional con JWT
- [ ] Logout funcional
- [ ] Base de datos H2 funcionando
- [ ] Estructura de carpetas correcta
- [ ] Documentacion API basica

---

## Impedimentos

| # | Descripcion | Reportado | Estado | Resolucion |
|---|-------------|-----------|--------|------------|
| - | Ninguno hasta el momento | - | - | - |

---

## Notas del Sprint Review

*Por completar al finalizar el sprint*

---

## Retrospectiva

### Que fue bien?
*Por completar*

### Que se puede mejorar?
*Por completar*

### Acciones para el proximo sprint
*Por completar*

---

*Ultima actualizacion: 18 Febrero 2026*
