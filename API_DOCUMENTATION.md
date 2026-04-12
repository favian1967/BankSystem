# 🏦 Bank System API Documentation

**Base URL:** `https://api.favian1967.site`

**Версия:** 1.0
**Дата:** 2026-03-03

---

## 📋 Оглавление

1. [Общие сведения](#общие-сведения)
2. [Аутентификация](#аутентификация)
3. [Формат ошибок](#формат-ошибок)
4. [Enum-справочники](#enum-справочники)
5. [Auth — Регистрация / Вход / Выход](#1-auth--регистрация--вход--выход)
6. [Accounts — Счета](#2-accounts--счета)
7. [Cards — Карты](#3-cards--карты)
8. [Transactions — Транзакции](#4-transactions--транзакции)
9. [Users — Пользователь](#5-users--пользователь)

---

## Общие сведения

| Параметр                  | Значение                                         |
| ------------------------- | ------------------------------------------------ |
| Протокол                  | HTTPS                                            |
| Content-Type              | `application/json`                               |
| Кодировка                 | UTF-8                                            |
| Формат дат                | ISO 8601 (`2026-03-03T14:30:00`)                 |
| Формат даты (без времени) | `YYYY-MM-DD`                                     |
| Десятичные числа          | Строковое представление BigDecimal (`"1500.00"`) |

---

## Аутентификация

API использует **JWT Bearer Token**.

### Как получить токен

1. Зарегистрироваться через `POST /api/auth/register`
2. Войти через `POST /api/auth/login` → в ответе придёт JWT-токен (строка).

### Как использовать токен

Все защищённые эндпоинты требуют заголовок:

```bash
Authorization: Bearer <jwt_token>
```

### Уровни доступа

| Роль    | Описание                                                                                   |
| ------- | ------------------------------------------------------------------------------------------ |
| `USER`  | Обычный пользователь — доступ ко всем эндпоинтам кроме `/api/admin/**`                     |
| `ADMIN` | Администратор — доступ ко всем эндпоинтам, включая `/api/admin/**` и `/api/cards/admin/**` |

### Публичные эндпоинты (без токена)

* `POST /api/auth/register`
* `POST /api/auth/login`
* `POST /api/auth/logout`
* `POST /api/auth/send`
* `POST /api/auth/confirm`

---

## Формат ошибок

При ошибке API возвращает JSON:

```json
{
  "timestamp": "2026-03-03T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "email is required",
  "path": "/api/auth/register"
}
```

| Поле        | Тип                 | Описание            |
| ----------- | ------------------- | ------------------- |
| `timestamp` | `string` (ISO 8601) | Время ошибки        |
| `status`    | `integer`           | HTTP-код            |
| `error`     | `string`            | Название ошибки     |
| `message`   | `string`            | Подробное сообщение |
| `path`      | `string`            | Путь запроса        |

---

## Enum-справочники

### Currency (Валюта)

```text
RUB | USD | EUR
```

### AccountType (Тип счёта)

```text
CHECKING | SAVED | DEPOSIT
```

### AccountStatus (Статус счёта)

```text
ACTIVE | BLOCKED | CLOSED
```

### CardType (Тип карты)

```text
DEBIT | CREDIT
```

### CardPaymentSystem (Платёжная система)

```text
VISA | MASTERCARD | MIR
```

### CardStatus (Статус карты)

```text
ACTIVE | BLOCKED | EXPIRED
```

### TransactionType (Тип транзакции)

```text
TRANSFER | DEPOSIT | WITHDRAW | PAYMENT
```

### TransactionStatus (Статус транзакции)

```text
PENDING | COMPLETED | FAILED
```

### UserRole (Роль пользователя)

```text
USER | ADMIN
```

---

## 1. Auth — Регистрация / Вход / Выход

### 1.1 Регистрация

```text
POST /api/auth/register
```

**Авторизация:** Не требуется

**Тело запроса (JSON Body):**

| Поле         | Тип      | Обязательно | Валидация                                      | Пример               |
| ------------ | -------- | ----------- | ---------------------------------------------- | -------------------- |
| `email`      | `string` | ✅           | Валидный email, max 254 символа                | `"user@example.com"` |
| `password`   | `string` | ✅           | 8–128 символов                                 | `"MySecurePass123"`  |
| `first_name` | `string` | ✅           | Только буквы (A-Za-z, А-Яа-яЁё, дефис, пробел) | `"Иван"`             |
| `phone`      | `string` | ✅           | Формат: `+7XXXXXXXXXX` (12 символов)           | `"+79991234567"`     |

**Пример запроса:**

```json
{
  "email": "user@example.com",
  "password": "MySecurePass123",
  "first_name": "Иван",
  "phone": "+79991234567"
}
```

**Ответ `200 OK`:**

```json
"User registered successfully"
```

> Тело ответа — строка (не JSON-объект, а просто строка в кавычках).

---

### 1.2 Вход (Login)

```text
POST /api/auth/login
```

**Авторизация:** Не требуется

**Тело запроса (JSON Body):**

| Поле       | Тип      | Обязательно | Валидация                     | Пример               |
| ---------- | -------- | ----------- | ----------------------------- | -------------------- |
| `email`    | `string` | ✅           | Валидный email, 3–254 символа | `"user@example.com"` |
| `password` | `string` | ✅           | 8–128 символов                | `"MySecurePass123"`  |

**Пример запроса:**

```json
{
  "email": "user@example.com",
  "password": "MySecurePass123"
}
```

**Ответ `200 OK`:**

```json
"eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIi..."
```

> Тело ответа — JWT-токен в виде строки. Используйте его в заголовке `Authorization: Bearer <token>`.

---

### 1.3 Выход (Logout)

```text
POST /api/auth/logout
```

**Заголовки (Headers):**

| Заголовок       | Значение             | Обязательно |
| --------------- | -------------------- | ----------- |
| `Authorization` | `Bearer <jwt_token>` | ✅           |

**Тело запроса:** Нет

**Ответ `200 OK`:**

```json
{
  "message": "Logged out successfully"
}
```

**Ответ при отсутствии токена:**

```json
{
  "message": "Authentication Failed"
}
```

---

### 1.4 Отправить код подтверждения email

```text
POST /api/auth/send
```

**Авторизация:** Требуется JWT (Bearer Token)

**Заголовки (Headers):**

| Заголовок       | Значение             | Обязательно |
| --------------- | -------------------- | ----------- |
| `Authorization` | `Bearer <jwt_token>` | ✅           |

**Тело запроса:** Нет

**Ответ `200 OK`:** Пустое тело (HTTP 200 без контента)

---

### 1.5 Подтвердить email

```text
POST /api/auth/confirm
```

**Авторизация:** Не требуется

**Тело запроса (JSON Body):**

| Поле  | Тип      | Обязательно | Описание                               | Пример       |
| ----- | -------- | ----------- | -------------------------------------- | ------------ |
| `key` | `string` | ✅           | Код подтверждения, полученный на email | `"a1b2c3d4"` |

**Пример запроса:**

```json
{
  "key": "a1b2c3d4"
}
```

**Ответ `200 OK`:**

```json
true
```

> или `false`, если ключ невалиден.

---

## 2. Accounts — Счета

> Все эндпоинты этого раздела требуют заголовок `Authorization: Bearer <jwt_token>`

### 2.1 Создать счёт

```text
POST /api/accounts/add
```

**Тело запроса (JSON Body):**

| Поле           | Тип             | Обязательно | Допустимые значения            | Пример       |
| -------------- | --------------- | ----------- | ------------------------------ | ------------ |
| `account_type` | `string` (enum) | ✅           | `CHECKING`, `SAVED`, `DEPOSIT` | `"CHECKING"` |
| `currency`     | `string` (enum) | ✅           | `RUB`, `USD`, `EUR`            | `"RUB"`      |

**Пример запроса:**

```json
{
  "account_type": "CHECKING",
  "currency": "RUB"
}
```

**Ответ `200 OK` — AccountResponse:**

```json
{
  "id": 1,
  "account_number": "40817810099001234567",
  "account_type": "CHECKING",
  "currency": "RUB",
  "balance": 0.00,
  "status": "ACTIVE"
}
```

---

### 2.2 Получить все свои счета

```text
GET /api/accounts/getAll
```

**Тело запроса:** Нет

**Ответ `200 OK` — массив AccountResponse:**

```json
[
  {
    "id": 1,
    "account_number": "40817810099001234567",
    "account_type": "CHECKING",
    "currency": "RUB",
    "balance": 15000.50,
    "status": "ACTIVE"
  },
  {
    "id": 2,
    "account_number": "40817840099009876543",
    "account_type": "SAVED",
    "currency": "USD",
    "balance": 500.00,
    "status": "ACTIVE"
  }
]
```

---

### 2.3 Получить счёт по ID

```text
GET /api/accounts/getById/{id}
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Пример:** `GET /api/accounts/getById/1`

**Ответ `200 OK` — AccountResponse:**

```json
{
  "id": 1,
  "account_number": "40817810099001234567",
  "account_type": "CHECKING",
  "currency": "RUB",
  "balance": 15000.50,
  "status": "ACTIVE"
}
```

---

### 2.4 Получить счёт по номеру

```text
GET /api/accounts/getByNumber/{accountNumber}
```

**Параметры пути (Path):**

| Параметр        | Тип      | Обязательно | Пример                   |
| --------------- | -------- | ----------- | ------------------------ |
| `accountNumber` | `string` | ✅           | `"40817810099001234567"` |

**Пример:** `GET /api/accounts/getByNumber/40817810099001234567`

**Ответ `200 OK` — AccountResponse** (см. формат выше)

---

### 2.5 Получить счета по типу

```text
GET /api/accounts/getByType/{type}
```

**Параметры пути (Path):**

| Параметр | Тип             | Обязательно | Допустимые значения            | Пример     |
| -------- | --------------- | ----------- | ------------------------------ | ---------- |
| `type`   | `string` (enum) | ✅           | `CHECKING`, `SAVED`, `DEPOSIT` | `CHECKING` |

**Пример:** `GET /api/accounts/getByType/CHECKING`

**Ответ `200 OK` — массив AccountResponse**

---

### 2.6 Получить счета по валюте

```text
GET /api/accounts/getByCurrency/{currency}
```

**Параметры пути (Path):**

| Параметр   | Тип             | Обязательно | Допустимые значения | Пример |
| ---------- | --------------- | ----------- | ------------------- | ------ |
| `currency` | `string` (enum) | ✅           | `RUB`, `USD`, `EUR` | `RUB`  |

**Пример:** `GET /api/accounts/getByCurrency/RUB`

**Ответ `200 OK` — массив AccountResponse**

---

### 2.7 Получить счета по статусу

```text
GET /api/accounts/getByStatus/{status}
```

**Параметры пути (Path):**

| Параметр | Тип             | Обязательно | Допустимые значения           | Пример   |
| -------- | --------------- | ----------- | ----------------------------- | -------- |
| `status` | `string` (enum) | ✅           | `ACTIVE`, `BLOCKED`, `CLOSED` | `ACTIVE` |

**Пример:** `GET /api/accounts/getByStatus/ACTIVE`

**Ответ `200 OK` — массив AccountResponse**

---

### 2.8 Получить баланс счёта

```text
GET /api/accounts/{id}/balance
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Пример:** `GET /api/accounts/1/balance`

**Ответ `200 OK`:**

```json
{
  "balance": 15000.50
}
```

---

### 2.9 Получить суммарный баланс по валюте

```text
GET /api/accounts/totalBalance/{currency}
```

**Параметры пути (Path):**

| Параметр   | Тип             | Обязательно | Допустимые значения | Пример |
| ---------- | --------------- | ----------- | ------------------- | ------ |
| `currency` | `string` (enum) | ✅           | `RUB`, `USD`, `EUR` | `RUB`  |

**Пример:** `GET /api/accounts/totalBalance/RUB`

**Ответ `200 OK`:**

```json
{
  "total_balance": 150000.00,
  "currency": "RUB"
}
```

---

### 2.10 Обновить статус счёта

```text
PATCH /api/accounts/{id}/status
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Тело запроса (JSON Body):**

| Поле     | Тип             | Обязательно | Допустимые значения           | Пример      |
| -------- | --------------- | ----------- | ----------------------------- | ----------- |
| `status` | `string` (enum) | ✅           | `ACTIVE`, `BLOCKED`, `CLOSED` | `"BLOCKED"` |

**Пример запроса:**

```json
{
  "status": "BLOCKED"
}
```

**Ответ `200 OK` — AccountResponse**

---

### 2.11 Закрыть счёт

```text
DELETE /api/accounts/{id}/close
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Пример:** `DELETE /api/accounts/1/close`

**Ответ `204 No Content`:** Пустое тело

---

### 2.12 Получить активные счета

```text
GET /api/accounts/active
```

**Тело запроса:** Нет

**Ответ `200 OK` — массив AccountResponse** (только со статусом `ACTIVE`)

---

### 2.13 Заблокировать счёт

```text
PATCH /api/accounts/{id}/block
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Тело запроса:** Нет

**Ответ `200 OK` — AccountResponse** (статус = `BLOCKED`)

---

### 2.14 Разблокировать счёт

```text
PATCH /api/accounts/{id}/unblock
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Тело запроса:** Нет

**Ответ `200 OK` — AccountResponse** (статус = `ACTIVE`)

---

### 2.15 Количество счетов

```text
GET /api/accounts/count
```

**Тело запроса:** Нет

**Ответ `200 OK`:**

```json
{
  "count": 5
}
```

---

### 2.16 Проверить существование счёта

```text
GET /api/accounts/exists/{accountNumber}
```

**Параметры пути (Path):**

| Параметр        | Тип      | Обязательно | Пример                   |
| --------------- | -------- | ----------- | ------------------------ |
| `accountNumber` | `string` | ✅           | `"40817810099001234567"` |

**Пример:** `GET /api/accounts/exists/40817810099001234567`

**Ответ `200 OK`:**

```json
{
  "exists": true
}
```

---

## 3. Cards — Карты

> Все эндпоинты этого раздела требуют заголовок `Authorization: Bearer <jwt_token>`

### Формат CardResponse

```json
{
  "id": 1,
  "card_number": "4276****1234",
  "card_holder_name": "IVAN IVANOV",
  "expiry_date": "2029-03-01",
  "card_type": "DEBIT",
  "payment_system": "VISA",
  "card_status": "ACTIVE",
  "account_id": 1,
  "created_at": "2026-03-03T12:00:00"
}
```

### Формат CardIssueResponse (при создании — содержит CVV)

```json
{
  "id": 1,
  "card_number": "4276123456781234",
  "card_holder_name": "IVAN IVANOV",
  "expiry_date": "2029-03-01",
  "card_type": "DEBIT",
  "payment_system": "VISA",
  "card_status": "ACTIVE",
  "account_id": 1,
  "cvv": "123",
  "created_at": "2026-03-03T12:00:00"
}
```

> ⚠️ CVV возвращается **только при создании карты**. Сохраните его — повторно получить невозможно.

---

### 3.1 Создать карту

```text
POST /api/cards/createCard
```

**Тело запроса (JSON Body):**

| Поле             | Тип             | Обязательно | Допустимые значения         | Пример    |
| ---------------- | --------------- | ----------- | --------------------------- | --------- |
| `account_id`     | `integer`       | ✅           | ≥ 0                         | `1`       |
| `card_type`      | `string` (enum) | ✅           | `DEBIT`, `CREDIT`           | `"DEBIT"` |
| `payment_system` | `string` (enum) | ✅           | `VISA`, `MASTERCARD`, `MIR` | `"VISA"`  |

**Пример запроса:**

```json
{
  "account_id": 1,
  "card_type": "DEBIT",
  "payment_system": "VISA"
}
```

**Ответ `200 OK` — CardIssueResponse** (см. формат выше, содержит `cvv`)

---

### 3.2 Получить все свои карты

```text
GET /api/cards/getMyCards
```

**Тело запроса:** Нет

**Ответ `200 OK` — массив CardResponse**

---

### 3.3 Получить карту по ID

```text
GET /api/cards/getCard/{id}
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Ответ `200 OK` — CardResponse**

---

### 3.4 Заблокировать карту

```text
POST /api/cards/block/{id}
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Тело запроса:** Нет

**Ответ `200 OK` — CardResponse** (статус = `BLOCKED`)

---

### 3.5 Разблокировать карту

```text
POST /api/cards/unblock/{id}
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Тело запроса:** Нет

**Ответ `200 OK` — CardResponse** (статус = `ACTIVE`)

---

### 3.6 Получить баланс карты

```text
GET /api/cards/balance/{id}
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Ответ `200 OK`:**

```json
15000.50
```

> Ответ — число (BigDecimal).

---

### 3.7 Получить карты по счёту

```text
GET /api/cards/getByAccount/{accountId}
```

**Параметры пути (Path):**

| Параметр    | Тип       | Обязательно | Валидация           | Пример |
| ----------- | --------- | ----------- | ------------------- | ------ |
| `accountId` | `integer` | ✅           | Положительное число | `1`    |

**Ответ `200 OK` — массив CardResponse**

---

### 3.8 Получить карты по статусу

```text
GET /api/cards/getByStatus/{status}
```

**Параметры пути (Path):**

| Параметр | Тип             | Обязательно | Допустимые значения            | Пример   |
| -------- | --------------- | ----------- | ------------------------------ | -------- |
| `status` | `string` (enum) | ✅           | `ACTIVE`, `BLOCKED`, `EXPIRED` | `ACTIVE` |

**Ответ `200 OK` — массив CardResponse**

---

### 3.9 Получить карты по типу

```text
GET /api/cards/getByType/{type}
```

**Параметры пути (Path):**

| Параметр | Тип             | Обязательно | Допустимые значения | Пример  |
| -------- | --------------- | ----------- | ------------------- | ------- |
| `type`   | `string` (enum) | ✅           | `DEBIT`, `CREDIT`   | `DEBIT` |

**Ответ `200 OK` — массив CardResponse**

---

### 3.10 Получить активные карты

```text
GET /api/cards/active
```

**Ответ `200 OK` — массив CardResponse** (только `ACTIVE`)

---

### 3.11 Получить заблокированные карты

```text
GET /api/cards/blocked
```

**Ответ `200 OK` — массив CardResponse** (только `BLOCKED`)

---

### 3.12 Получить просроченные карты

```text
GET /api/cards/expired
```

**Ответ `200 OK` — массив CardResponse** (только `EXPIRED`)

---

### 3.13 Количество карт

```text
GET /api/cards/count
```

**Ответ `200 OK`:**

```json
{
  "count": 3
}
```

---

### 3.14 Количество карт по статусу

```text
GET /api/cards/countByStatus/{status}
```

**Параметры пути (Path):**

| Параметр | Тип             | Обязательно | Допустимые значения            | Пример   |
| -------- | --------------- | ----------- | ------------------------------ | -------- |
| `status` | `string` (enum) | ✅           | `ACTIVE`, `BLOCKED`, `EXPIRED` | `ACTIVE` |

**Ответ `200 OK`:**

```json
{
  "count": 2,
  "status": 0
}
```

> `status` — ordinal-значение enum (ACTIVE=0, BLOCKED=1, EXPIRED=2)

---

### 3.15 Удалить карту

```text
DELETE /api/cards/delete/{id}
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Ответ `204 No Content`:** Пустое тело

---

### 3.16 Проверить срок действия карты

```text
GET /api/cards/checkExpiry/{id}
```

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `id`     | `integer` | ✅           | Положительное число | `1`    |

**Ответ `200 OK`:**

```json
{
  "expired": false,
  "expiry_date": "2029-03-01",
  "days_until_expiry": 1094
}
```

---

### 3.17 [ADMIN] Получить карты пользователя

```text
GET /api/cards/admin/getByUserId/{userId}
```

**Авторизация:** Требуется JWT + роль `ADMIN`

**Параметры пути (Path):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `userId` | `integer` | ✅           | Положительное число | `1`    |

**Ответ `200 OK` — массив CardResponse**

---

### 3.18 [ADMIN] Получить все карты системы

```text
GET /api/cards/admin/getAllCards
```

**Авторизация:** Требуется JWT + роль `ADMIN`

**Ответ `200 OK` — массив CardResponse**

---

### 3.19 [ADMIN] Статистика по картам

```text
GET /api/cards/admin/stats
```

**Авторизация:** Требуется JWT + роль `ADMIN`

**Ответ `200 OK`:**

```json
{
  "total_cards": 150,
  "active_cards": 120,
  "blocked_cards": 25,
  "expired_cards": 5
}
```

---

## 4. Transactions — Транзакции

> Все эндпоинты этого раздела требуют заголовок `Authorization: Bearer <jwt_token>`

### Формат TransactionResponse

```json
{
  "id": 1,
  "from_account_id": 1,
  "to_account_id": null,
  "transaction_type": "DEPOSIT",
  "amount": 5000.00,
  "currency": "RUB",
  "description": "Зарплата",
  "status": "COMPLETED",
  "created_at": "2026-03-03T14:30:00"
}
```

### ⚠️ Idempotency-Key

Все мутирующие операции (deposit, withdraw, transfer) требуют заголовок **`Idempotency-Key`** — уникальный идентификатор запроса (UUID). Это защищает от дублирования транзакций при повторных запросах.

---

### 4.1 Пополнение (Deposit)

```text
POST /api/transactions/deposit
```

**Заголовки (Headers):**

| Заголовок         | Значение             | Обязательно |
| ----------------- | -------------------- | ----------- |
| `Authorization`   | `Bearer <jwt_token>` | ✅           |
| `Idempotency-Key` | Уникальный UUID      | ✅           |
| `Content-Type`    | `application/json`   | ✅           |

**Тело запроса (JSON Body):**

| Поле          | Тип       | Обязательно | Валидация                      | Пример       |
| ------------- | --------- | ----------- | ------------------------------ | ------------ |
| `account_id`  | `integer` | ✅           | ≥ 0                            | `1`          |
| `amount`      | `decimal` | ✅           | 0.01–1000000                   | `5000.00`    |
| `description` | `string`  | ❌           | Без спец. символов `< > " ' &` | `"Зарплата"` |

**Пример запроса:**

```text
POST https://api.favian1967.site/api/transactions/deposit
Headers:
  Authorization: Bearer eyJhbGciOi...
  Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
  Content-Type: application/json
```

```json
{
  "account_id": 1,
  "amount": 5000.00,
  "description": "Зарплата"
}
```

**Ответ `200 OK` — TransactionResponse**

---

### 4.2 Снятие (Withdraw)

```text
POST /api/transactions/withdraw
```

**Заголовки (Headers):**

| Заголовок         | Значение             | Обязательно |
| ----------------- | -------------------- | ----------- |
| `Authorization`   | `Bearer <jwt_token>` | ✅           |
| `Idempotency-Key` | Уникальный UUID      | ✅           |
| `Content-Type`    | `application/json`   | ✅           |

**Тело запроса (JSON Body):**

| Поле          | Тип       | Обязательно | Валидация                      | Пример             |
| ------------- | --------- | ----------- | ------------------------------ | ------------------ |
| `account_id`  | `integer` | ✅           | > 0                            | `1`                |
| `amount`      | `decimal` | ✅           | 0.01–1000000                   | `2000.00`          |
| `description` | `string`  | ❌           | Без спец. символов `< > " ' &` | `"ATM withdrawal"` |

**Пример запроса:**

```json
{
  "account_id": 1,
  "amount": 2000.00,
  "description": "ATM withdrawal"
}
```

**Ответ `200 OK` — TransactionResponse**

---

### 4.3 Перевод (Transfer)

```text
POST /api/transactions/transfer
```

**Заголовки (Headers):**

| Заголовок         | Значение             | Обязательно |
| ----------------- | -------------------- | ----------- |
| `Authorization`   | `Bearer <jwt_token>` | ✅           |
| `Idempotency-Key` | Уникальный UUID      | ✅           |
| `Content-Type`    | `application/json`   | ✅           |

**Тело запроса (JSON Body):**

| Поле              | Тип       | Обязательно | Валидация                         | Пример                   |
| ----------------- | --------- | ----------- | --------------------------------- | ------------------------ |
| `from_account_id` | `integer` | ✅           | ID вашего счёта                   | `1`                      |
| `to_account_id`   | `string`  | ✅           | Номер счёта получателя            | `"40817810099009876543"` |
| `amount`          | `decimal` | ✅           | 0.01–1000000                      | `1500.00`                |
| `description`     | `string`  | ❌           | Max 500 символов, без `< > " ' &` | `"За обед"`              |

**Пример запроса:**

```json
{
  "from_account_id": 1,
  "to_account_id": "40817810099009876543",
  "amount": 1500.00,
  "description": "За обед"
}
```

**Ответ `200 OK` — TransactionResponse:**

```json
{
  "id": 5,
  "from_account_id": 1,
  "to_account_id": 2,
  "transaction_type": "TRANSFER",
  "amount": 1500.00,
  "currency": "RUB",
  "description": "За обед",
  "status": "COMPLETED",
  "created_at": "2026-03-03T14:35:00"
}
```

---

### 4.4 История транзакций (с пагинацией)

```text
GET /api/transactions/account/{accountId}?page={page}&size={size}
```

**Параметры пути (Path):**

| Параметр    | Тип       | Обязательно | Валидация | Пример |
| ----------- | --------- | ----------- | --------- | ------ |
| `accountId` | `integer` | ✅           | > 0       | `1`    |

**Параметры запроса (Query Parameters):**

| Параметр | Тип       | Обязательно | Валидация           | Пример |
| -------- | --------- | ----------- | ------------------- | ------ |
| `page`   | `integer` | ✅           | ≥ 0 (нумерация с 0) | `0`    |
| `size`   | `integer` | ✅           | 1–100               | `10`   |

**Пример:** `GET /api/transactions/account/1?page=0&size=10`

**Ответ `200 OK` — Page<TransactionResponse>:**

```json
{
  "content": [
    {
      "id": 5,
      "from_account_id": 1,
      "to_account_id": 2,
      "transaction_type": "TRANSFER",
      "amount": 1500.00,
      "currency": "RUB",
      "description": "За обед",
      "status": "COMPLETED",
      "created_at": "2026-03-03T14:35:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": { "sorted": false, "unsorted": true, "empty": true },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 25,
  "totalPages": 3,
  "last": false,
  "first": true,
  "size": 10,
  "number": 0,
  "sort": { "sorted": false, "unsorted": true, "empty": true },
  "numberOfElements": 10,
  "empty": false
}
```

---

### 4.5 Последние транзакции

```text
GET /api/transactions/account/{accountId}/recent?limit={limit}
```

**Параметры пути (Path):**

| Параметр    | Тип       | Обязательно | Валидация | Пример |
| ----------- | --------- | ----------- | --------- | ------ |
| `accountId` | `integer` | ✅           | > 0       | `1`    |

**Параметры запроса (Query Parameters):**

| Параметр | Тип       | Обязательно | Валидация | По умолчанию | Пример |
| -------- | --------- | ----------- | --------- | ------------ | ------ |
| `limit`  | `integer` | ❌           | 1–50      | `5`          | `10`   |

**Пример:** `GET /api/transactions/account/1/recent?limit=10`

**Ответ `200 OK` — массив TransactionResponse**

---

## 5. Users — Пользователь

> Все эндпоинты этого раздела требуют заголовок `Authorization: Bearer <jwt_token>`

### 5.1 Смена пароля

```text
PATCH /api/users/changePassword
```

**Тело запроса (JSON Body):**

| Поле                  | Тип      | Обязательно | Валидация                         | Пример               |
| --------------------- | -------- | ----------- | --------------------------------- | -------------------- |
| `old_password`        | `string` | ✅           | Не пустой                         | `"OldPass123"`       |
| `new_password`        | `string` | ✅           | 8–128 символов                    | `"NewSecurePass456"` |
| `repeat_new_password` | `string` | ✅           | Должен совпадать с `new_password` | `"NewSecurePass456"` |

**Пример запроса:**

```json
{
  "old_password": "OldPass123",
  "new_password": "NewSecurePass456",
  "repeat_new_password": "NewSecurePass456"
}
```

**Ответ `200 OK` — ChangePasswordResponse:**

```json
{
  "email": "user@example.com",
  "message": "Password changed successfully"
}
```

---

## 📌 Сводная таблица всех эндпоинтов

| #  | Метод    | URL                                            | Авторизация | Тело      | Idempotency-Key |
| -- | -------- | ---------------------------------------------- | ----------- | --------- | --------------- |
| 1  | `POST`   | `/api/auth/register`                           | ❌           | JSON      | ❌               |
| 2  | `POST`   | `/api/auth/login`                              | ❌           | JSON      | ❌               |
| 3  | `POST`   | `/api/auth/logout`                             | ✅ Bearer    | —         | ❌               |
| 4  | `POST`   | `/api/auth/send`                               | ✅ Bearer    | —         | ❌               |
| 5  | `POST`   | `/api/auth/confirm`                            | ❌           | JSON      | ❌               |
| 6  | `POST`   | `/api/accounts/add`                            | ✅ Bearer    | JSON      | ❌               |
| 7  | `GET`    | `/api/accounts/getAll`                         | ✅ Bearer    | —         | ❌               |
| 8  | `GET`    | `/api/accounts/getById/{id}`                   | ✅ Bearer    | —         | ❌               |
| 9  | `GET`    | `/api/accounts/getByNumber/{accountNumber}`    | ✅ Bearer    | —         | ❌               |
| 10 | `GET`    | `/api/accounts/getByType/{type}`               | ✅ Bearer    | —         | ❌               |
| 11 | `GET`    | `/api/accounts/getByCurrency/{currency}`       | ✅ Bearer    | —         | ❌               |
| 12 | `GET`    | `/api/accounts/getByStatus/{status}`           | ✅ Bearer    | —         | ❌               |
| 13 | `GET`    | `/api/accounts/{id}/balance`                   | ✅ Bearer    | —         | ❌               |
| 14 | `GET`    | `/api/accounts/totalBalance/{currency}`        | ✅ Bearer    | —         | ❌               |
| 15 | `PATCH`  | `/api/accounts/{id}/status`                    | ✅ Bearer    | JSON      | ❌               |
| 16 | `DELETE` | `/api/accounts/{id}/close`                     | ✅ Bearer    | —         | ❌               |
| 17 | `GET`    | `/api/accounts/active`                         | ✅ Bearer    | —         | ❌               |
| 18 | `PATCH`  | `/api/accounts/{id}/block`                     | ✅ Bearer    | —         | ❌               |
| 19 | `PATCH`  | `/api/accounts/{id}/unblock`                   | ✅ Bearer    | —         | ❌               |
| 20 | `GET`    | `/api/accounts/count`                          | ✅ Bearer    | —         | ❌               |
| 21 | `GET`    | `/api/accounts/exists/{accountNumber}`         | ✅ Bearer    | —         | ❌               |
| 22 | `POST`   | `/api/cards/createCard`                        | ✅ Bearer    | JSON      | ❌               |
| 23 | `GET`    | `/api/cards/getMyCards`                        | ✅ Bearer    | —         | ❌               |
| 24 | `GET`    | `/api/cards/getCard/{id}`                      | ✅ Bearer    | —         | ❌               |
| 25 | `POST`   | `/api/cards/block/{id}`                        | ✅ Bearer    | —         | ❌               |
| 26 | `POST`   | `/api/cards/unblock/{id}`                      | ✅ Bearer    | —         | ❌               |
| 27 | `GET`    | `/api/cards/balance/{id}`                      | ✅ Bearer    | —         | ❌               |
| 28 | `GET`    | `/api/cards/getByAccount/{accountId}`          | ✅ Bearer    | —         | ❌               |
| 29 | `GET`    | `/api/cards/getByStatus/{status}`              | ✅ Bearer    | —         | ❌               |
| 30 | `GET`    | `/api/cards/getByType/{type}`                  | ✅ Bearer    | —         | ❌               |
| 31 | `GET`    | `/api/cards/active`                            | ✅ Bearer    | —         | ❌               |
| 32 | `GET`    | `/api/cards/blocked`                           | ✅ Bearer    | —         | ❌               |
| 33 | `GET`    | `/api/cards/expired`                           | ✅ Bearer    | —         | ❌               |
| 34 | `GET`    | `/api/cards/count`                             | ✅ Bearer    | —         | ❌               |
| 35 | `GET`    | `/api/cards/countByStatus/{status}`            | ✅ Bearer    | —         | ❌               |
| 36 | `DELETE` | `/api/cards/delete/{id}`                       | ✅ Bearer    | —         | ❌               |
| 37 | `GET`    | `/api/cards/checkExpiry/{id}`                  | ✅ Bearer    | —         | ❌               |
| 38 | `GET`    | `/api/cards/admin/getByUserId/{userId}`        | ✅ ADMIN     | —         | ❌               |
| 39 | `GET`    | `/api/cards/admin/getAllCards`                 | ✅ ADMIN     | —         | ❌               |
| 40 | `GET`    | `/api/cards/admin/stats`                       | ✅ ADMIN     | —         | ❌               |
| 41 | `POST`   | `/api/transactions/deposit`                    | ✅ Bearer    | JSON      | ✅               |
| 42 | `POST`   | `/api/transactions/withdraw`                   | ✅ Bearer    | JSON      | ✅               |
| 43 | `POST`   | `/api/transactions/transfer`                   | ✅ Bearer    | JSON      | ✅               |
| 44 | `GET`    | `/api/transactions/account/{accountId}`        | ✅ Bearer    | — (Query) | ❌               |
| 45 | `GET`    | `/api/transactions/account/{accountId}/recent` | ✅ Bearer    | — (Query) | ❌               |
| 46 | `PATCH`  | `/api/users/changePassword`                    | ✅ Bearer    | JSON      | ❌               |

---

## 🔄 Типичный сценарий использования

```text
1. POST /api/auth/register        → Регистрация
2. POST /api/auth/login           → Получение JWT токена
3. POST /api/accounts/add         → Создание счёта (с токеном)
4. POST /api/cards/createCard     → Создание карты к счёту
5. POST /api/transactions/deposit → Пополнение счёта
6. POST /api/transactions/transfer→ Перевод на другой счёт
7. GET  /api/transactions/account/{id}/recent → Просмотр последних транзакций
8. POST /api/auth/logout          → Выход
```
