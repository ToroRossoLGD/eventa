@echo off
setlocal
if exist "%~dp0.tools\jdk\bin\java.exe" set "JAVA_HOME=%~dp0.tools\jdk"
if exist "%~dp0.tools\apache-maven-3.9.11\bin\mvn.cmd" (
  call "%~dp0.tools\apache-maven-3.9.11\bin\mvn.cmd" -Dmaven.repo.local="%~dp0.tools\repository" %*
) else (
  call mvn %*
)
exit /b %errorlevel%
