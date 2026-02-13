@REM Maven Wrapper script for Windows
@REM Downloads and caches Maven, then delegates to it
@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"

if not exist "%MAVEN_HOME%" (
    echo Downloading Maven...
    mkdir "%MAVEN_HOME%" 2>nul
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile '%TEMP%\maven.zip'; Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%MAVEN_HOME%' -Force; Remove-Item '%TEMP%\maven.zip'"
)

for /f "delims=" %%i in ('dir /s /b "%MAVEN_HOME%\mvn.cmd" 2^>nul') do set "MVN_CMD=%%i"

if "%MVN_CMD%"=="" (
    echo Error: Maven executable not found
    exit /b 1
)

"%MVN_CMD%" %*
