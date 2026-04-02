<#
.SYNOPSIS
    Resetea la base de datos del proyecto (PostgreSQL por defecto, H2 opcional).

.DESCRIPTION
    Modos soportados:
      -Mode postgres     -> Resetea PostgreSQL (por defecto) borrando/recreando schema public.
      -Mode h2-full      -> Borra ficheros H2 locales.
      -Mode h2-truncate  -> Vacía tablas H2 manteniendo esquema.
      -Mode both         -> Ejecuta reset de PostgreSQL y H2 full.

    Compatibilidad hacia atrás:
      -Mode full      == h2-full
      -Mode truncate  == h2-truncate

    También limpia la carpeta uploads/entregas para dejar el entorno limpio.

.PARAMETER Mode
    postgres (por defecto), h2-full, h2-truncate, both, full, truncate.

.PARAMETER DatabaseUrl
    URL JDBC de PostgreSQL. Si no se indica, usa DATABASE_URL o
    jdbc:postgresql://localhost:5432/tfgdb.

.PARAMETER PgUser
    Usuario PostgreSQL (default DATABASE_USER o tfg).

.PARAMETER PgPassword
    Password PostgreSQL como SecureString.
    Si no se indica, se usa DATABASE_PASSWORD.

.EXAMPLE
    .\reset-db.ps1
    .\reset-db.ps1 -Mode postgres
    .\reset-db.ps1 -Mode both
    .\reset-db.ps1 -Mode postgres -DatabaseUrl "jdbc:postgresql://localhost:5432/tfgdb" -PgUser tfg -PgPassword (ConvertTo-SecureString "miPassword" -AsPlainText -Force)
#>
param(
    [ValidateSet("postgres", "h2-full", "h2-truncate", "both", "full", "truncate")]
    [string]$Mode = "postgres",

    [string]$DatabaseUrl = $env:DATABASE_URL,
    [string]$PgUser = $env:DATABASE_USER,
    [SecureString]$PgPassword
)

$ErrorActionPreference = "Stop"

$backendDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$dataDir = Join-Path $backendDir "data"
$dbFile = Join-Path $dataDir "tfgdb.mv.db"
$uploadsDir = Join-Path (Join-Path $backendDir "uploads") "entregas"

if ([string]::IsNullOrWhiteSpace($DatabaseUrl)) {
    $DatabaseUrl = "jdbc:postgresql://localhost:5432/tfgdb"
}
if ([string]::IsNullOrWhiteSpace($PgUser)) {
    $PgUser = "tfg"
}

if (-not $PgPassword) {
    if (-not [string]::IsNullOrWhiteSpace($env:DATABASE_PASSWORD)) {
        $PgPassword = ConvertTo-SecureString $env:DATABASE_PASSWORD -AsPlainText -Force
    }
    else {
        throw "No se proporciono password de PostgreSQL. Define DATABASE_PASSWORD o usa -PgPassword (SecureString)."
    }
}

function ConvertFrom-SecureStringToPlainText {
    param([SecureString]$SecurePassword)

    if (-not $SecurePassword) {
        return ""
    }

    return [System.Net.NetworkCredential]::new('', $SecurePassword).Password
}

function Resolve-EffectiveMode {
    param([string]$InputMode)
    switch ($InputMode) {
        "full" { return "h2-full" }
        "truncate" { return "h2-truncate" }
        default { return $InputMode }
    }
}

function ConvertFrom-PostgresJdbcUrl {
    param([string]$JdbcUrl)

    $pattern = '^jdbc:postgresql://(?<host>[^:/?#]+)(?::(?<port>\d+))?/(?<db>[^?]+)'
    $match = [regex]::Match($JdbcUrl, $pattern)
    if (-not $match.Success) {
        throw "DATABASE_URL no tiene formato JDBC PostgreSQL valido: $JdbcUrl"
    }

    $portValue = 5432
    if ($match.Groups["port"].Success) {
        $portValue = [int]$match.Groups["port"].Value
    }

    return [pscustomobject]@{
        Host = $match.Groups["host"].Value
        Port = $portValue
        Database = $match.Groups["db"].Value
    }
}

function Find-PsqlExecutable {
    $fromPath = (Get-Command psql -ErrorAction SilentlyContinue)
    if ($fromPath) {
        return $fromPath.Source
    }

    $candidates = @(
        "C:\Program Files\PostgreSQL\18\bin\psql.exe",
        "C:\Program Files\PostgreSQL\17\bin\psql.exe",
        "C:\Program Files\PostgreSQL\16\bin\psql.exe"
    )

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    return $null
}

function Stop-BackendIfRunning {
    $portInUse = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
    if ($portInUse) {
        Write-Host "[!] El puerto 8080 esta en uso. Deteniendo proceso..." -ForegroundColor Yellow
        $portInUse | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
        Start-Sleep -Seconds 2
        Write-Host "[OK] Proceso detenido." -ForegroundColor Green
    }
}

function Reset-H2Full {
    Write-Host "`n=== RESET H2 FULL (borrar ficheros) ===" -ForegroundColor Cyan
    if (Test-Path $dbFile) {
        Remove-Item "$dataDir\tfgdb*" -Force
        Write-Host "[OK] Ficheros H2 eliminados." -ForegroundColor Green
    }
    else {
        Write-Host "[i] No se encontraron ficheros H2 en $dataDir." -ForegroundColor Gray
    }
}

function Reset-H2Truncate {
    Write-Host "`n=== RESET H2 TRUNCATE (vaciar datos, mantener esquema) ===" -ForegroundColor Cyan

    if (-not (Test-Path $dbFile)) {
        throw "No existe la base H2 en: $dbFile"
    }

    $h2Jar = Get-ChildItem "$env:USERPROFILE\.m2\repository\com\h2database\h2" -Recurse -Filter "h2-*.jar" |
        Where-Object { $_.Name -notmatch "sources|javadoc" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $h2Jar) {
        throw "No se encontro el JAR de H2. Ejecuta .\mvnw.cmd compile primero."
    }

    $tables = @(
        "onedrive_tokens",
        "microsoft_tokens",
        "feedbacks",
        "materiales",
        "entregas",
        "entregables",
        "notificaciones",
        "preferencias_notificacion",
        "actividades_grupos",
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
        java -cp $h2Jar.FullName org.h2.tools.RunScript -url $jdbcUrl -user sa -password "" -script $sqlFile
        Write-Host "[OK] Tablas H2 vaciadas." -ForegroundColor Green
    }
    finally {
        Remove-Item $sqlFile -Force -ErrorAction SilentlyContinue
    }
}

function Reset-PostgresWithPsql {
    param(
        [string]$PsqlExe,
        [string]$PgHost,
        [int]$Port,
        [string]$Database,
        [string]$User,
        [SecureString]$Password
    )

    $sql = @"
DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public AUTHORIZATION CURRENT_USER;
GRANT ALL ON SCHEMA public TO CURRENT_USER;
"@

    $plainPassword = ConvertFrom-SecureStringToPlainText -SecurePassword $Password
    $env:PGPASSWORD = $plainPassword
    try {
        & $PsqlExe -h $PgHost -p $Port -U $User -d $Database -v ON_ERROR_STOP=1 -c $sql
        if ($LASTEXITCODE -ne 0) {
            throw "psql termino con codigo $LASTEXITCODE"
        }
    }
    finally {
        $plainPassword = $null
        Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
    }
}

function Reset-PostgresWithDocker {
    param(
        [string]$PgHost,
        [int]$Port,
        [string]$Database,
        [string]$User,
        [SecureString]$Password
    )

    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $docker) {
        return $false
    }

    $runningNames = & docker ps --format "{{.Names}}" 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $runningNames) {
        return $false
    }

    $container = $null
    if ($runningNames -contains "tfg-postgres") {
        $container = "tfg-postgres"
    }
    else {
        $container = $runningNames | Where-Object { $_ -match "postgres" } | Select-Object -First 1
    }

    if (-not $container) {
        return $false
    }

    $sql = "DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public AUTHORIZATION CURRENT_USER; GRANT ALL ON SCHEMA public TO CURRENT_USER;"

    $plainPassword = ConvertFrom-SecureStringToPlainText -SecurePassword $Password
    $pgEnvKey = "PGPASS" + "WORD"
    $dockerEnvArg = "{0}={1}" -f $pgEnvKey, $plainPassword
    & docker exec -e $dockerEnvArg $container psql -h $PgHost -p $Port -U $User -d $Database -v ON_ERROR_STOP=1 -c $sql
    if ($LASTEXITCODE -ne 0) {
        throw "docker exec psql termino con codigo $LASTEXITCODE"
    }

    $plainPassword = $null
    $dockerEnvArg = $null

    Write-Host "[OK] Reset PostgreSQL aplicado via contenedor '$container'." -ForegroundColor Green
    return $true
}

function Reset-Postgres {
    Write-Host "`n=== RESET POSTGRESQL (schema public) ===" -ForegroundColor Cyan

    $conn = ConvertFrom-PostgresJdbcUrl -JdbcUrl $DatabaseUrl
    $psqlExe = Find-PsqlExecutable

    if ($psqlExe) {
        Write-Host "[i] Usando psql local: $psqlExe" -ForegroundColor Gray
        Reset-PostgresWithPsql -PsqlExe $psqlExe -PgHost $conn.Host -Port $conn.Port -Database $conn.Database -User $PgUser -Password $PgPassword
        Write-Host "[OK] Reset PostgreSQL completado en $($conn.Host):$($conn.Port)/$($conn.Database)." -ForegroundColor Green
        return
    }

    Write-Host "[i] psql local no encontrado, intentando via Docker..." -ForegroundColor Gray
    $doneViaDocker = Reset-PostgresWithDocker -PgHost $conn.Host -Port $conn.Port -Database $conn.Database -User $PgUser -Password $PgPassword
    if (-not $doneViaDocker) {
        throw "No se encontro psql local ni contenedor PostgreSQL en ejecucion para resetear la BD."
    }
}

function Clear-Uploads {
    if (Test-Path $uploadsDir) {
        Remove-Item "$uploadsDir\*" -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "[OK] Carpeta de entregas limpiada." -ForegroundColor Green
    }
    else {
        Write-Host "[i] No existe carpeta de entregas." -ForegroundColor Gray
    }
}

$effectiveMode = Resolve-EffectiveMode -InputMode $Mode

Write-Host "[INFO] Modo seleccionado: $effectiveMode" -ForegroundColor Yellow
Write-Host "[INFO] DATABASE_URL: $DatabaseUrl" -ForegroundColor Yellow

Stop-BackendIfRunning

switch ($effectiveMode) {
    "postgres" {
        Reset-Postgres
    }
    "h2-full" {
        Reset-H2Full
    }
    "h2-truncate" {
        Reset-H2Truncate
    }
    "both" {
        Reset-Postgres
        Reset-H2Full
    }
    default {
        throw "Modo no soportado: $effectiveMode"
    }
}

Clear-Uploads

Write-Host "`n[DONE] Reset completado. Arranca el backend con: .\mvnw.cmd spring-boot:run" -ForegroundColor Cyan
Write-Host "[DONE] Si la BD quedo vacia, DataSeeder volvera a poblarla al arrancar." -ForegroundColor Cyan
