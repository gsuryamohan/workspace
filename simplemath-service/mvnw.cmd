@REM Maven Wrapper startup script for Windows
@echo off
setlocal

set BASE_DIR=%~dp0
set WRAPPER_DIR=%BASE_DIR%\.mvn\wrapper
set PROPS_FILE=%WRAPPER_DIR%\maven-wrapper.properties
set WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar

if not exist "%PROPS_FILE%" (
  echo Missing %PROPS_FILE% 1>&2
  exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%PROPS_FILE%") do (
  if "%%A"=="wrapperUrl" set WRAPPER_URL=%%B
)

if not exist "%WRAPPER_JAR%" (
  if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'" || exit /b 1
)

java -Dmaven.multiModuleProjectDirectory="%BASE_DIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*

