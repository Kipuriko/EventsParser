# Архитектура проекта IT EventParser

Этот документ описывает архитектурные решения **финальной** версии приложения и соответствует
текущей реализации в репозитории.

## Общая идея

**IT EventParser** агрегирует IT-мероприятия из источников (публичные Telegram-каналы через
web-превью) и предоставляет:

- список с фильтрами/сортировкой,
- экран деталей,
- избранное + напоминания,
- экспорт/импорт избранного в JSON,
- экспорт одного события в `.ics` (iCalendar) и добавление в системный календарь.

Архитектура: **Clean Architecture** + **MVI** (Orbit MVI).

---

## Слои и зависимости (Clean Architecture)

### Domain (`ru.purebytestudio.eventparser.domain`)

**Чистый Kotlin**, не зависит от Android SDK.

- **Модели**: `Event`, `EventCategory`, `EventType`, `QuickFilter`, `EventSortType`,
  `FavoritesExport`, `EventRefreshSummary`.
- **Контракты**: `EventRepository`, `UserPreferencesRepository`, а также платформенные интерфейсы в
  `ru.purebytestudio.eventparser.platform`.
- **UseCase**: `GetFilteredEventsUseCase`, `RefreshEventsUseCase`, `ToggleFavoriteUseCase`,
  `GetEventByIdUseCase`, `GetFavoriteEventsUseCase`, `ImportFavoriteEventsUseCase`,
  `CleanupPastEventsUseCase`.

**Правило**: Domain не импортирует классы Android/Compose/Room/Retrofit.

---

### Presentation (`ru.purebytestudio.eventparser.presentation`)

**ViewModel** и MVI-контейнеры Orbit: `ContainerHost<State, SideEffect>`.

- **State** — неизменяемое состояние экрана.
- **SideEffect** — одноразовые события (навигация, тосты/снэкбары и т.п.).
- **Intent** — публичные методы ViewModel (например, `refreshEvents()`, `toggleFavorite()`), внутри
  которых выполняются `reduce { ... }` и `postSideEffect(...)`.

---

### UI (`ru.purebytestudio.eventparser.ui`)

Jetpack Compose UI:

- экраны: `EventsScreen`, `FavoritesScreen`, `EventDetailScreen`, `SettingsScreen`,
  `OnboardingScreen`;
- компоненты: карточки, чипсы, шимер, пустые состояния, текст со ссылками и т.д.

UI подписывается на Orbit-`State` и обрабатывает `SideEffect` (например, навигация, share-intent,
snackbar).

---

### Data (`ru.purebytestudio.eventparser.data`)

Слой данных реализует репозитории и работу с Android-инфраструктурой:

- **Local**: Room (`data.local.*`) + DataStore Preferences (
  `data.local.preferences.AppPreferences`).
- **Remote**: парсеры (`data.remote.*`, `data.remote.parser.*`) — сейчас `TelegramParser` на базе
  `Jsoup`.
- **Repository impl**: `data.repository.EventRepositoryImpl`,
  `data.repository.UserPreferencesRepositoryImpl`.
- **Background**: воркеры WorkManager (`data.worker.*`).
- **Notifications**: `data.notification.*`.
- **Platform impl**: `data.platform.*` — реализации интерфейсов из
  `ru.purebytestudio.eventparser.platform`.
- **Export/IO**: `data.export.IcsExporter`, `data.io.DocumentTextStorage`.
- **Стабильность**: `data.crash.CrashHandler`, `data.platform.ExponentialBackoffRetryPolicy`.

---

## Хранилище данных

### Room (SQLite)

- **БД**: `data.local.database.AppDatabase` (версия 4), таблица `events` (
  `data.local.entity.EventEntity`).
- **DAO**: `data.local.dao.EventDao` возвращает `Flow<List<EventEntity>>` для реактивного UI.
- **Индексы**: по `category`, `source`, `eventType`, `isFavorite`, `dateTime`, `isFree`, а также по
  `title/description`.
- **Upsert**: вставка событий — `OnConflictStrategy.REPLACE` по `id`.
- **Очистка**: `deletePastEvents()` удаляет прошедшие события, кроме избранных.
- **Миграции**: используется `fallbackToDestructiveMigration(dropAllTables = true)` — при изменении
  схемы база может быть сброшена (важно для релизов).

---

### DataStore Preferences

`AppPreferences` хранит пользовательские настройки и UI-память:

- тема (`is_dark_theme`, допускает `null` → «как в системе»),
- сортировка (`event_sort_type`),
- выбранные категории (`selected_categories`),
- онбординг (`has_seen_onboarding`),
- флаг «разрешение POST_NOTIFICATIONS уже спрашивали»,
- «последняя выбранная категория» + активные quick-фильтры для восстановления UI.

---

## Парсинг Telegram (Remote)

### Источник и ограничения

Парсинг реализован через web-превью `t.me/s/<channel>` (библиотека `Jsoup`).

Ограничения текущей реализации:

- берутся только **последние 10 сообщений** канала (см. `TelegramParser.PARSE_LIMIT`),
- качество извлечения зависит от форматирования поста и доступности страницы,
- каналы задаются маппингом в `TelegramChannelProvider` (по категориям).

---

### Пайплайн парсинга

`TelegramMessagePipeline` преобразует одно Telegram-сообщение в один или несколько `Event`:

- сохраняет форматирование текста (переносы строк, ссылки),
- извлекает и нормализует дату/время (включая случаи без года),
- определяет `EventType` и дополнительные поля (организатор, цена/бесплатно, формат, призовой фонд,
  теги),
- поддерживает «дайджесты» (один пост → несколько событий),
- отсекает нерелевантные «даты» (например, дедлайны регистрации/скидки) по контексту.

---

### Дедупликация

Дедупликация работает в два слоя:

- **на уровне парсера**: группировка по нормализованному ключу (`EventIdentityNormalizer`) и выбор
  «лучшего» дубля (например, с картинкой / более длинным описанием),
- **на уровне БД**: `EventDeduplicationService` удаляет старые не-избранные дубли и не вставляет
  события, дублирующие уже существующие избранные.

---

## Фоновые задачи и уведомления

### WorkManager

- **Периодическое обновление событий**: `EventRefreshWorker` планируется в `EventParserApplication`
  каждые **6 часов** при наличии сети (`NetworkType.CONNECTED`).
- **Точечные напоминания**: `EventReminderScheduler` создаёт **one-time** задачи
  `EventReminderWorker` на конкретные моменты:
    - за 24 часа,
    - за 1 час до начала.
      Планирование происходит при добавлении в избранное и восстанавливается при старте приложения.

Примечание: старый подход с periodic-work для уведомлений отменяется (
`cancelUniqueWork("event_notifications")`), но класс `EventNotificationWorker` остаётся в кодовой
базе как вспомогательный/legacy.

---

### Notifications

`data.notification.NotificationManager`:

- создаёт 2 канала: напоминания и «новые события»,
- учитывает `POST_NOTIFICATIONS` (Android 13+) и глобальное выключение уведомлений,
- открывает `MainActivity` с `event_id`, чтобы перейти в детали,
- показывает уведомления о **новых найденных событиях** после фонового обновления (не включает
  прошедшие).

---

## Навигация и deep links

Навигация реализована через **Navigation Compose** (`NavHost`/`composable`) в
`navigation/AppNavigation.kt`.

- Экран деталей принимает аргумент `eventId` в роуте `event/{eventId}`.
- В `AndroidManifest.xml` настроен deep link `eventparser://event/<eventId>`.
- `MainActivity` поддерживает открытие события из deep link или из уведомления (`event_id` extra) и
  выполняет навигацию на экран деталей.

---

## Экспорт / импорт

- **Избранное → JSON**: `FavoritesViewModel` сериализует `FavoritesExport` через
  `kotlinx.serialization` и пишет текст в выбранный документ через `DocumentTextStorage` (SAF).
- **JSON → Избранное**: импорт читает файл, проверяет `schemaVersion`, выполняет upsert в БД и
  помечает события как избранные; параллельно планируются напоминания.
- **Событие → .ics**: `IcsExporter` генерирует iCalendar-файл в `cacheDir` и отдаёт `content://` Uri
  через `FileProvider`.

---

## Платформенные абстракции и обработка ошибок

Интерфейсы (Domain): `NetworkStatusProvider`, `ErrorMessageProvider`, `ResourceProvider`,
`RetryPolicy`, `TimeProvider`.
Реализации (Data/Android):

- `AndroidNetworkStatusProvider` — проверка сети,
- `AndroidErrorMessageProvider` — маппинг `IOException`/`HttpException` в локализованные строки,
- `AndroidResourceProvider` — доступ к ресурсам,
- `SystemTimeProvider` — время/дата,
- `ExponentialBackoffRetryPolicy` — ретраи с exponential backoff для retryable-ошибок.

---

## Dependency Injection (Koin)

Koin инициализируется в `EventParserApplication`, список модулей: `di/appModules`.

Модули:

- `DatabaseModule` (Room + DAO + дедупликация),
- `NetworkModule` (парсер и агрегатор),
- `RepositoryModule` (репозитории + DataStore preferences),
- `DomainModule` (use cases),
- `PresentationModule` (view models),
- `NotificationModule` (уведомления/планировщик),
- `PlatformModule` (платформенные реализации),
- `SerializationModule` (настройки `Json`),
- `ExportModule` (SAF storage + `.ics` exporter).

---

## Логирование и crash handling

- `Timber`: в debug — `DebugTree`, в release — логируем только WARN/ERROR.
- `CrashHandler`: устанавливает `UncaughtExceptionHandler`, пишет crash-логи в `filesDir/crashes` (
  хранит последние 10).
