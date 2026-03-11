<#
.SYNOPSIS
    Comprueba PostgreSQL, puebla la BD y arranca el backend.

.DESCRIPTION
    1. Verifica que PostgreSQL esta accesible (Docker o local).
    2. Crea la base de datos "tfgdb" si no existe.
    3. Arranca el backend (PostgreSQL es la BD por defecto).
       El DataSeeder de Spring pobla las tablas automaticamente si estan vacias.

.PARAMETER DockerMode
    Si se indica, levanta PostgreSQL con docker-compose antes de comprobar.

.EXAMPLE
    .\test-postgres.ps1               # PostgreSQL ya esta corriendo (local o Docker)
    .\test-postgres.ps1 -DockerMode   # Levanta PostgreSQL con docker-compose primero
#>
param(
    [switch]$DockerMode
)

$ErrorActionPreference = "Stop"

$DB_HOST   = "localhost"
$DB_PORT   = 5432
$DB_NAME   = "tfgdb"
$DB_USER   = "tfg"
$DB_PASS   = "tfg1234"
$backendDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "`n=== Test PostgreSQL + Backend ===" -ForegroundColor Cyan

# --- 1. Levantar PostgreSQL con Docker si se pide ---
if ($DockerMode) {
    Write-Host "`n[1/4] Levantando PostgreSQL con docker-compose..." -ForegroundColor Yellow
    $composeFile = Join-Path (Split-Path $backendDir) "docker-compose.yml"
    docker-compose -f $composeFile up -d postgres
    Write-Host "Esperando a que PostgreSQL este listo..." -ForegroundColor Gray
    $maxRetries = 15
    $retry = 0
    do {
        Start-Sleep -Seconds 2
        $retry++
        $healthy = docker inspect --format="{{.State.Health.Status}}" tfg-postgres 2>$null
    } while ($healthy -ne "healthy" -and $retry -lt $maxRetries)

    if ($healthy -ne "healthy") {
        Write-Host "[!] PostgreSQL no arranco a tiempo." -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] PostgreSQL levantado via Docker." -ForegroundColor Green
} else {
    Write-Host "`n[1/4] Comprobando conexion a PostgreSQL ($DB_HOST`:$DB_PORT)..." -ForegroundColor Yellow
}

# --- 2. Verificar que PostgreSQL acepta conexiones ---
$tcpOk = $false
for ($i = 0; $i -lt 5; $i++) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect($DB_HOST, $DB_PORT)
        $tcp.Close()
        $tcpOk = $true
        break
    } catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $tcpOk) {
    Write-Host "[!] No se puede conectar a PostgreSQL en $DB_HOST`:$DB_PORT" -ForegroundColor Red
    Write-Host "    Asegurate de que PostgreSQL esta corriendo." -ForegroundColor Red
    Write-Host "    Opciones:" -ForegroundColor Gray
    Write-Host "      - .\test-postgres.ps1 -DockerMode   (levanta con Docker)" -ForegroundColor Gray
    Write-Host "      - Arranca PostgreSQL local manualmente" -ForegroundColor Gray
    exit 1
}
Write-Host "[OK] PostgreSQL accesible en $DB_HOST`:$DB_PORT" -ForegroundColor Green

# --- 3. Crear la base de datos si no existe ---
Write-Host "`n[2/4] Verificando base de datos '$DB_NAME'..." -ForegroundColor Yellow

$psqlCmd = $null
if (Get-Command psql -ErrorAction SilentlyContinue) {
    $psqlCmd = "local"
} elseif ($DockerMode -or (docker ps --filter "name=tfg-postgres" --format "{{.Names}}" 2>$null) -eq "tfg-postgres") {
    $psqlCmd = "docker"
}

if ($psqlCmd -eq "local") {
    $env:PGPASSWORD = $DB_PASS
    $dbExists = psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" 2>$null
    if ($dbExists -ne "1") {
        Write-Host "Creando base de datos '$DB_NAME'..." -ForegroundColor Gray
        psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -c "CREATE DATABASE $DB_NAME;" 2>$null
    }
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
    Write-Host "[OK] Base de datos '$DB_NAME' lista." -ForegroundColor Green
} elseif ($psqlCmd -eq "docker") {
    $dbExists = docker exec tfg-postgres psql -U $DB_USER -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" 2>$null
    if ($dbExists.Trim() -ne "1") {
        Write-Host "Creando base de datos '$DB_NAME'..." -ForegroundColor Gray
        docker exec tfg-postgres psql -U $DB_USER -d postgres -c "CREATE DATABASE $DB_NAME;" 2>$null
    }
    Write-Host "[OK] Base de datos '$DB_NAME' lista." -ForegroundColor Green
} else {
    Write-Host "[i] No se encontro psql ni contenedor Docker. Se asume que la BD ya existe." -ForegroundColor Gray
}

# --- 4. Matar proceso en puerto 8080 si hay alguno ---
Write-Host "`n[3/4] Comprobando puerto 8080..." -ForegroundColor Yellow
$portInUse = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($portInUse) {
    Write-Host "Puerto 8080 en uso. Deteniendo proceso..." -ForegroundColor Gray
    $portInUse | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 2
}
Write-Host "[OK] Puerto 8080 libre." -ForegroundColor Green

# --- 5. Arrancar backend ---
Write-Host "`n[4/4] Arrancando backend..." -ForegroundColor Yellow
Write-Host "       PostgreSQL es la BD por defecto." -ForegroundColor Gray
Write-Host "       El DataSeeder poblara la BD automaticamente si esta vacia." -ForegroundColor Gray
Write-Host "       Credenciales de prueba:" -ForegroundColor Gray
Write-Host "         Admin:    admin@ull.edu.es / admin123" -ForegroundColor White
Write-Host "         Profesor: juan.garcia@ull.edu.es / prof123" -ForegroundColor White
Write-Host "         Alumno:   ana.fernandez@ull.edu.es / alumno123" -ForegroundColor White
Write-Host ""

Push-Location $backendDir
try {
    .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
