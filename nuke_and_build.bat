@echo off
echo Step 1: Killing ALL Java and Gradle processes...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM gradle.exe 2>nul
timeout /T 5 /NOBREAK >nul

echo Step 2: Deleting ALL caches...
rmdir /s /q "C:\Users\Геннадий\.dental-clinic" 2>nul
rmdir /s /q "C:\Users\Геннадий\Documents\OpenCode\1\.gradle" 2>nul
rmdir /s /q "C:\Users\Геннадий\Documents\OpenCode\1\composeApp\build" 2>nul
rmdir /s /q "C:\Users\Геннадий\Documents\OpenCode\1\.kotlin" 2>nul
rmdir /s /q "C:\Users\Геннадий\Documents\OpenCode\1\build" 2>nul

echo Step 3: Killing processes again...
taskkill /F /IM java.exe 2>nul
taskkill /F /IM gradle.exe 2>nul
timeout /T 3 /NOBREAK >nul

echo Step 4: Building from scratch...
cd /d "C:\Users\Геннадий\Documents\OpenCode\1"
call gradlew.bat :composeApp:clean :composeApp:run --no-daemon --no-configuration-cache --rerun-tasks
echo EXIT_CODE=%ERRORLEVEL%
