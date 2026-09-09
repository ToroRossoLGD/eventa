$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$toolsDir = Join-Path $projectRoot '.tools'
New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
if (-not (Test-Path (Join-Path $toolsDir 'jdk'))) {
    Invoke-WebRequest -Uri 'https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.zip' -OutFile (Join-Path $toolsDir 'jdk.zip')
    Expand-Archive -LiteralPath (Join-Path $toolsDir 'jdk.zip') -DestinationPath (Join-Path $toolsDir 'java-unpack') -Force
    $jdkFolder = Get-ChildItem (Join-Path $toolsDir 'java-unpack') -Directory | Select-Object -First 1
    Move-Item -LiteralPath $jdkFolder.FullName -Destination (Join-Path $toolsDir 'jdk')
}
if (-not (Test-Path (Join-Path $toolsDir 'apache-maven-3.9.11'))) {
    Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip' -OutFile (Join-Path $toolsDir 'maven.zip')
    Expand-Archive -LiteralPath (Join-Path $toolsDir 'maven.zip') -DestinationPath $toolsDir -Force
}
Write-Output 'Java i Maven su spremni u .tools direktorijumu.'
