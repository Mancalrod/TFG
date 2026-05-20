<#
.SYNOPSIS
    Copia el esquema y los datos de una base PostgreSQL de origen a otra de destino.

.DESCRIPTION
    Usa pg_dump para extraer la base de origen y pg_restore para cargarla en el destino.
    Acepta URLs JDBC o URLs postgresql:// y toma credenciales desde parámetros o variables de entorno.
    Puede funcionar en dos pasos separados: exportar un dump primero e importarlo después.

.PARAMETER SourceUrl
    URL JDBC o URL PostgreSQL del origen.

.PARAMETER TargetUrl
    URL JDBC o URL PostgreSQL del destino.

.PARAMETER SourceUser
    Usuario del origen. Si no se indica, intenta usar el usuario embebido en la URL o SOURCE_DATABASE_USER.

.PARAMETER SourcePassword
    Password del origen como SecureString. Si no se indica, intenta usar SOURCE_DATABASE_PASSWORD o el password embebido en la URL.

.PARAMETER TargetUser
    Usuario del destino. Si no se indica, intenta usar el usuario embebido en la URL o TARGET_DATABASE_USER.

.PARAMETER TargetPassword
    Password del destino como SecureString. Si no se indica, intenta usar TARGET_DATABASE_PASSWORD o el password embebido en la URL.

.PARAMETER KeepDump
    Conserva el archivo temporal del dump al finalizar.

.PARAMETER Mode
    export  -> genera un dump desde la base de origen.
    import  -> restaura un dump en la base de destino.
    transfer -> hace export + import en una sola ejecución.

.PARAMETER DumpFile
    Ruta del archivo dump. En export se crea o sobrescribe; en import debe existir.

.EXAMPLE
    .\copy-postgres-data.ps1 `
        -SourceUrl "jdbc:postgresql://source-host:5432/source_db?sslmode=require" `
        -TargetUrl "jdbc:postgresql://target-host:5432/target_db?sslmode=require" `
        -SourceUser source_user `
        -SourcePassword (ConvertTo-SecureString "source_pass" -AsPlainText -Force) `
        -TargetUser target_user `
        -TargetPassword (ConvertTo-SecureString "target_pass" -AsPlainText -Force)

.EXAMPLE
    $env:SOURCE_DATABASE_URL = "postgresql://source_user:source_pass@source-host:5432/source_db?sslmode=require"
    $env:TARGET_DATABASE_URL = "postgresql://target_user:target_pass@target-host:5432/target_db?sslmode=require"
    .\copy-postgres-data.ps1

.EXAMPLE
    .\copy-postgres-data.ps1 -Mode export -SourceUrl "jdbc:postgresql://source-host:5432/source_db?sslmode=require" -DumpFile "C:\temp\source.dump"

.EXAMPLE
    .\copy-postgres-data.ps1 -Mode import -TargetUrl "jdbc:postgresql://target-host:5432/target_db?sslmode=require" -DumpFile "C:\temp\source.dump"
#>
param(
    [ValidateSet("export", "import", "transfer")]
    [string]$Mode = "transfer",

    [string]$SourceUrl = $env:SOURCE_DATABASE_URL,
    [string]$TargetUrl = $env:TARGET_DATABASE_URL,

    [string]$SourceUser = $env:SOURCE_DATABASE_USER,
    [SecureString]$SourcePassword,

    [string]$TargetUser = $env:TARGET_DATABASE_USER,
    [SecureString]$TargetPassword,

    [string]$DumpFile,

    [switch]$KeepDump
)

$ErrorActionPreference = "Stop"

function Get-PlainTextFromSecureString {
    param([SecureString]$SecureString)

    if (-not $SecureString) {
        return ""
    }

    return [System.Net.NetworkCredential]::new('', $SecureString).Password
}

function Parse-PostgresConnectionUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url
    )

    $normalized = $Url.Trim()
    if ($normalized.StartsWith("jdbc:")) {
        $normalized = $normalized.Substring(5)
    }

    $pattern = '^(?<scheme>postgres|postgresql)://(?:(?<user>[^:@/?#]+)(?::(?<pass>[^@/?#]*))?@)?(?<host>[^:/?#]+)(?::(?<port>\d+))?(?:/(?<db>[^?#]*))?(?:\?(?<query>.*))?$'
    $match = [regex]::Match($normalized, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if (-not $match.Success) {
        throw "La URL no es valida. Usa formato jdbc:postgresql://host:puerto/base o postgresql://user:pass@host:puerto/base"
    }

    $queryParams = @{}
    if (-not [string]::IsNullOrWhiteSpace($match.Groups["query"].Value)) {
        foreach ($pair in $match.Groups["query"].Value -split '&') {
            if ([string]::IsNullOrWhiteSpace($pair)) { continue }
            $parts = $pair -split '=', 2
            $key = [Uri]::UnescapeDataString($parts[0])
            $value = if ($parts.Length -eq 2) { [Uri]::UnescapeDataString($parts[1]) } else { "" }
            $queryParams[$key.ToLowerInvariant()] = $value
        }
    }

    return [pscustomobject]@{
        Host      = $match.Groups["host"].Value
        Port      = if ($match.Groups["port"].Success) { [int]$match.Groups["port"].Value } else { 5432 }
        Database  = [Uri]::UnescapeDataString($match.Groups["db"].Value)
        User      = [Uri]::UnescapeDataString($match.Groups["user"].Value)
        Password  = [Uri]::UnescapeDataString($match.Groups["pass"].Value)
        SslMode   = if ($queryParams.ContainsKey("sslmode") -and -not [string]::IsNullOrWhiteSpace($queryParams["sslmode"])) { $queryParams["sslmode"] } else { "require" }
    }
}

function Resolve-ConnectionInfo {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,

        [string]$ExplicitUser,

        [SecureString]$ExplicitPassword,

        [string]$EnvUserName,

        [string]$EnvPasswordName
    )

    $parsed = Parse-PostgresConnectionUrl -Url $Url

    $user = $ExplicitUser
    if ([string]::IsNullOrWhiteSpace($user)) {
        $user = $parsed.User
    }
    if ([string]::IsNullOrWhiteSpace($user)) {
        $user = [Environment]::GetEnvironmentVariable($EnvUserName, "Process")
    }

    $password = Get-PlainTextFromSecureString -SecureString $ExplicitPassword
    if ([string]::IsNullOrWhiteSpace($password)) {
        $password = $parsed.Password
    }
    if ([string]::IsNullOrWhiteSpace($password)) {
        $password = [Environment]::GetEnvironmentVariable($EnvPasswordName, "Process")
    }

    if ([string]::IsNullOrWhiteSpace($user)) {
        throw "No se pudo resolver el usuario para la URL: $Url"
    }

    if ([string]::IsNullOrWhiteSpace($password)) {
        throw "No se pudo resolver el password para la URL: $Url"
    }

    if ([string]::IsNullOrWhiteSpace($parsed.Host)) {
        throw "La URL no contiene host valido: $Url"
    }

    if ([string]::IsNullOrWhiteSpace($parsed.Database)) {
        throw "La URL no contiene nombre de base de datos: $Url"
    }

    return [pscustomobject]@{
        Host     = $parsed.Host
        Port     = $parsed.Port
        Database = $parsed.Database
        User     = $user
        Password = $password
        SslMode  = $parsed.SslMode
    }
}

function Find-PostgresExecutable {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ExecutableName
    )

    $fromPath = Get-Command $ExecutableName -ErrorAction SilentlyContinue
    if ($fromPath) {
        return $fromPath.Source
    }

    $candidateDirs = @(
        "C:\Program Files\PostgreSQL\18\bin",
        "C:\Program Files\PostgreSQL\17\bin",
        "C:\Program Files\PostgreSQL\16\bin"
    )

    foreach ($dir in $candidateDirs) {
        $candidate = Join-Path $dir $ExecutableName
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    return $null
}

function Invoke-PostgresDump {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PgDumpExe,

        [Parameter(Mandatory = $true)]
        [pscustomobject]$Connection,

        [Parameter(Mandatory = $true)]
        [string]$DumpFile
    )

    $previousPassword = [Environment]::GetEnvironmentVariable("PGPASSWORD", "Process")
    $previousSslMode = [Environment]::GetEnvironmentVariable("PGSSLMODE", "Process")

    try {
        [Environment]::SetEnvironmentVariable("PGPASSWORD", $Connection.Password, "Process")
        [Environment]::SetEnvironmentVariable("PGSSLMODE", $Connection.SslMode, "Process")

        & $PgDumpExe --host $Connection.Host --port $Connection.Port --username $Connection.User --dbname $Connection.Database --format custom --no-owner --no-acl --verbose --file $DumpFile
        if ($LASTEXITCODE -ne 0) {
            throw "pg_dump termino con codigo $LASTEXITCODE"
        }
    }
    finally {
        [Environment]::SetEnvironmentVariable("PGPASSWORD", $previousPassword, "Process")
        [Environment]::SetEnvironmentVariable("PGSSLMODE", $previousSslMode, "Process")
    }
}

function Invoke-PostgresRestore {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PgRestoreExe,

        [Parameter(Mandatory = $true)]
        [pscustomobject]$Connection,

        [Parameter(Mandatory = $true)]
        [string]$DumpFile
    )

    $previousPassword = [Environment]::GetEnvironmentVariable("PGPASSWORD", "Process")
    $previousSslMode = [Environment]::GetEnvironmentVariable("PGSSLMODE", "Process")

    try {
        [Environment]::SetEnvironmentVariable("PGPASSWORD", $Connection.Password, "Process")
        [Environment]::SetEnvironmentVariable("PGSSLMODE", $Connection.SslMode, "Process")

        & $PgRestoreExe --host $Connection.Host --port $Connection.Port --username $Connection.User --dbname $Connection.Database --clean --if-exists --no-owner --no-privileges --verbose $DumpFile
        if ($LASTEXITCODE -ne 0) {
            throw "pg_restore termino con codigo $LASTEXITCODE"
        }
    }
    finally {
        [Environment]::SetEnvironmentVariable("PGPASSWORD", $previousPassword, "Process")
        [Environment]::SetEnvironmentVariable("PGSSLMODE", $previousSslMode, "Process")
    }
}

$pgDumpExe = Find-PostgresExecutable -ExecutableName "pg_dump.exe"
if (-not $pgDumpExe) {
    throw "No se encontro pg_dump.exe. Instala PostgreSQL client tools o añade pg_dump al PATH."
}

$pgRestoreExe = Find-PostgresExecutable -ExecutableName "pg_restore.exe"
if (-not $pgRestoreExe) {
    throw "No se encontro pg_restore.exe. Instala PostgreSQL client tools o añade pg_restore al PATH."
}

function Get-DefaultDumpFile {
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    return Join-Path $env:TEMP "tfg_postgres_transfer_$timestamp.dump"
}

function Ensure-ExportParameters {
    param([string]$InputUrl)

    if ([string]::IsNullOrWhiteSpace($InputUrl)) {
        throw "Debes indicar -SourceUrl o definir SOURCE_DATABASE_URL"
    }
}

function Ensure-ImportParameters {
    param([string]$InputUrl)

    if ([string]::IsNullOrWhiteSpace($InputUrl)) {
        throw "Debes indicar -TargetUrl o definir TARGET_DATABASE_URL"
    }
}

if ([string]::IsNullOrWhiteSpace($DumpFile)) {
    $DumpFile = Get-DefaultDumpFile
}

switch ($Mode) {
    "export" {
        Ensure-ExportParameters -InputUrl $SourceUrl
        $source = Resolve-ConnectionInfo -Url $SourceUrl -ExplicitUser $SourceUser -ExplicitPassword $SourcePassword -EnvUserName "SOURCE_DATABASE_USER" -EnvPasswordName "SOURCE_DATABASE_PASSWORD"

        Write-Host "==> Origen: $($source.Host):$($source.Port)/$($source.Database)" -ForegroundColor Cyan
        Write-Host "==> Dump: $DumpFile" -ForegroundColor Cyan
        Write-Host "==> Exportando base de origen con pg_dump..." -ForegroundColor Cyan

        Invoke-PostgresDump -PgDumpExe $pgDumpExe -Connection $source -DumpFile $DumpFile
        Write-Host "[OK] Dump generado correctamente." -ForegroundColor Green
        return
    }

    "import" {
        Ensure-ImportParameters -InputUrl $TargetUrl

        if (-not (Test-Path $DumpFile)) {
            throw "No existe el dump indicado: $DumpFile"
        }

        $target = Resolve-ConnectionInfo -Url $TargetUrl -ExplicitUser $TargetUser -ExplicitPassword $TargetPassword -EnvUserName "TARGET_DATABASE_USER" -EnvPasswordName "TARGET_DATABASE_PASSWORD"

        Write-Host "==> Destino: $($target.Host):$($target.Port)/$($target.Database)" -ForegroundColor Cyan
        Write-Host "==> Dump: $DumpFile" -ForegroundColor Cyan
        Write-Host "==> Restaurando en la base de destino con pg_restore..." -ForegroundColor Cyan

        Invoke-PostgresRestore -PgRestoreExe $pgRestoreExe -Connection $target -DumpFile $DumpFile
        Write-Host "[OK] Restauracion completada correctamente." -ForegroundColor Green
        return
    }

    "transfer" {
        Ensure-ExportParameters -InputUrl $SourceUrl
        Ensure-ImportParameters -InputUrl $TargetUrl

        $source = Resolve-ConnectionInfo -Url $SourceUrl -ExplicitUser $SourceUser -ExplicitPassword $SourcePassword -EnvUserName "SOURCE_DATABASE_USER" -EnvPasswordName "SOURCE_DATABASE_PASSWORD"
        $target = Resolve-ConnectionInfo -Url $TargetUrl -ExplicitUser $TargetUser -ExplicitPassword $TargetPassword -EnvUserName "TARGET_DATABASE_USER" -EnvPasswordName "TARGET_DATABASE_PASSWORD"

        if ($source.Host -eq $target.Host -and $source.Port -eq $target.Port -and $source.Database -eq $target.Database -and $source.User -eq $target.User) {
            Write-Host "[i] Origen y destino parecen apuntar al mismo servidor/base/usuario. Continúo porque puede ser intencional, pero revisa los parámetros." -ForegroundColor Yellow
        }

        Write-Host "==> Origen: $($source.Host):$($source.Port)/$($source.Database)" -ForegroundColor Cyan
        Write-Host "==> Destino: $($target.Host):$($target.Port)/$($target.Database)" -ForegroundColor Cyan
        Write-Host "==> Archivo temporal: $DumpFile" -ForegroundColor Cyan

        try {
            Write-Host "==> Exportando base de origen con pg_dump..." -ForegroundColor Cyan
            Invoke-PostgresDump -PgDumpExe $pgDumpExe -Connection $source -DumpFile $DumpFile

            Write-Host "==> Restaurando en la base de destino con pg_restore..." -ForegroundColor Cyan
            Invoke-PostgresRestore -PgRestoreExe $pgRestoreExe -Connection $target -DumpFile $DumpFile

            Write-Host "[OK] Copia completada correctamente." -ForegroundColor Green
        }
        finally {
            if (-not $KeepDump -and (Test-Path $DumpFile)) {
                Remove-Item $DumpFile -Force -ErrorAction SilentlyContinue
            }
            elseif ($KeepDump) {
                Write-Host "[i] Se ha conservado el dump en: $DumpFile" -ForegroundColor Gray
            }
        }
        return
    }
}
