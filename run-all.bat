@echo off
setlocal

cd /d "%~dp0"

if not exist "storage" mkdir "storage"
if not exist "storage\generated" mkdir "storage\generated"
if not exist "templates" mkdir "templates"

echo Building frontend and local Spring Boot jar...
cd backend
call gradlew.bat packageLocal
if errorlevel 1 exit /b 1

cd /d "%~dp0"
set "APP_JAR="
for %%J in (backend\build\libs\*.jar) do set "APP_JAR=%%~fJ"
if not defined APP_JAR (
  echo No Spring Boot jar found under backend\build\libs.
  exit /b 1
)

echo Starting DocuAgent Local at http://localhost:8080
java -jar "%APP_JAR%" ^
  --spring.datasource.url=jdbc:sqlite:storage/app.db ^
  --app.storage.templates-dir=templates ^
  --app.storage.generated-dir=storage/generated
