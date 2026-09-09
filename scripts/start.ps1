param([ValidateSet('demo','mysql')][string]$Profile = 'demo', [int]$Port = 8080)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
if (-not (Test-Path '.tools/jdk/bin/java.exe') -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
    & "$PSScriptRoot/bootstrap.ps1"
}
& "$projectRoot/mvnw.cmd" -B package -DskipTests
if ($LASTEXITCODE -ne 0) { throw 'Kompilacija nije uspela.' }
$javaPath = if (Test-Path '.tools/jdk/bin/java.exe') { Join-Path $projectRoot '.tools/jdk/bin/java.exe' } else { 'java' }
& $javaPath -jar target/eventa-1.0.0.jar "--spring.profiles.active=$Profile" "--server.port=$Port"
