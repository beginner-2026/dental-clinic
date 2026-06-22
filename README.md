# Dental Clinic

Кроссплатформенное приложение для ведения стоматологической клиники (Android + Desktop).

## Стек

- **Kotlin Multiplatform** 2.1.0
- **Compose Multiplatform** 1.7.1 (Material 3)
- **SQLDelight** 2.0.2
- **Kotlinx Coroutines** 1.9.0
- **Kotlinx Datetime** 0.6.1

## Запуск

### Desktop

```bash
gradlew.bat :composeApp:run
```

Или `run.dental` — лаунчер с проверками.

### Android

Открыть в Android Studio, запустить `composeApp` на эмуляторе или устройстве.

## Структура проекта

```
composeApp/src/
├── commonMain/kotlin/com/dental/
│   ├── App.kt                  — точка входа UI
│   ├── model/                   — модели данных
│   ├── data/                    — репозитории, БД
│   └── ui/                      — экраны и компоненты
│       ├── odontogram/          — одонтограмма (Canvas-рисование зубов)
│       ├── calendar/            — календарь записей
│       ├── patient/             — список пациентов
│       ├── medicalHistory/      — история болезни
│       └── invoice/             — прайс-лист
├── androidMain/                 — Android-специфичный код
└── desktopMain/                 — Desktop-специфичный код (JDBC SQLite)
```

## Одонтограмма

Зубы рисуются программно через Canvas Compose (`OdontogramToothRenderer.kt`).
11 типов зубов, градиентная 3D-заливка, 2 выбираемые части (коронка/корень).

## База данных

SQLDelight, 10 таблиц. Desktop: SQLite через JDBC. Android: AndroidSqliteDriver.
В development-режиме БД на Desktop удаляется при каждом запуске.

## Лицензия

2026 Dental Clinic App
