@echo off
taskkill /F /IM java.exe 2>nul
taskkill /F /IM gradle.exe 2>nul
timeout /T 3 /NOBREAK >nul
rmdir /s /q "C:\Users\Геннадий\.dental-clinic" 2>nul
cd /d "C:\Users\Геннадий\Documents\OpenCode\1"
rmdir /s /q ".gradle" 2>nul
rmdir /s /q "composeApp\build" 2>nul
rmdir /s /q ".kotlin" 2>nul
call gradlew.bat :composeApp:clean :composeApp:run --no-daemon --no-configuration-cache
exit /b %ERRORLEVEL%
