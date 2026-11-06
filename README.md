# Ремонт бытовой техники (Вариант 4) — Задание 3

REST API на Spring Boot + реляционная БД PostgreSQL. Сущности: User, Device, RepairOrder, SparePart, RepairItem.

Архитектура: controller (запросы и маппинг) -> service (логика и бизнес-операции) -> repository (Spring Data JPA + PostgreSQL).

## Запуск

1. Запустить БД и применить схему/данные:
   ```
   cp .env.example .env   # изменить пароли
   docker compose --env-file .env up
   ```
2. Запустить приложение (параметры подключения берутся из переменных окружения, см. `src/main/resources/application.yml`):
   ```
   mvn spring-boot:run
   ```
   Адрес: http://localhost:8080

## База данных

- PostgreSQL 16, контейнер с томом `repair_pgdata`.
- Схема создаётся скриптами из `db/init` при первом запуске:
  - `01-schema.sql` — таблицы, внешние ключи, UNIQUE, CHECK и индексы;
  - `02-app-user.sql` — ограниченный пользователь `app_user` (только SELECT/INSERT/UPDATE/DELETE, без DDL);
  - `03-seed.sql` — тестовые данные (клиенты, устройства, запчасти, наряды).
- Пароли и адрес подключения передаются через переменные окружения (`.env`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), в коде секреты не хранятся.
- Hibernate работает в режиме `ddl-auto: validate` — структуру контролируют SQL-скрипты.

Таблицы и ограничения:

| Таблица | Ограничения |
|---|---|
| users | PK, UNIQUE(email) |
| devices | PK, FK(owner_id) -> users, UNIQUE(serial_number) |
| spare_parts | PK, CHECK(price >= 0), CHECK(0 <= reserved <= stock) |
| repair_orders | PK, FK(device_id) -> devices, CHECK(status IN ...), CHECK(cost >= 0) |
| repair_items | PK, FK(order_id), FK(spare_part_id), CHECK(quantity > 0), CHECK(usage_status IN ...) |

Каскадное удаление реализовано на уровне сервисов в одной транзакции:
пользователь -> устройства -> наряды -> позиции (с освобождением резервов).

## Бизнес-операции

Статус наряда управляется только бизнес-операциями (через PATCH изменить нельзя).

| # | Операция | Правила |
|---|---|---|
| 1 | POST /api/repair-orders/{id}/start | NEW -> IN_PROGRESS |
| 2 | POST /api/repair-orders/{id}/wait-for-parts | IN_PROGRESS -> WAITING_FOR_PARTS, только если есть незакрытая позиция |
| 3 | POST /api/repair-orders/{id}/resume | WAITING_FOR_PARTS -> IN_PROGRESS |
| 4 | POST /api/repair-orders/{id}/complete | IN_PROGRESS/WAITING_FOR_PARTS -> COMPLETED; все позиции должны быть закрыты; стоимость = сумма USED-позиций по фиксированным ценам |
| 5 | POST /api/repair-orders/{id}/cancel | из любого активного статуса; все резервы освобождаются |
| 6 | POST /api/repair-items | добавление запчасти в наряд: наряд активен, есть свободный остаток; цена фиксируется; reserved_quantity запчасти растёт |
| 7 | POST /api/repair-items/{id}/use | RESERVED -> USED; склад и резерв уменьшаются |
| 8 | POST /api/repair-items/{id}/cancel | RESERVED -> CANCELLED; резерв возвращается на склад |

Инварианты: `reserved <= stock` (CHECK в БД), цена позиции фиксируется в момент создания,
запрещены недопустимые переходы статусов (409 Conflict).

## Ресурсы

/api/users, /api/devices, /api/repair-orders, /api/spare-parts, /api/repair-items
(GET список, GET по id, POST, PATCH, DELETE).

Через PATCH нельзя менять status наряда, usageStatus позиции и reservedQuantity запчасти —
только бизнес-операции. Нарушения ограничений БД возвращаются как 409 Conflict.

## Postman

`postman/repair-service.postman_collection.json` — CRUD всех сущностей и бизнес-операции:
полный сценарий ремонта (создание клиента и устройства, наряд, резервирование запчасти,
приёмка в работу, списание, завершение со расчётом стоимости) и проверки отказа
(недостаточно склада, недопустимый статус, повторное списание). Перед запуском
выбрать окружение с `baseUrl = http://localhost:8080`.

## Тесты

`mvn test` — интеграционные тесты REST API и бизнес-операций (H2 в режиме PostgreSQL,
16 тестов): полный сценарий ремонта, проверка инвариантов и отказов (409/400), каскадное удаление.
