<#
.SYNOPSIS
    Asegura que PostgreSQL esta corriendo y arranca el backend Spring Boot.
.DESCRIPTION
    1. Intenta iniciar PostgreSQL via servicio de Windows.
    2. Si no hay servicio, usa pg_ctl directamente.
    3. Verifica la BD y arranca el backend.
.EXAMPLE
    .\test-postgres.ps1
#>
param()

$ErrorActionPreference = "Stop"

$DB_HOST    = "localhost"
$DB_PORT    = 5432
$DB_NAME    = "tfgdb"
$DB_USER    = "tfg"
$DB_PASS    = "tfg1234"
$backendDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Rutas de la instalacion local de PostgreSQL
$pgData  = "C:\Program Files\PostgreSQL\18\data"
$pgCtl   = "C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe"
$psqlExe = "C:\Program Files\PostgreSQL\18\bin\psql.exe"

# ----------------------------------------------------------------
# Asegurar que PostgreSQL este corriendo
# ----------------------------------------------------------------
function Start-Postgres {
    # 1. Intentar via servicio de Windows (instalador oficial registra uno)
    $svcNames = @("postgresql-x64-18", "postgresql-x64-17", "postgresql-x64-16", "postgresql")
    foreach ($svc in $svcNames) {
        $s = Get-Service $svc -ErrorAction SilentlyContinue
        if ($s) {
            if ($s.Status -ne "Running") {
                Write-Host "    Iniciando servicio '$svc'..." -ForegroundColor Gray
                Start-Service $svc
                Write-Host "[OK] Servicio '$svc' iniciado." -ForegroundColor Green
            } else {
                Write-Host "[OK] Servicio '$svc' ya estaba corriendo." -ForegroundColor Green
            }
            return
        }
    }

    # 2. Fallback: pg_ctl cuando no hay servicio de Windows
    if (-not (Test-Path $pgCtl)) {
        Write-Host "[!] No se encontro pg_ctl ni ningun servicio de PostgreSQL." -ForegroundColor Red
        Write-Host "    Instala PostgreSQL o arrancalo manualmente antes de ejecutar este script." -ForegroundColor Red
        exit 1
    }
    Write-Host "    Comprobando estado via pg_ctl..." -ForegroundColor Gray
    $pgStatus = & $pgCtl status -D $pgData 2>&1
    if ($pgStatus -match "no server running") {
        Write-Host "    Iniciando PostgreSQL via pg_ctl..." -ForegroundColor Gray
        $logFile = Join-Path $backendDir "pg.log"
        & $pgCtl start -D $pgData -l $logFile
        Start-Sleep -Seconds 3
        Write-Host "[OK] PostgreSQL iniciado via pg_ctl." -ForegroundColor Green
    } else {
        Write-Host "[OK] PostgreSQL ya estaba corriendo (pg_ctl)." -ForegroundColor Green
    }
}

# ----------------------------------------------------------------
# Main
# ----------------------------------------------------------------
Write-Host ""
Write-Host "=== Test PostgreSQL + Backend ===" -ForegroundColor Cyan

# --- 1. Asegurar que PostgreSQL este corriendo ---
Write-Host ""
Write-Host "[1/4] Iniciando PostgreSQL..." -ForegroundColor Yellow
Start-Postgres

# --- 2. Verificar que PostgreSQL acepta conexiones ---
$tcpOk = $false
for ($i = 0; $i -lt 6; $i++) {
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
    Write-Host "[!] PostgreSQL no responde en $DB_HOST`:$DB_PORT tras varios intentos." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] PostgreSQL accesible en $DB_HOST`:$DB_PORT" -ForegroundColor Green

# --- 3. Crear la base de datos si no existe ---
Write-Host ""
Write-Host "[2/4] Verificando base de datos '$DB_NAME'..." -ForegroundColor Yellow

if (Test-Path $psqlExe) {
    $env:PGPASSWORD = $DB_PASS
    $dbExists = & $psqlExe -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" 2>$null
    if ($dbExists.Trim() -ne "1") {
        Write-Host "Creando base de datos '$DB_NAME'..." -ForegroundColor Gray
        & $psqlExe -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -c "CREATE DATABASE $DB_NAME;" 2>$null
    }
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
    Write-Host "[OK] Base de datos '$DB_NAME' lista." -ForegroundColor Green
} else {
    Write-Host "[i] psql no encontrado en '$psqlExe'. Se asume que la BD ya existe." -ForegroundColor Gray
}

# --- 4. Liberar puerto 8080 ---
Write-Host ""
Write-Host "[3/4] Comprobando puerto 8080..." -ForegroundColor Yellow
$portInUse = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($portInUse) {
    Write-Host "Puerto 8080 en uso. Deteniendo proceso..." -ForegroundColor Gray
    $portInUse | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 2
}
Write-Host "[OK] Puerto 8080 libre." -ForegroundColor Green

# --- 5. Arrancar backend ---
Write-Host ""
Write-Host "[4/4] Arrancando backend..." -ForegroundColor Yellow
Write-Host "      PostgreSQL es la BD por defecto." -ForegroundColor Gray
Write-Host "      El DataSeeder poblara la BD si esta vacia." -ForegroundColor Gray
Write-Host "      Credenciales de prueba:" -ForegroundColor Gray
Write-Host "        Admin:    admin@ull.edu.es / admin123" -ForegroundColor White
Write-Host "        Profesor: juan.garcia@ull.edu.es / prof123" -ForegroundColor White
Write-Host "        Alumno:   ana.fernandez@ull.edu.es / alumno123" -ForegroundColor White
Write-Host ""

Push-Location $backendDir
try {
    .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}