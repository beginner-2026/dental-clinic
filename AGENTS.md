# AGENTS.md — DentalClinic

## Shell / окружение

- **Shell сломан**: PowerShell `ChildProcess.kill` на каждой команде. Для сборки используй `cmd /c "..."`.
- **Git**: установлен portable в `C:\tools\git\bin\git.exe`. Не в PATH — добавляй вручную (`$env:Path += ";C:\tools\git\bin"`).
- **Remote**: `origin` → `github.com/beginner-2026/dental-clinic.git`, URL содержит PAT для push без пароля.
- **Две копии проекта**:
  - `C:\DentalClinic` — основной (сборка, запуск)
  - `C:\Users\Геннадий\Documents\OpenCode\1` — рабочая область (в ней Скриншоты, ТЗ, этот AGENTS.md)
  Все правки — только в `C:\DentalClinic`.

## Сборка / запуск

- **Desktop**: `gradlew.bat :composeApp:run` (или `run.dental` — лаунчер с проверками)
- **Nuclear rebuild**: `build_run.bat` — убивает java/gradle, удаляет БД, `clean --rerun-tasks`
- **Быстрая проверка компиляции**: `gradlew.bat :composeApp:compileKotlinDesktop`
- **Gradle home**: `-g C:\dental-cache` (передан в `gradlew.bat`)
- **Configuration cache**: включён (`gradle.properties`). При проблемах: `--no-configuration-cache`
- **JDK**: 17
- **Версии ключевые**: Kotlin 2.1.0, Compose Multiplatform 1.7.1, SQLDelight 2.0.2

## Архитектура

- **Single module** (`:composeApp`), три source set: `commonMain`, `androidMain`, `desktopMain`
- **Desktop entry**: `com.dental.MainKt` → `application { Window { App(...) } }`
- **Android entry**: `MainActivity` → `setContent { App(...) }`
- **DI**: ручной, через `remember {}` в `App.kt` + передача параметров
- **Навигация**: ручная, `enum AppScreen` в `App.kt` (без библиотек)
- **UI**: Material 3, только светлая тема, весь текст на русском (без i18n)
- **Состояние**: `MutableStateFlow`/`StateFlow` в ViewModel
- **Expect/actual**: `DatabaseDriverFactory` (2 реализации) и `getPlatformName()`

## База данных (SQLDelight)

- **Desktop БД**: `~/.dental-clinic/dental.db` — удаляется при каждом запуске (dev-режим)
- **Android БД**: `AndroidSqliteDriver` — обычное создание
- **Миграции**: файл `1.sqm` существует, но desktop использует `Schema.create()` (а не migrate) после удаления
- **Цены**: хранятся как `Long` в копейках (`1500_00` = 1500.00 руб)
- **Нумерация зубов**: FDI (11–18, 21–28, 31–38, 41–48)
- **Сиды**: 5 пациентов + 7 записей при первом запуске `SeedData.kt`

## Одонтограмма

- **Зубы рисуются кодом Canvas** в `OdontogramToothRenderer.kt` (~500 строк). Никаких PNG/SVG.
- 11 типов зубов (`ToothType` enum), маппинг FDI → тип в `getToothType(number)`
- Коронка + корень — независимо выбираемые части (`ToothPart.CROWN` / `ROOT`)
- Цвета протезов задаются через `CrownOptionColors`/`RootOptionColors` в `OdontogramCanvas.kt`
- `CrownOptionColors` и `RootOptionColors` — `public`, используются из `MedicalHistoryScreen.kt`
- При изменении формы зуба менять `drawUpperMolar()` / `drawUpperPremolar()` и соответствующий кейс в `drawCrownSelection()`
- При изменении корней править `drawEnhancedRoots()` и `drawRoot()`

## Тесты / CI

- **Тестов нет**: ни одного тестового файла, зависимости, source set
- **CI нет**: нет `.github/workflows/`

## Прочее

- **Git**: репозиторий есть (origin: github.com/beginner-2026/dental-clinic.git), но git не установлен
- **Gradle cache**: `.gradle-home/` — пустая директория в Git (не в .gitignore)
- **ProGuard**: включён (`isMinifyEnabled = true`) для release Android
- **Java module**: для desktop подключён `java.sql` (для JDBC/SQLite)
- **Документация**: только `ТЗ_2.docx` в корне (бинарный)
