# Build better-glue
#
# Usage:  powershell -ExecutionPolicy Bypass -File .\build.ps1
#         powershell -ExecutionPolicy Bypass -File .\build.ps1 -JavaHome "C:\path\to\jdk21"
#         powershell -ExecutionPolicy Bypass -File .\build.ps1 -Proxy "http://127.0.0.1:7890"
#
# Notes:
#  - Requires a JDK 21. Default lookup: D:\java21, then %JAVA_HOME%, then PATH.
#  - Gradle cache lives inside the project (.gradle-home) so the folder is self-contained.
#  - Output: build\libs\better-glue-1.0.0.jar
#  - -Proxy sets the JVM proxy properties for this run; needed when maven.fabricmc.net
#    is unreachable directly. Gradle ignores the Windows system proxy and HTTP_PROXY.
#  - This file is intentionally ASCII-only + UTF-8 with BOM, so Windows PowerShell 5.1
#    can parse it no matter what the system code page is.

param(
    [string]$JavaHome = "",
    [string]$Proxy = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

function Find-Jdk {
    param([string]$Preferred)

    $candidates = New-Object System.Collections.Generic.List[string]
    if ($Preferred) { $candidates.Add($Preferred) }
    if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }
    $candidates.Add("D:\java21")
    $candidates.Add("C:\Program Files\Java\latest\jdk-21")

    foreach ($candidate in $candidates) {
        if (-not $candidate) { continue }
        $javac = Join-Path $candidate "bin\javac.exe"
        if (Test-Path $javac) { return $candidate }
    }

    $onPath = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($onPath) {
        return (Split-Path -Parent (Split-Path -Parent $onPath.Source))
    }
    return $null
}

$jdk = Find-Jdk -Preferred $JavaHome
if (-not $jdk) {
    Write-Host "ERROR: no JDK 21 found." -ForegroundColor Red
    Write-Host "Pass one explicitly:  .\build.ps1 -JavaHome `"C:\path\to\jdk-21`""
    exit 1
}

$env:JAVA_HOME = $jdk
$env:PATH = (Join-Path $jdk "bin") + ";" + $env:PATH
if (-not $env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME = Join-Path $root ".gradle-home"
}

Write-Host "JAVA_HOME        = $($env:JAVA_HOME)"
Write-Host "GRADLE_USER_HOME = $($env:GRADLE_USER_HOME)"

$gradleArgs = @("--no-daemon", "--console=plain")
if ($Proxy) {
    try {
        $proxyUri = [System.Uri]$Proxy
    } catch {
        Write-Host "ERROR: bad -Proxy value: $Proxy" -ForegroundColor Red
        exit 1
    }
    if (-not $proxyUri.Host -or $proxyUri.Port -le 0) {
        Write-Host "ERROR: -Proxy needs a host and port, e.g. http://127.0.0.1:7890" -ForegroundColor Red
        exit 1
    }
    $gradleArgs += "-Dhttp.proxyHost=$($proxyUri.Host)"
    $gradleArgs += "-Dhttp.proxyPort=$($proxyUri.Port)"
    $gradleArgs += "-Dhttps.proxyHost=$($proxyUri.Host)"
    $gradleArgs += "-Dhttps.proxyPort=$($proxyUri.Port)"
    Write-Host "PROXY            = $($proxyUri.Host):$($proxyUri.Port)"
}
Write-Host ""

$gradlew = Join-Path $root "gradlew.bat"

# Windows PowerShell 5.1 turns a native program's stderr into terminating errors when
# $ErrorActionPreference is Stop, and Gradle always writes something to stderr.
# Merge the streams explicitly and check the exit code ourselves.
$previous = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$gradleArgs += "build"
& $gradlew @gradleArgs 2>&1 | ForEach-Object { Write-Host $_ }
$gradleExit = $LASTEXITCODE
$ErrorActionPreference = $previous

if ($gradleExit -ne 0) {
    Write-Host ""
    Write-Host "BUILD FAILED (exit=$gradleExit)" -ForegroundColor Red
    exit $gradleExit
}

$jar = Get-ChildItem (Join-Path $root "build\libs\*.jar") -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch "sources" } |
    Select-Object -First 1

if (-not $jar) {
    Write-Host "Build finished but no jar was produced." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "OK: $($jar.FullName)" -ForegroundColor Green
Write-Host "Drop it into .minecraft\mods (needs Fabric Loader 0.20.1+ and Fabric API for 1.21.10)."
