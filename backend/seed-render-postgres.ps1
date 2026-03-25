param(
    [string]$DbHost,

    [string]$ExternalUrl,

    [int]$Port = 5432,

    [string]$Database = "tfg_8pmy",

    [string]$User = "tfg",

    [string]$Password,

    [switch]$DisableSsl
)

$ErrorActionPreference = "Stop"

function Get-PlainTextFromSecureString {
    param([Security.SecureString]$SecureString)

    $BSTR = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($BSTR)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
    }
}

function Parse-ExternalPostgresUrl {
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
        throw "ExternalUrl no es valida. Usa formato postgresql://user:password@host:port/db"
    }

    return [pscustomobject]@{
        Scheme = $match.Groups["scheme"].Value.ToLowerInvariant()
        User   = [Uri]::UnescapeDataString($match.Groups["user"].Value)
        Pass   = [Uri]::UnescapeDataString($match.Groups["pass"].Value)
        Host   = $match.Groups["host"].Value
        Port   = $match.Groups["port"].Value
        Db     = [Uri]::UnescapeDataString($match.Groups["db"].Value)
        Query  = $match.Groups["query"].Value
    }
}

function Build-JdbcUrlFromExternal {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,

        [Parameter(Mandatory = $true)]
        [string]$FallbackDatabase,

        [Parameter(Mandatory = $true)]
        [int]$FallbackPort,

        [Parameter(Mandatory = $true)]
        [string]$SslMode
    )

    $parsed = Parse-ExternalPostgresUrl -Url $Url

    $dbHostFromUrl = $parsed.Host
    if ([string]::IsNullOrWhiteSpace($dbHostFromUrl)) {
        throw "ExternalUrl no contiene host valido"
    }

    $port = if (-not [string]::IsNullOrWhiteSpace($parsed.Port)) { [int]$parsed.Port } else { $FallbackPort }

    $dbFromUrl = $parsed.Db
    $db = if ([string]::IsNullOrWhiteSpace($dbFromUrl)) { $FallbackDatabase } else { $dbFromUrl }

    $queryParams = @{}
    if (-not [string]::IsNullOrWhiteSpace($parsed.Query)) {
        $rawQuery = $parsed.Query
        foreach ($pair in $rawQuery -split '&') {
            if ([string]::IsNullOrWhiteSpace($pair)) { continue }
            $parts = $pair -split '=', 2
            $key = [Uri]::UnescapeDataString($parts[0])
            $value = if ($parts.Length -eq 2) { [Uri]::UnescapeDataString($parts[1]) } else { "" }
            $queryParams[$key] = $value
        }
    }

    if (-not $queryParams.ContainsKey("sslmode")) {
        $queryParams["sslmode"] = $SslMode
    }

    $queryString = ($queryParams.GetEnumerator() | ForEach-Object {
            "{0}={1}" -f [Uri]::EscapeDataString($_.Key), [Uri]::EscapeDataString($_.Value)
        }) -join '&'

    return "jdbc:postgresql://${dbHostFromUrl}`:${port}/${db}?${queryString}"
}

if (-not [string]::IsNullOrWhiteSpace($ExternalUrl)) {
    $parsed = Parse-ExternalPostgresUrl -Url $ExternalUrl

    $DbHost = $parsed.Host
    if (-not [string]::IsNullOrWhiteSpace($parsed.Port)) {
        $Port = [int]$parsed.Port
    }

    $dbFromUrl = $parsed.Db
    if (-not [string]::IsNullOrWhiteSpace($dbFromUrl)) {
        $Database = $dbFromUrl
    }

    if ([string]::IsNullOrWhiteSpace($User) -or $User -eq "tfg") {
        if (-not [string]::IsNullOrWhiteSpace($parsed.User)) {
            $User = $parsed.User
        }
        if ([string]::IsNullOrWhiteSpace($Password) -and -not [string]::IsNullOrWhiteSpace($parsed.Pass)) {
            $Password = $parsed.Pass
        }
    }
}

if ([string]::IsNullOrWhiteSpace($DbHost)) {
    throw "Debes indicar -DbHost o -ExternalUrl"
}

if ($DbHost -match '[/?]') {
    throw "DbHost no puede contener '/' ni '?'. Usa solo el host o pasa -ExternalUrl completo."
}

if ([string]::IsNullOrWhiteSpace($Password)) {
    $secure = Read-Host "PostgreSQL password" -AsSecureString
    $Password = Get-PlainTextFromSecureString -SecureString $secure
}

$sslMode = if ($DisableSsl) { "disable" } else { "require" }
$jdbcUrl = if (-not [string]::IsNullOrWhiteSpace($ExternalUrl)) {
    Build-JdbcUrlFromExternal -Url $ExternalUrl -FallbackDatabase $Database -FallbackPort $Port -SslMode $sslMode
}
else {
    "jdbc:postgresql://${DbHost}`:${Port}/${Database}?sslmode=${sslMode}"
}

Write-Host "==> JDBC URL: $jdbcUrl"
Write-Host "==> Running seeder with non-prod profile behavior"

$previousEnv = @{}
$keys = @(
    "DATABASE_URL",
    "DATABASE_USER",
    "DATABASE_PASSWORD",
    "SPRING_DATASOURCE_URL",
    "SPRING_DATASOURCE_USERNAME",
    "SPRING_DATASOURCE_PASSWORD",
    "SERVER_PORT",
    "SPRING_PROFILES_ACTIVE",
    "SPRING_FLYWAY_ENABLED",
    "SPRING_JPA_HIBERNATE_DDL_AUTO",
    "ONEDRIVE_ENABLED",
    "CLOUDINARY_ENABLED"
)

foreach ($k in $keys) {
    $previousEnv[$k] = [Environment]::GetEnvironmentVariable($k, "Process")
}

try {
    [Environment]::SetEnvironmentVariable("DATABASE_URL", $jdbcUrl, "Process")
    [Environment]::SetEnvironmentVariable("DATABASE_USER", $User, "Process")
    [Environment]::SetEnvironmentVariable("DATABASE_PASSWORD", $Password, "Process")
    [Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_URL", $jdbcUrl, "Process")
    [Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_USERNAME", $User, "Process")
    [Environment]::SetEnvironmentVariable("SPRING_DATASOURCE_PASSWORD", $Password, "Process")
    [Environment]::SetEnvironmentVariable("SERVER_PORT", "0", "Process")

    [Environment]::SetEnvironmentVariable("SPRING_PROFILES_ACTIVE", $null, "Process")
    [Environment]::SetEnvironmentVariable("SPRING_FLYWAY_ENABLED", "false", "Process")
    [Environment]::SetEnvironmentVariable("SPRING_JPA_HIBERNATE_DDL_AUTO", "update", "Process")
    [Environment]::SetEnvironmentVariable("ONEDRIVE_ENABLED", "false", "Process")
    [Environment]::SetEnvironmentVariable("CLOUDINARY_ENABLED", "false", "Process")

    Push-Location $PSScriptRoot
    & .\mvnw.cmd spring-boot:run
}
finally {
    Pop-Location
    foreach ($k in $keys) {
        [Environment]::SetEnvironmentVariable($k, $previousEnv[$k], "Process")
    }
}
