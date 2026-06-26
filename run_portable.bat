@echo off
chcp 1251 >nul
title DentalClinic — запуск portable

set "EXE=C:\OpenCode\DentalClinic\Builds\DentalClinic\DentalClinic.exe"

if not exist "%EXE%" (
    echo.
    echo Сборка не найдена! Запустите build_and_run_portable.bat
    echo чтобы собрать приложение.
    echo.
    pause
    exit /b 1
)

echo Запуск DentalClinic...
start "" "%EXE%"
