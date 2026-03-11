<#
.SYNOPSIS
    Reinicia las tablas H2 del proyecto TFG Gestión de Entregables.

.DESCRIPTION
    Dos modos de ejecución:
      -Mode full   → Borra los ficheros de la base de datos H2. Hibernate los
                      recreará al arrancar con ddl-auto=update.  (por defecto)
      -Mode truncate → Conecta a la BD H2 por JDBC y ejecuta TRUNCATE TABLE
                        en todas las tablas, manteniendo el esquema.

    También limpia la carpeta de entregas (uploads/entregas) si existe.

.PARAMETER Mode
    "full" (por defecto) o "truncate".

.EXAMPLE
    .\reset-db.ps1                  # Reset completo (borra ficheros DB)
    .\reset-db.ps1 -Mode truncate   # Solo vacía los datos, mantiene esquema
#>
param(
    [ValidateSet("full", "truncate")]
    [string]$Mode = "full"
)

$ErrorActionPreference = "Stop"

$backendDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$dataDir     = Join-Path $backendDir "data"
$dbFile      = Join-Path $dataDir "tfgdb.mv.db"
$uploadsDir  = Join-Path (Join-Path $backendDir "uploads") "entregas"

# ─── Comprobar si el servidor está usando el puerto 8080 ───
$portInUse = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($portInUse) {
    Write-Host "[!] El puerto 8080 esta en uso. Deteniendo el proceso..." -ForegroundColor Yellow
    $portInUse | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 2
    Write-Host "[OK] Proceso detenido." -ForegroundColor Green
}

# ─── Modo FULL: borrar ficheros de la BD ───
if ($Mode -eq "full") {
    Write-Host "`n=== RESET COMPLETO (borrar ficheros H2) ===" -ForegroundColor Cyan

    if (Test-Path $dbFile) {
        Remove-Item "$dataDir\tfgdb*" -Force
        Write-Host "[OK] Ficheros de base de datos eliminados." -ForegroundColor Green
    } else {
        Write-Host "[i] No se encontraron ficheros de base de datos." -ForegroundColor Gray
    }
}

# ─── Modo TRUNCATE: vaciar tablas via SQL ───
if ($Mode -eq "truncate") {
    Write-Host "`n=== TRUNCATE (vaciar datos, mantener esquema) ===" -ForegroundColor Cyan

    if (-not (Test-Path $dbFile)) {
        Write-Host "[!] No existe la base de datos en: $dbFile" -ForegroundColor Red
        exit 1
    }

    # Buscar el JAR de H2 en el repositorio Maven local
    $h2Jar = Get-ChildItem "$env:USERPROFILE\.m2\repository\com\h2database\h2" -Recurse -Filter "h2-*.jar" |
             Where-Object { $_.Name -notmatch "sources|javadoc" } |
             Sort-Object LastWriteTime -Descending |
             Select-Object -First 1

    if (-not $h2Jar) {
        Write-Host "[!] No se encontro el JAR de H2. Ejecuta 'mvnw compile' primero." -ForegroundColor Red
        exit 1
    }

    Write-Host "[i] Usando H2 JAR: $($h2Jar.FullName)" -ForegroundColor Gray

    # Tablas en orden seguro (primero las que no tienen dependencias entrantes,
    # respetando claves foráneas)
    $tables = @(
        "onedrive_tokens",
        "microsoft_tokens",
        "feedbacks",
        "materiales",
        "entregas",
        "entregables",
        "actividades_grupos",     # tabla join ManyToMany
        "actividades",
        "estudiantes",
        "grupos",
        "profesores",
        "cursos",
        "usuarios"
    )

    $sql = "SET REFERENTIAL_INTEGRITY FALSE;`n"
    foreach ($t in $tables) {
        $sql += "TRUNCATE TABLE $t;`n"
    }
    $sql += "SET REFERENTIAL_INTEGRITY TRUE;"

    $sqlFile = Join-Path $backendDir "reset-tables.sql"
    $sql | Out-File -FilePath $sqlFile -Encoding utf8

    $jdbcUrl = "jdbc:h2:file:$dataDir/tfgdb"

    try {
        java -cp $h2Jar.FullName org.h2.tools.RunScript `
            -url $jdbcUrl -user sa -password "" -script $sqlFile
        Write-Host "[OK] Todas las tablas han sido vaciadas." -ForegroundColor Green
    }
    catch {
        Write-Host "[!] Error al ejecutar SQL: $_" -ForegroundColor Red
    }
    finally {
        Remove-Item $sqlFile -Force -ErrorAction SilentlyContinue
    }
}

# ─── Limpiar carpeta de entregas ───
if (Test-Path $uploadsDir) {
    Remove-Item "$uploadsDir\*" -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "[OK] Carpeta de entregas limpiada." -ForegroundColor Green
} else {
    Write-Host "[i] No existe carpeta de entregas." -ForegroundColor Gray
}

Write-Host "`n[DONE] Reset completado. Arranca el servidor con: .\mvnw.cmd spring-boot:run" -ForegroundColor Cyan
