# IT EventParser

**IT EventParser** — Android-приложение для агрегации и просмотра IT-мероприятий (митапы,
конференции, воркшопы, хакатоны и т.д.). В текущей версии данные собираются из публичных
Telegram-каналов через web-превью.

Архитектура проекта: **Clean Architecture** + **MVI** (Orbit MVI).

## 📱 Возможности

- **Лента событий**: список предстоящих мероприятий с pull-to-refresh.
- **Фильтры и сортировка**:
    - категория,
    - быстрые фильтры: «Сегодня», «На этой неделе», «Онлайн», «Бесплатные»,
    - сортировка: по дате / по названию.
- **Детали события**:
    - открытие ссылки,
    - «поделиться»,
    - добавить в календарь,
    - экспорт в `.ics` (iCalendar) и отправка файла.
- **Избранное**:
    - сохранение в локальную БД,
    - напоминания за **24 часа** и за **1 час** до начала (WorkManager one-time).
- **Импорт / экспорт избранного**:
    - экспорт в JSON (`FavoritesExport`, со схемой `schemaVersion`),
    - импорт из JSON с валидацией версии.
- **Уведомления**:
    - напоминания о событиях из избранного,
    - уведомление о «новых найденных событиях» после фонового обновления.
- **Онбординг**: показывается при первом запуске.
- **Настройки**: тема (светлая/тёмная/как в системе), «очистить кэш» UI-настроек, переход в
  системные настройки уведомлений.
- **Deep link**: `eventparser://event/<eventId>` открывает экран события.

## 🛠 Технологический стек

- **Язык**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Архитектура**: Clean Architecture + MVI (Orbit MVI)
- **DI**: Koin
- **Асинхронность**: Coroutines + Flow
- **Хранилище**:
    - Room (SQLite) — события/избранное,
    - DataStore Preferences — настройки и сохранение UI-состояния фильтров/сортировки.
- **Сеть/парсинг**:
    - Jsoup — парсинг Telegram Web (`t.me/s/...`),
    - Retrofit/OkHttp — подключены как сетевой стек и используются в обработке сетевых
      ошибок/ретраев.
- **Фоновая работа**: WorkManager (периодическое обновление + точечные напоминания)
- **Изображения**: Coil
- **Логирование**: Timber
- **Навигация**: Navigation Compose

## 📂 Структура проекта

```text
ru.purebytestudio.eventparser
├── data/
│   ├── crash/                 # CrashHandler
│   ├── export/                # .ics экспорт (IcsExporter)
│   ├── io/                    # SAF-IO (DocumentTextStorage)
│   ├── local/                 # Room + DataStore
│   │   ├── dao/
│   │   ├── database/
│   │   ├── entity/
│   │   └── preferences/
│   ├── notification/          # NotificationManager + scheduler
│   ├── platform/              # Android-реализации platform-интерфейсов
│   ├── remote/                # агрегатор + парсеры
│   │   └── parser/            # TelegramParser + pipeline
│   ├── repository/            # реализации репозиториев
│   ├── service/               # дедупликация событий
│   ├── util/                  # вспомогательные утилиты
│   └── worker/                # WorkManager workers
├── di/                        # Koin модули
├── domain/                    # чистая бизнес-логика
│   ├── model/
│   ├── repository/
│   ├── usecase/
│   └── util/
├── navigation/                # NavHost + bottom bar
├── platform/                  # интерфейсы (NetworkStatusProvider и др.)
├── presentation/              # ViewModel (Orbit MVI)
└── ui/                        # Compose UI (screens/components/theme)
```

## 🚀 Сборка и запуск

1) Открыть проект в Android Studio

2) Запустить на устройстве/эмуляторе:

- **minSdk**: 30
- **targetSdk/compileSdk**: 36

## 🏗 Архитектура и решения

Детальное описание архитектуры — в [ARCHITECTURE.md](ARCHITECTURE.md).

---
Developed with ❤️ by Purebyte Studio
---