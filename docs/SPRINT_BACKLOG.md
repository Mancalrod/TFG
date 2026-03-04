# Sprint Backlog - Sprint 2

## Informacion del Sprint

| Campo | Valor |
|-------|-------|
| **Sprint** | 2 |
| **Objetivo** | Gestion Entregables + Entregas (Frontend) |
| **Fecha Inicio** | 9 Marzo 2026 |
| **Fecha Fin** | 22 Marzo 2026 |
| **Duracion** | 2 semanas |
| **Horas Planificadas** | 120 horas (60h/persona) |
| **Horas Completadas** | 0 horas |
| **Estado** | EN CURSO |

---

## Equipo

| Miembro | Rol | Horas Disponibles | Horas Asignadas |
|---------|-----|-------------------|-----------------|
| Manuel Maria Calderon Rodriguez | Desarrollador Full-Stack | 60h | 60h |
| Jose Manuel Marquez Gutierrez | Desarrollador Full-Stack | 60h | 60h |

---

## Objetivo del Sprint

> Completar la funcionalidad frontend de gestion de entregables y entregas. Al finalizar, el profesor podra crear/editar/eliminar entregables, y el estudiante podra realizar entregas con subida de archivos, ver su historial de versiones y reenviar entregas. El profesor podra ver la lista de entregas por entregable y descargar archivos. Todas las funcionalidades tendran tests asociados.

---

## Contexto: Que existe ya (Sprint 1)

**Backend** (100% funcional para entregables y entregas):
- `EntregableService`: crear, listar, obtener, actualizar, cambiar visibilidad, eliminar, filtrar por plazo
- `EntregableController`: 10 endpoints REST
- `EntregaService`: realizar (con upload multipart y versionado), obtener, listar, calificar, descargar, estadisticas
- `EntregaController`: 10 endpoints REST
- Tests existentes: `EntregableServiceTest`, `EntregableControllerTest`, `EntregaServiceTest`, `EntregaControllerTest`

**Frontend** (parcialmente implementado):
- `EntregablePage.tsx` - Vista de detalle de entregable (solo lectura)
- `EditarEntregablePage.tsx` - Pagina de edicion (estructura existe pero logica incompleta)
- `entregableService.ts` - 10 metodos API (crear, listar, obtener, actualizar, cambiarVisibilidad, eliminar, etc.)
- `entregaService.ts` - 10 metodos API (realizar, listarParaEvaluar, listarHistorial, calificar, descargar, etc.)

**Pendiente en este sprint**: Completar toda la logica frontend de interaccion.

---

## Product Backlog Items (PBIs) del Sprint

### PBI-031/032: Crear Entregable (Frontend Profesor)
**Prioridad:** Must Have | **Estimacion:** 10h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-2.01 | Crear pagina/modal CrearEntregablePage con formulario completo | Manuel Maria | 3h | PENDIENTE | - |
| T-2.02 | Implementar campos: titulo, descripcion, fechas inicio/limite, nota maxima | Manuel Maria | 2h | PENDIENTE | - |
| T-2.03 | Implementar seleccion tipo archivo esperado y tamano maximo | Jose Manuel | 2h | PENDIENTE | - |
| T-2.04 | Conectar formulario con entregableService.crear() | Jose Manuel | 1.5h | PENDIENTE | - |
| T-2.05 | Validaciones frontend (campos requeridos, fechas coherentes) y mensajes de error | Manuel Maria | 1.5h | PENDIENTE | - |

---

### PBI-034/035: Editar y Configurar Entregable (Frontend)
**Prioridad:** Must Have | **Estimacion:** 8h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-2.06 | Completar EditarEntregablePage: cargar datos existentes del entregable | Jose Manuel | 3h | PENDIENTE | - |
| T-2.07 | Implementar actualizacion con entregableService.actualizar() | Jose Manuel | 2h | PENDIENTE | - |
| T-2.08 | Toggle permiteReenvio y configuracion avanzada | Manuel Maria | 1.5h | PENDIENTE | - |
| T-2.09 | Feedback visual de cambios guardados exitosamente | Manuel Maria | 1.5h | PENDIENTE | - |

---

### PBI-033: Eliminar y Visibilidad Entregable (Frontend)
**Prioridad:** Must Have | **Estimacion:** 6h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-2.10 | Boton eliminar entregable con dialogo de confirmacion | Jose Manuel | 2h | PENDIENTE | - |
| T-2.11 | Toggle visibilidad VISIBLE/OCULTO con icono visual | Manuel Maria | 2h | PENDIENTE | - |
| T-2.12 | Actualizar lista de entregables tras eliminar/cambiar visibilidad | Jose Manuel | 2h | PENDIENTE | - |

---

### PBI-036/037: Formulario Entrega Estudiante (Subida Archivos)
**Prioridad:** Must Have | **Estimacion:** 22h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-2.13 | Crear componente FormularioEntrega con zona drag & drop | Manuel Maria | 4h | PENDIENTE | - |
| T-2.14 | Implementar subida multipart con entregaService.realizar() | Jose Manuel | 4h | PENDIENTE | - |
| T-2.15 | Validacion frontend de tipo y tamano de archivo (segun entregable) | Manuel Maria | 2h | PENDIENTE | - |
| T-2.16 | Barra de progreso durante la subida | Jose Manuel | 2h | PENDIENTE | - |
| T-2.17 | Pantalla de confirmacion de entrega exitosa con resumen | Manuel Maria | 2h | PENDIENTE | - |
| T-2.18 | Manejo errores de subida (tamano excedido, tipo invalido, fuera de plazo) | Jose Manuel | 2h | PENDIENTE | - |
| T-2.19 | Integrar formulario en EntregablePage (condicional: solo si estudiante) | Manuel Maria | 3h | PENDIENTE | - |
| T-2.20 | Mostrar estado actual de la entrega del estudiante en EntregablePage | Jose Manuel | 3h | PENDIENTE | - |

---

### PBI-038/040/041: Vista Entregas Estudiante (Historial y Reenvio)
**Prioridad:** Must Have | **Estimacion:** 16h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-2.21 | Crear componente historial de versiones por entregable | Manuel Maria | 4h | PENDIENTE | - |
| T-2.22 | Mostrar cada version con: fecha, estado, calificacion (si existe) | Jose Manuel | 3h | PENDIENTE | - |
| T-2.23 | Indicar claramente version activa vs versiones anteriores | Manuel Maria | 2h | PENDIENTE | - |
| T-2.24 | Boton reenviar entrega (visible solo si permiteReenvio y en plazo) | Jose Manuel | 3h | PENDIENTE | - |
| T-2.25 | Vista global "Mis Entregas" del estudiante (todas sus entregas) | Manuel Maria | 4h | PENDIENTE | - |

---

### PBI-039/042: Vista Entregas Profesor + Descarga
**Prioridad:** Must Have | **Estimacion:** 16h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-2.26 | Crear vista lista de entregas por entregable (profesor) | Jose Manuel | 4h | PENDIENTE | - |
| T-2.27 | Mostrar por cada entrega: estudiante, fecha, estado, calificacion | Manuel Maria | 3h | PENDIENTE | - |
| T-2.28 | Boton descargar archivo individual de cada entrega | Jose Manuel | 3h | PENDIENTE | - |
| T-2.29 | Navegacion fluida: actividad -> entregable -> entregas | Manuel Maria | 3h | PENDIENTE | - |
| T-2.30 | Indicadores visuales por estado (badges: pendiente, entregado, calificado) | Jose Manuel | 3h | PENDIENTE | - |

---

### Mejoras UX y Navegacion
**Prioridad:** Should Have | **Estimacion:** 10h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-2.31 | Mejorar navegacion entre actividad <-> entregables | Manuel Maria | 3h | PENDIENTE | - |
| T-2.32 | Breadcrumbs o indicadores de ubicacion | Jose Manuel | 3h | PENDIENTE | - |
| T-2.33 | Indicadores de carga (loading spinners/skeletons) | Manuel Maria | 2h | PENDIENTE | - |
| T-2.34 | Mensajes de exito/error globales (toasts/notificaciones) | Jose Manuel | 2h | PENDIENTE | - |

---

### Testing Sprint 2
**Prioridad:** Must Have | **Estimacion:** 26h | **Estado:** PENDIENTE

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-2.35 | Ampliar tests unitarios EntregableService | Manuel Maria | 4h | PENDIENTE | - |
| T-2.36 | Ampliar tests unitarios EntregableController | Jose Manuel | 4h | PENDIENTE | - |
| T-2.37 | Ampliar tests unitarios EntregaService (upload, versionado) | Manuel Maria | 5h | PENDIENTE | - |
| T-2.38 | Ampliar tests unitarios EntregaController | Jose Manuel | 5h | PENDIENTE | - |
| T-2.39 | Tests integracion: flujo crear entregable -> entregar -> ver historial | Manuel Maria | 4h | PENDIENTE | - |
| T-2.40 | Tests integracion: flujo reenvio y versionado de entregas | Jose Manuel | 4h | PENDIENTE | - |

---

### Buffer y Contingencia
**Estimacion:** 6h | **Estado:** RESERVADO

| ID | Tarea | Responsable | Estimacion | Estado | Horas Reales |
|----|-------|-------------|------------|--------|--------------|
| T-2.41 | Ajustes backend si necesario | Ambos | 4h | RESERVADO | - |
| T-2.42 | Buffer para imprevistos | Ambos | 2h | RESERVADO | - |

---

## Resumen del Progreso

### Por Estado
| Estado | Tareas | Horas |
|--------|--------|-------|
| COMPLETADO | 0 | 0h |
| EN PROGRESO | 0 | 0h |
| PENDIENTE | 40 | 114h |
| RESERVADO | 2 | 6h |
| **Total** | **42** | **120h** |

### Por Persona
| Persona | Completado | En Progreso | Pendiente | Total |
|---------|------------|-------------|-----------|-------|
| Manuel Maria Calderon Rodriguez | 0h | 0h | 60h | 60h |
| Jose Manuel Marquez Gutierrez | 0h | 0h | 60h | 60h |

---

## Burndown Chart (Horas Restantes)

```
120|XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
115|                                                                  (Ideal)
110|
100|
 90|
 80|
 70|
 60|
 50|
 40|
 30|
 20|
 10|
  0|________________________________________________________________________
    D1  D2  D3  D4  D5  D6  D7  D8  D9  D10 D11 D12 D13 D14
```

**Estado:** En curso

---

## Daily Scrum Notes

### Semana 1 (9-13 Mar)

#### Dia 1-2 (9-10 Mar)
- [ ] Crear entregable: formulario profesor
- [ ] Editar entregable: completar logica

#### Dia 3-4 (11-12 Mar)
- [ ] Eliminar + visibilidad entregable
- [ ] Formulario entrega estudiante (inicio)

#### Dia 5 (13 Mar)
- [ ] Formulario entrega: subida archivos + validaciones

### Semana 2 (16-20 Mar)

#### Dia 6-7 (16-17 Mar)
- [ ] Vista entregas estudiante (historial versiones)
- [ ] Vista entregas profesor (lista por entregable)

#### Dia 8-9 (18-19 Mar)
- [ ] Descarga archivos + reenvio entregas
- [ ] Mejoras UX (breadcrumbs, toasts, loading)

#### Dia 10 (20 Mar)
- [ ] Testing: ampliar tests entregables y entregas
- [ ] Tests integracion flujos completos

#### Dia 11-12 (21-22 Mar)
- [ ] Completar testing
- [ ] Correccion de bugs
- [ ] Sprint Review y Retrospectiva

---

## Criterios de Aceptacion del Sprint

- [ ] Profesor puede crear entregables con todos los campos (titulo, descripcion, fechas, nota, tipo archivo, tamano)
- [ ] Profesor puede editar entregables existentes
- [ ] Profesor puede eliminar entregables (con confirmacion)
- [ ] Profesor puede cambiar visibilidad VISIBLE/OCULTO
- [ ] Profesor puede configurar si permite reenvios
- [ ] Estudiante puede subir archivos como entrega (respetando tipo y tamano)
- [ ] Estudiante puede ver historial de versiones de sus entregas
- [ ] Estudiante puede reenviar entregas si el entregable lo permite
- [ ] Profesor puede ver lista de entregas por entregable (con estado visual)
- [ ] Profesor puede descargar archivos de entregas individuales
- [ ] Navegacion fluida: actividad -> entregable -> entregas
- [ ] Tests unitarios y de integracion pasando (cobertura ampliada)
- [ ] Todo desplegado y funcional en Render

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

*Ultima actualizacion: 3 Marzo 2026*
