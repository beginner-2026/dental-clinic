# AGENTS.md — DentalClinic

## Основное
- отвечать только по-русски

## Сборка / запуск

- **Desktop портативная** (папка с JRE): `gradlew :composeApp:createDistributable`
- **Desktop EXE-установщик**: `gradlew :composeApp:packageExe`
- **Desktop толстый JAR**: `gradlew :composeApp:packageUberJarForCurrentOS`
- **Android APK**: `gradlew :composeApp:assembleDebug` / `assembleRelease`
- **Проверка компиляции (быстро)**: `./gradlew :composeApp:compileKotlinDesktop` — завершается сразу без открытия окна
- **Запуск desktop в фоне** (не блокирует терминал): `Start-Process -FilePath ".\gradlew" -ArgumentList ":composeApp:run"` — окно живёт отдельно, закрывается крестиком
- **Результат всех сборок копировать в `Builds/`**

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

- **Git**: репозиторий есть (origin: github.com/beginner-2026/dental-clinic.git)
- **Gradle cache**: `.gradle-home/` — пустая директория в Git (не в .gitignore)
- **ProGuard**: включён (`isMinifyEnabled = true`) для release Android
- **Java module**: для desktop подключён `java.sql` (для JDBC/SQLite)
- **Документация**: только `ТЗ_2.docx` в корне (бинарный)
