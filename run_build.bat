@echo off
cd /d "C:\OpenCode\DentalClinic"
echo Starting Gradle build...
call gradlew.bat :composeApp:clean :composeApp:run --no-daemon --no-configuration-cache
echo EXIT_CODE=%ERRORLEVEL%
