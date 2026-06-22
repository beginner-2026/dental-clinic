@echo off
cd /d "C:\Users\Геннадий\Documents\OpenCode\1"
echo Starting Gradle build...
call gradlew.bat :composeApp:clean :composeApp:run --no-daemon --no-configuration-cache
echo EXIT_CODE=%ERRORLEVEL%
