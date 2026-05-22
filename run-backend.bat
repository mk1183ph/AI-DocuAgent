@echo off
setlocal

cd /d "%~dp0"

if not exist "storage" mkdir "storage"
if not exist "storage\generated" mkdir "storage\generated"
if not exist "templates" mkdir "templates"

cd backend
call gradlew.bat bootRun
