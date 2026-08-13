# Обзор Wage App Backend

Документ описывает фактически реализованное состояние backend и его место в системе. Корневой `README.md` служит публичной визиткой проекта; технические детали для разработчиков находятся здесь и в репозитории инфраструктуры.

## Назначение и граница

Wage App ведёт учёт рабочих смен и выплат отдельно для каждой `Company` — точки, у которой собственные сотрудники, смены и расчёты. Этот репозиторий содержит backend: он принимает данные о смене, рассчитывает выплаты, хранит результаты, строит ведомости и отправляет события сервису уведомлений.

Окружающие компоненты:

| Компонент | Ответственность |
| --- | --- |
| `wage-app-web` | Пользовательский web-интерфейс |
| `wage-app-backend` | Доменная логика, REST API и хранение данных Wage App |
| Keycloak | Аутентификация пользователей и выдача JWT |
| PostgreSQL | Данные backend; Liquibase управляет схемой `wage_app` |
| `wage-app-tg-bot` | Приём событий backend и Telegram-уведомления |
| [`wage-app-infr`](https://github.com/dundunts/wage-app-infr) | Compose- и Kubernetes-инфраструктура, production-конфигурация и runbooks |

Web-клиент и Telegram-бот развиваются в отдельных репозиториях. Backend взаимодействует с ними по REST.

## Доменный поток

Канонические определения терминов находятся в [`CONTEXT.md`](../CONTEXT.md).

1. Пользователь открывает `Shift Session` для одной `Company`. `Shift Session Start` задаёт начало первого интервала и не является `Checkpoint`.
2. `Checkpoint` завершает интервал. Он содержит состав команды этого интервала и накопленные к своему времени `Revenue` и `Restaurant Tips`. При приходе или уходе сотрудника создаётся новый `Checkpoint` со старым составом команды; `Final Checkpoint` завершает последний интервал.
3. Для каждого интервала backend вычисляет прирост выручки и чаевых. К приросту выручки применяется `Revenue Percentage`, после чего `Percent from Revenue` и чаевые делятся поровну между сотрудниками интервала. Отработанное время сохраняется, но не влияет на сумму выплаты.
4. Расчёт сохраняется как `Shift Result Draft`. Его подтверждение создаёт `Shift Result`, закрывает сессию и публикует событие уведомления. Результат также можно сохранить напрямую как `Manual Override`.
5. Результаты агрегируются в личный или общий `Payroll` за первую половину месяца, вторую половину либо произвольный период. Общую ведомость можно экспортировать в XLSX.

Все денежные значения сейчас являются целыми рублями. `Revenue Percentage` хранится в сотых долях процента: например, `600` соответствует `6%`. Доля от выручки округляется до целого отдельно для сотрудника и интервала; чаевые делятся целочисленно, поэтому остаток от деления не распределяется.

## API и доступ

REST API расположен под `/api/v1`:

| Группа | Назначение |
| --- | --- |
| `/company` | Точки и их параметры |
| `/employee` | Сотрудники, принадлежность точкам и связь с пользователем |
| `/session` | Открытие, закрытие и изменение сессий смен |
| `/checkpoint` | Создание, изменение и удаление Checkpoint |
| `/calculation` | Расчёт, подтверждение и удаление черновика результата |
| `/shift-result` | Чтение и ручное сохранение результатов смен |
| `/salary` | Личная и общая ведомость, XLSX-экспорт |

Все запросы `/api/**` требуют JWT от Keycloak. `sub` токена используется как идентификатор `User` и может быть связан с `Employee`.

Для операций со сменами, расчётами, результатами и ведомостями backend проверяет связь пользователя с `Company`. `Position` (`MANAGER`, `WAITER_ACTIVE`, `WAITER_INACTIVE`) является характеристикой сотрудника, а не ролью безопасности. CRUD компаний и сотрудников сейчас требует только аутентификации. Actuator endpoints доступны без аутентификации.

OpenAPI-описания в репозитории нет; фактический контракт задают контроллеры и интеграционные тесты.

## Устройство backend

- Kotlin, Java 21, Spring Boot 3.5;
- Spring WebFlux и Kotlin Coroutines;
- PostgreSQL через R2DBC;
- Liquibase и Preliquibase для схемы;
- Spring Security OAuth2 Resource Server и OAuth2 Client;
- Apache POI для XLSX;
- Actuator и Prometheus registry для health/metrics endpoints.

Основные пакеты:

- `domain` — доменные модели, расчёт выплат и агрегация Payroll;
- `application/controller` — HTTP API;
- `application/service` — прикладные сценарии;
- `application/data` — R2DBC entities и repositories;
- `messaging` — преобразование и отправка событий Telegram-сервису;
- `config` — security, web clients и общая конфигурация.

Уведомления доставляются best effort: события находятся в памяти процесса, постоянной очереди и повторных попыток нет. Одна попытка ограничена 10 секундами; её ошибка логируется и не останавливает обработку последующих событий. Ошибка Telegram-сервиса не откатывает подтверждённый результат смены.

## Локальная разработка

Нужны JDK 21, Docker, PostgreSQL и Keycloak. Тесты используют Testcontainers, поэтому для полного набора тестов требуется работающий Docker daemon.

```shell
bash gradlew test
```

После запуска PostgreSQL и Keycloak приложение запускается так:

```shell
bash gradlew bootRun
```

По умолчанию backend ожидает PostgreSQL на `localhost:5434`, Keycloak на `localhost:8080` с realm `WageApp` и слушает порт `8085`. Основные переменные окружения:

| Переменная | Назначение |
| --- | --- |
| `POSTGRES_URL`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Подключение R2DBC и Liquibase к PostgreSQL |
| `KEYCLOAK_URL`, `KEYCLOAK_REALM` | JWT issuer и OAuth2 provider |
| `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET` | Client credentials для исходящих запросов |
| `TELEGRAM_BOT_URL` | Адрес сервиса уведомлений |

Текущий `docker-compose.yaml` не является полностью автономным quickstart: ему нужны внешние значения окружения и подготовленная конфигурация Keycloak. Канонического `.env.example` в backend пока нет.

## Deployment и релизы

Runtime source of truth находится в [`wage-app-infr`](https://github.com/dundunts/wage-app-infr), а не в этом репозитории. Backend-репозиторий владеет исходным кодом, тестами, Dockerfile и публикацией образа; infra-репозиторий — Helm charts, runtime contract и GitOps desired state.

На 11 августа 2026 года Kubernetes-развёртывание PostgreSQL, Keycloak, backend и web проверено как `Synced/Healthy`, но production traffic cutover не завершён. Compose остаётся production fallback. Telegram-бот продолжает работать отдельно, а отправка уведомлений из Kubernetes backend отключена. Актуальное состояние и процедуры следует проверять в [архитектуре CI/CD](https://github.com/dundunts/wage-app-infr/blob/main/docs/architecture/ci-cd-gitops.md) и [операционных evidence](https://github.com/dundunts/wage-app-infr/tree/main/docs/operations/evidence), а не копировать из этого snapshot.

Production-релиз начинается с аннотированного тега `vX.Y.Z` или `vX.Y.Z-U<N>` на соответствующей release-ветке. CI один раз собирает и публикует неизменяемый image, фиксирует digest и открывает image-only PR в `wage-app-infr`; после merge Argo CD применяет desired state. Откат выполняется через Git на ранее опубликованный digest.

## Текущие ограничения

- `Recalculation` реализован не полностью: backend открывает закрытую сессию для перерасчёта, но подтверждение нового draft конфликтует с единственным допустимым результатом для `Company` и даты; web-клиент этот сценарий не предоставляет.
- `Position` не ограничивает операции; часть административного CRUD проверяет только наличие JWT.
- Уведомления не имеют durable queue и retry.
- Actuator endpoints публичны.
- Полностью воспроизводимого локального bootstrap и OpenAPI-контракта нет.
- Dockerfile объявляет порт `8080`, тогда как приложение, Compose и Kubernetes используют `8085`.
- Централизованного сбора логов, метрик и алертов в текущем Kubernetes-стеке нет; PostgreSQL и его локальные backups находятся на одном узле. Подробности принадлежат infra-репозиторию.
