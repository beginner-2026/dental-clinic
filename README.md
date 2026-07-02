Реализовано в DentalClinic — Синхронизация и бэкап
Зависимости: kotlinx-serialization-json, Ktor-client (okhttp/java), Ktor-server (netty)
Desktop БД: ~/.dental-clinic/dental.db — данные теперь сохраняются между запусками
Бэкап (JSON):
- Экспорт всех данных → ~/.dental-clinic/backups/backup_<timestamp>.json
- Импорт из JSON → восстановление всех данных
- Работает на Android (в context.filesDir/backups/)
Синхронизация:
- Desktop: Ktor сервер стартует авто (порт 9876)
- Android: подключается по IP, Push/Pull через настройки
- Endpoints: GET /api/sync/ping, POST /api/sync/pull, POST /api/sync/push
Экран «Настройки» в боковом меню: Backup, Restore, Push, Pull
APK: Builds/DentalClinic-debug.apk




