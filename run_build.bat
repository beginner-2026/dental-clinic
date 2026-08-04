@echo off
cd /d "C:\OpenCode\DentalClinic"
echo Starting Gradle build...
call gradlew.bat :composeApp:run
echo EXIT_CODE=%ERRORLEVEL%
pause
