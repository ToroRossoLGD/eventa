$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
New-Item -ItemType Directory -Force -Path 'artifacts' | Out-Null
$sourcePaths = @('src', 'docs', 'scripts', 'tests', 'pom.xml', 'README.md', 'Dockerfile', 'compose.yaml', '.dockerignore', '.gitignore', '.prettierignore', 'mvnw.cmd', 'package.json', 'package-lock.json', 'playwright.config.js')
Compress-Archive -Path $sourcePaths -DestinationPath 'artifacts/Eventa-izvorni-kod.zip' -Force
Write-Output 'Arhiva: artifacts/Eventa-izvorni-kod.zip'
