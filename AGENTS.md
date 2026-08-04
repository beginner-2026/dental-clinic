# AGENTS.md — DentalClinic

## Основное
- отвечать только по-русски

## Сборка / запуск

- **Desktop портативная** (папка с JRE): `gradlew :composeApp:createDistributable`
- **Android APK**: `gradlew :composeApp:assembleDebug` / `assembleRelease`
- **Проверка компиляции (быстро)**: `./gradlew :composeApp:compileKotlinDesktop`
- **Результат всех сборок копировать в `Builds/`**

## Архитектура

- **Single module** (`:composeApp`), три source set: `commonMain`, `androidMain`, `desktopMain`
- **Desktop entry**: `com.dental.MainKt` → `application { Window { App(...) } }`
- **Android entry**: `MainActivity` → `setContent { App(...) }`
- **DI**: ручной, через `remember {}` в `App.kt` + передача параметров
- **Навигация**: ручная, `enum AppScreen` в `App.kt` (без библиотек)
- **UI**: Material 3, только светлая тема, весь текст на русском (без i18n)
- **Состояние**: `MutableStateFlow`/`StateFlow` в ViewModel
- **Expect/actual**: `DatabaseDriverFactory`, `getPlatformName()`, `BackupStorage`, `platformStartSyncServer`

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

## Синхронизация и резервное копирование

- **BackupManager** (`commonMain`): экспорт всех таблиц БД в `BackupData` (JSON) и импорт обратно
  - `exportAll()` проставляет `lastSyncedAt` (текущий timestamp) и `syncedByDevice` (`getPlatformName()`)
- **BackupStorage** (expect/actual): сохранение/загрузка JSON-файла на устройстве
  - Desktop: `~/.dental-clinic/backups/backup_<timestamp>.json`
  - Android: `context.filesDir/backups/backup_<timestamp>.json`
- **CloudSyncStorage** (expect/actual): синхронизация JSON-файла через облачные хранилища
  - **Desktop** (`CloudSyncStorage.desktop.kt`):
    - Автодетект: `~/Google Drive`, `~/My Drive`, `~/OneDrive`, `~/Dropbox`, `~/Yandex.Disk`
    - Также проверяет диски `G:`, `H:`, `I:`, `J:`, `K:` для `My Drive`/`Google Drive`
    - Если не найдено — использует `~/.dental-clinic/cloud-sync/`
    - Файл: `sync.json` в папке `DentalClinic/`
    - `openFolder()` — открывает папку в проводнике через `Desktop.getDesktop().open()`
  - **Android** (`CloudSyncStorage.android.kt`):
    - Использует SAF через `ContentResolver` (файл по URI из SharedPreferences)
    - `SyncFilePicker` использует `ActivityResultContracts.CreateDocument("application/json")` с `takePersistableUriPermission`
    - На Android SAF-пикер поддерживает Google Drive нативно
  - **Общие методы**: `save()`, `load()`, `statusText()`, `isConfigured`, `configure(path)`, `getLastSyncTimestamp()`, `getDeviceName()`, `openFolder()`
- **BackupData** (`BackupData.kt`): `version = 2`, добавлены поля `lastSyncedAt: Long?` и `syncedByDevice: String?`
- **Экран Settings**: создание/восстановление бэкапа (JSON), Push/Pull синхронизация с облаком, кнопка «Открыть папку»

## Зависимости (новые)

- `kotlinx-serialization-json` — сериализация/десериализация BackupData
- `ktor-client-core`, `ktor-client-okhttp` (Android), `ktor-client-java` (Desktop) — HTTP-клиент
- `ktor-server-core`, `ktor-server-netty`, `ktor-server-status-pages`, `ktor-server-content-negotiation` (Desktop) — HTTP-сервер
- `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json` — JSON через Content Negotiation

## Тесты / CI

- **Тестов нет**: ни одного тестового файла, зависимости, source set
- **CI нет**: нет `.github/workflows/`

## Прочее

- **Git**: репозиторий есть (origin: github.com/beginner-2026/dental-clinic.git)
- **Gradle cache**: `.gradle-home/` — пустая директория в Git (не в .gitignore)
- **ProGuard**: включён (`isMinifyEnabled = true`) для release Android
- **Java module**: для desktop подключён `java.sql` (для JDBC/SQLite)
- **Документация**: только `ТЗ_2.docx` в корне (бинарный)
