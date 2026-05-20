# Migracion de Neon a Render Postgres

## Objetivo

Sustituir Neon cuando se agota la cuota gratuita y mantener el backend en Render operativo con PostgreSQL gestionado.

## Prerrequisitos

- Servicio backend ya desplegado en Render.
- Acceso a Render Dashboard con permisos para crear bases de datos y editar variables de entorno.
- Backup de la base de datos actual (si necesitas conservar datos).

## Copia entre despliegues

Si lo que necesitas es pasar el contenido de una base PostgreSQL de un despliegue a otro y no puedes tener ambas activas a la vez, usa el script en dos pasos:

1. Exporta los datos de la primera base a un fichero.
2. Cuando la segunda base esté disponible, importa ese fichero.

```powershell
.\backend\copy-postgres-data.ps1 -Mode export `
	-SourceUrl "jdbc:postgresql://<origen>:5432/<bd_origen>?sslmode=require" `
	-SourceUser "<usuario_origen>" `
	-DumpFile "C:\temp\origen.dump"

.\backend\copy-postgres-data.ps1 -Mode import `
	-TargetUrl "jdbc:postgresql://<destino>:5432/<bd_destino>?sslmode=require" `
	-TargetUser "<usuario_destino>" `
	-DumpFile "C:\temp\origen.dump"
```

## Paso 1: Crear Render Postgres

1. En Render, crear un nuevo recurso PostgreSQL.
2. Elegir plan segun carga esperada.
3. Copiar credenciales de conexion:
- host
- port
- database
- user
- password
- ssl mode

## Paso 2: Construir DATABASE_URL JDBC

Formato recomendado:

```text
jdbc:postgresql://<host>:<port>/<database>?sslmode=require
```

Ejemplo:

```text
jdbc:postgresql://dpg-xxxx-a.oregon-postgres.render.com:5432/tfgdb?sslmode=require
```

## Paso 3: Actualizar variables del backend en Render

Actualizar estas variables en el servicio web del backend:

- `SPRING_PROFILES_ACTIVE=prod`
- `DATABASE_URL=jdbc:postgresql://<host>:<port>/<database>?sslmode=require`
- `DATABASE_USER=<user>`
- `DATABASE_PASSWORD=<password>`

No es necesario cambiar el codigo para mover de Neon a Render Postgres, porque la app ya consume variables de entorno genericas.

## Paso 4: Redeploy y validacion

1. Forzar redeploy del backend.
2. Verificar en logs de arranque:
- Sin `GenericJDBCException`
- Sin `PSQLException` de autenticacion o cuota
- Flyway ejecutado correctamente

3. Verificar endpoints:
- `GET /api/health/liveness` debe devolver 200
- `GET /api/health/readiness` debe devolver 200 si DB esta accesible
- `GET /api/health` (alias de readiness) debe devolver 200 si DB esta accesible

## Paso 5: Smoke test funcional

Ejecutar al menos:

1. Login
2. Un endpoint de lectura contra BD
3. Un endpoint de escritura contra BD

Si lectura/escritura fallan, revisar credenciales y reglas de acceso del proveedor.

## Notas de robustez aplicadas

- `application-prod.properties` usa ahora:
- `spring.jpa.hibernate.ddl-auto=validate` (Flyway como autoridad de esquema)
- Timeouts explicitos de HikariCP para fallo rapido

- Health endpoints separados:
- Liveness: no depende de DB
- Readiness: comprueba acceso real a DB

## Rollback rapido

Si algo falla tras migrar:

1. Restaurar variables `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` anteriores.
2. Redeploy.
3. Revisar logs y repetir validacion de health endpoints.

## Checklist final

- [ ] Base Postgres creada en Render
- [ ] Variables de entorno actualizadas
- [ ] Backend redeploy exitoso
- [ ] Readiness en UP
- [ ] Login + lectura + escritura OK
- [ ] Documentacion de operacion actualizada
