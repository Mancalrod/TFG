# Seguridad de Preproduccion y Auditoria de Permisos

Este documento concentra tres entregables operativos:

- Variables de entorno minimas para ejecutar la plataforma de forma segura.
- Checklist OWASP para gate de preproduccion.
- Matriz de permisos por rol y por curso para auditoria.

## 1. Variables de Entorno Minimas

### 1.1 Backend (obligatorias)

| Variable | Entorno | Ejemplo | Uso |
|---|---|---|---|
| DATABASE_URL | Todos | jdbc:postgresql://host:5432/tfgdb | Conexion BD |
| DATABASE_USER | Todos | tfg_app | Usuario BD |
| DATABASE_PASSWORD | Todos | secret | Password BD |
| JWT_SECRET | Todos | cadena-larga-aleatoria | Firma JWT |
| app.jwt.expiration | Todos | 86400000 | Expiracion access token |
| app.jwt.refresh-expiration | Todos | 604800000 | Expiracion refresh token |
| spring.profiles.active | Todos | local / prod | Perfil Spring |

### 1.2 Backend (correo adaptativo)

#### Local o development (SMTP)

| Variable | Entorno | Ejemplo | Uso |
|---|---|---|---|
| NODE_ENV | local/dev | development | Seleccion de canal de correo |
| spring.mail.host | local/dev | smtp.gmail.com | Servidor SMTP |
| spring.mail.port | local/dev | 587 | Puerto SMTP |
| spring.mail.username | local/dev | no-reply@dominio.com | Cuenta SMTP |
| spring.mail.password | local/dev | app-password | Credencial SMTP |
| EMAIL_FROM_EMAIL | local/dev/prod | no-reply@dominio.com | Remitente logico |
| EMAIL_FROM_NAME | local/dev/prod | Polan | Nombre visible del remitente |

#### Produccion (Brevo API)

| Variable | Entorno | Ejemplo | Uso |
|---|---|---|---|
| NODE_ENV | prod | production | Activa canal HTTP en produccion |
| EMAIL_PROVIDER | prod | brevo | Selecciona proveedor HTTP |
| BREVO_API_KEY | prod | xkeysib-xxxxxx | Token API Brevo |
| BREVO_FROM_EMAIL | prod | no-reply@dominio.com | Remitente verificado |

### 1.3 Backend (Cloudinary)

| Variable | Entorno | Ejemplo | Uso |
|---|---|---|---|
| CLOUDINARY_ENABLED | prod | true | Activa almacenamiento Cloudinary |
| CLOUDINARY_CLOUD_NAME | prod | mi-cloud | Cuenta Cloudinary |
| CLOUDINARY_API_KEY | prod | 123456 | API key |
| CLOUDINARY_API_SECRET | prod | xxxxx | API secret |

### 1.4 Frontend (minimas)

| Variable | Entorno | Ejemplo | Uso |
|---|---|---|---|
| VITE_API_BASE_URL | Todos | https://api.midominio.com | URL API |

### 1.5 Recomendaciones de gestion

- Nunca versionar secretos en repositorio.
- Cargar secretos desde proveedor seguro (Render, GitHub Secrets, Vault o equivalente).
- Rotar JWT_SECRET, API keys y contraseñas de servicio al menos cada 90 dias.
- Verificar que NODE_ENV=production en preprod/prod para evitar dependencia de SMTP bloqueado por puertos.

## 2. Checklist OWASP para Gate de Preproduccion

Usar este checklist como criterio de salida antes de promover a produccion.

### 2.1 A01 Broken Access Control

- [ ] Todos los endpoints sensibles validan identidad del usuario autenticado.
- [ ] Permisos de profesor y estudiante se validan por curso, no solo por rol global.
- [ ] No hay endpoints que permitan modificar recursos de otro usuario sin control explicito.
- [ ] Pruebas negativas ejecutadas: acceso horizontal y vertical denegado.

### 2.2 A02 Cryptographic Failures

- [ ] Contraseñas almacenadas con BCrypt y nunca en claro.
- [ ] JWT_SECRET con longitud fuerte y sin valores por defecto en entornos reales.
- [ ] TLS habilitado en frontend y backend (HTTPS obligatorio).

### 2.3 A03 Injection

- [ ] Acceso a datos via JPA/repositorios parametrizados (sin SQL concatenado).
- [ ] Validacion de inputs en body, query, path y headers.
- [ ] Pruebas de inyeccion basicas contra campos de busqueda y formularios.

### 2.4 A04 Insecure Design

- [ ] Casos de uso de seguridad cubiertos por pruebas (password, foto, notificaciones, permisos por curso).
- [ ] Limites de tamano/tipo de archivo aplicados en backend y frontend.

### 2.5 A05 Security Misconfiguration

- [ ] CORS restringido a dominios esperados.
- [ ] Perfil de produccion sin consola H2 ni configuraciones de desarrollo.
- [ ] Headers de seguridad revisados (cache sensible, content type, etc.).

### 2.6 A06 Vulnerable and Outdated Components

- [ ] Dependencias escaneadas en CI (SCA) sin vulnerabilidades criticas abiertas.
- [ ] Versiones de librerias de auth/cifrado actualizadas.

### 2.7 A07 Identification and Authentication Failures

- [ ] Expiracion y refresh de tokens validados.
- [ ] Rate limiting activo en login y cambio de contraseña.
- [ ] Mensajes de error de login no filtran informacion sensible.

### 2.8 A08 Software and Data Integrity Failures

- [ ] Pipeline CI con checks obligatorios de tests.
- [ ] Migraciones BD versionadas (Flyway) y aplicadas en orden.

### 2.9 A09 Security Logging and Monitoring Failures

- [ ] Eventos criticos logueados: login, cambio de contraseña, subida de foto, errores de autorizacion.
- [ ] Alertas configuradas para picos de 401/403/429 y errores de correo.

### 2.10 A10 Server-Side Request Forgery

- [ ] Integraciones externas (Cloudinary/Brevo/OneDrive) usan dominios esperados.
- [ ] No se consumen URLs arbitrarias proporcionadas por usuario sin validacion.

## 3. Matriz de Permisos por Rol y por Curso

Regla base de auditoria: un mismo usuario puede ser profesor en Curso A y estudiante en Curso B. Todas las validaciones deben evaluar el contexto del curso objetivo.

### 3.1 Leyenda

- ALLOW: permitido.
- DENY: denegado.
- COND: permitido bajo condicion explicita.

### 3.2 Matriz principal

| Accion | Admin | Profesor del curso objetivo | Estudiante del curso objetivo | Usuario autenticado sin pertenencia al curso | No autenticado |
|---|---|---|---|---|---|
| Cambiar su propia contraseña | ALLOW | ALLOW | ALLOW | ALLOW | DENY |
| Cambiar contraseña de otro usuario | COND (solo si politica interna lo permite) | DENY | DENY | DENY | DENY |
| Subir su foto de perfil | ALLOW | ALLOW | ALLOW | ALLOW | DENY |
| Editar preferencias de notificacion propias | ALLOW | ALLOW | ALLOW | ALLOW | DENY |
| Crear entregable en curso X | ALLOW | ALLOW | DENY | DENY | DENY |
| Calificar entrega del curso X | ALLOW | ALLOW | DENY | DENY | DENY |
| Ver pendientes de calificacion (profesor) | ALLOW | ALLOW (solo sus cursos) | DENY | DENY | DENY |
| Ver pendientes de entrega (estudiante) | ALLOW | DENY* | ALLOW (solo propio usuario) | DENY | DENY |
| Leer notificaciones de otro usuario | DENY | DENY | DENY | DENY | DENY |

* Excepcion solo si existe caso de negocio formal y endpoint dedicado con auditoria.

### 3.3 Casos de auditoria obligatorios

- Caso A: usuario U profesor en Curso A y estudiante en Curso B.
  - [ ] U puede crear entregables en Curso A.
  - [ ] U no puede crear entregables en Curso B salvo rol profesor en B.
  - [ ] U solo ve pendientes de estudiante de su propio usuario.

- Caso B: escalada horizontal.
  - [ ] Estudiante S1 no puede consultar ni modificar recursos de S2.

- Caso C: escalada vertical.
  - [ ] Estudiante no puede ejecutar endpoints de profesor/admin.

## 4. Evidencias Minimas para Cierre de Auditoria

- Resultado de tests backend y frontend del ultimo commit candidato.
- Evidencia de variables configuradas sin secretos hardcodeados.
- Capturas o logs de pruebas negativas 401/403/429.
- Checklist OWASP firmado por responsable tecnico.
- Matriz de permisos validada en al menos dos cursos con roles cruzados.
