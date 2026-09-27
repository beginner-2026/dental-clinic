@echo off
chcp 1251 >nul
title DentalClinic — сборка и запуск

echo [1/6] Завершаю старые процессы...
taskkill /F /IM gradle.exe 2>nul
taskkill /F /IM java.exe 2>nul
timeout /T 3 /NOBREAK >nul

echo [2/6] Очищаю кэш сборки...
cd /d "%~dp0"
rmdir /s /q ".gradle" 2>nul
rmdir /s /q "composeApp\build" 2>nul
rmdir /s /q ".kotlin" 2>nul

echo [3/6] Проверяю Java...
if not defined JAVA_HOME (
    for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\JavaSoft\JDK" /s 2^>nul ^| findstr "JavaHome"') do (
        if exist "%%b\bin\java.exe" set "JAVA_HOME=%%b"
    )
    if not defined JAVA_HOME (
        for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\JavaSoft\Java Development Kit" /s 2^>nul ^| findstr "JavaHome"') do (
            if exist "%%b\bin\java.exe" set "JAVA_HOME=%%b"
        )
    )
    if not defined JAVA_HOME (
        for /f "tokens=2*" %%a in ('reg query "HKLM\SOFTWARE\JavaSoft\JRE" /s 2^>nul ^| findstr "JavaHome"') do (
            if exist "%%b\bin\java.exe" set "JAVA_HOME=%%b"
        )
    )
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo    JAVA_HOME = %JAVA_HOME%
) else (
    echo    Java не найдена в реестре. Ищу в Program Files...
    if exist "C:\Program Files\Java\*" (
        for /d %%d in ("C:\Program Files\Java\*") do (
            if exist "%%d\bin\java.exe" set "JAVA_HOME=%%d"
        )
    )
    if defined JAVA_HOME (
        set "PATH=%JAVA_HOME%\bin;%PATH%"
        echo    JAVA_HOME = %JAVA_HOME%
    )
)

java.exe -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ОШИБКА] Java не найдена или не работает.
    echo.
    echo Установите JDK 17+ и задайте переменную JAVA_HOME, например:
    echo    setx JAVA_HOME "C:\Program Files\Java\jdk-17"
    echo.
    pause
    exit /b 1
)

echo [4/6] Запускаю Gradle сборку...
call gradlew.bat :composeApp:run --no-daemon --no-configuration-cache
set EXITCODE=%ERRORLEVEL%

if %EXITCODE% neq 0 (
    echo.
    echo [ОШИБКА] Сборка завершилась с кодом %EXITCODE%
    pause
)

exit /b %EXITCODE%
