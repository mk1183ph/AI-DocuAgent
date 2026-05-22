@echo off
setlocal

cd /d "%~dp0"

if not exist "storage" mkdir "storage"
if not exist "storage\generated" mkdir "storage\generated"
if not exist "templates" mkdir "templates"

start "DocuAgent Backend" cmd /k "%~dp0run-backend.bat"
start "DocuAgent Frontend" cmd /k "%~dp0run-frontend.bat"

echo DocuAgent Local dev servers are starting.
echo Backend:  http://localhost:8080/api/health
echo Frontend: http://localhost:5173
