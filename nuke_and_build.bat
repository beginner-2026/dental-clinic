@echo off
echo Step 1: Killing ALL Java and Gradle processes...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM gradle.exe 2>nul
timeout /T 5 /NOBREAK >nul

echo Step 2: Deleting ALL caches...
rmdir /s /q "C:\Users\Геннадий\.dental-clinic" 2>nul
rmdir /s /q "C:\OpenCode\DentalClinic\.gradle" 2>nul
rmdir /s /q "C:\OpenCode\DentalClinic\composeApp\build" 2>nul
rmdir /s /q "C:\OpenCode\DentalClinic\.kotlin" 2>nul
rmdir /s /q "C:\OpenCode\DentalClinic\build" 2>nul

echo Step 3: Killing processes again...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM gradle.exe 2>nul
timeout /T 3 /NOBREAK >nul

echo Step 4: Building from scratch...
cd /d "C:\OpenCode\DentalClinic"
call gradlew.bat :composeApp:clean :composeApp:run --no-daemon --no-configuration-cache --rerun-tasks
echo EXIT_CODE=%ERRORLEVEL%
