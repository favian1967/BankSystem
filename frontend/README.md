# Aurora Bank — Frontend

Современный интерфейс интернет-банка (React + Vite + TypeScript) поверх Bank_System API.
Тёмная финтех-тема, стеклянные карточки, графики cash flow, выпуск/заморозка карт,
переводы с idempotency и встроенный AI-ассистент.

<img alt="stack" src="https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=white" />
<img alt="vite" src="https://img.shields.io/badge/Vite-5-646CFF?logo=vite&logoColor=white" />
<img alt="ts" src="https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white" />

---

## Что внутри

| Раздел | Возможности |
|--------|-------------|
| **Overview** | Суммарный баланс по валютам, доход/расход за 30 дней, график cash flow (14 дней), последние операции, избранная карта |
| **Accounts** | Открытие счёта, пополнение, снятие, заморозка/разморозка, закрытие |
| **Cards** | Выпуск карты (VISA/Mastercard/МИР), красивый визуал карты, показать номер, заморозка, удаление, показ CVV один раз |
| **Transfer** | Перевод по номеру счёта с live-превью, защита от двойной отправки (Idempotency-Key) |
| **Transactions** | История с пагинацией и фильтром по типу |
| **AI Assistant** | Плавающий чат поверх `/api/helper` (работает при запущенном Kafka-воркере, иначе деградирует мягко) |

---

## Быстрый запуск

### 1. Запусти backend (порт 8080)

Из корня проекта (не из `frontend/`):

```bash
# нужен .env с DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, JWT_EXPIRATION, EMAIL_* и т.д.
docker compose up -d          # Postgres + Redis + Kafka + приложение
```

Backend уже настроен на CORS для `http://localhost:5173`, так что фронт подключится без проблем.

> Если поднимаешь бэкенд из IDE — активируй профиль `dev`
> (`SPRING_PROFILES_ACTIVE=dev`), тогда доступны Swagger и админ-эндпоинты для сидинга.

### 2. Запусти frontend

```bash
cd frontend
npm install
npm run dev
```

Открой **http://localhost:5173**

В dev-режиме все запросы на `/api/*` **проксируются** на `http://localhost:8080`
(см. `vite.config.ts`) — CORS и абсолютные URL не нужны.

### 3. Первый сценарий (демо за 30 секунд)

1. Нажми **Create account** → зарегистрируйся (телефон в формате `+7XXXXXXXXXX`, пароль ≥ 8 символов).
2. Раздел **Accounts** → **Open account** (например, RUB / CHECKING).
3. На счёте нажми **Top up** и внеси, скажем, `50000` — увидишь, как оживает дашборд.
4. **Cards** → **Issue card** — выпусти карту, посмотри визуал и CVV.
5. **Transfer** — переведи деньги на номер другого счёта.
6. Кликни фиолетовую кнопку ✨ внизу справа — поговори с AI-ассистентом.

---

## Конфигурация

Скопируй `.env.example` в `.env` при необходимости:

```bash
# Оставь пустым, чтобы использовать dev-прокси (рекомендуется для разработки)
VITE_API_URL=
# Куда прокси форвардит /api в dev
VITE_API_TARGET=http://localhost:8080
```

Для **продакшн-сборки**, когда фронт и API на разных доменах, укажи абсолютный URL:

```bash
VITE_API_URL=https://api.your-domain.com
```

---

## Сборка для продакшена

```bash
npm run build      # tsc + vite build → dist/
npm run preview    # локальный предпросмотр собранного билда
```

Артефакт — статические файлы в `dist/`. Варианты деплоя:

- **Отдельно** (Vercel/Netlify/Nginx) — задай `VITE_API_URL` на адрес бэкенда.
- **Из Spring** — скопируй содержимое `dist/` в `src/main/resources/static/` бэкенда
  и раздавай с того же origin (тогда `VITE_API_URL` можно оставить пустым).

---

## Структура

```
src/
├── api/            # client (fetch + JWT + Idempotency-Key) и типы
├── auth/           # AuthContext: логин/регистрация, декодирование JWT
├── components/     # Layout, BankCard, TransactionRow, AssistantWidget, общие
├── lib/            # форматтеры, работа с JWT, агрегация транзакций, useBankData
├── pages/          # Login, Dashboard, Accounts, Cards, Transfer, Transactions
├── ui/             # Toast, Modal, icons
└── styles/         # design-система (index.css) + стили приложения (app.css)
```

## Стек

React 18 · Vite 5 · TypeScript 5 · React Router 6 · Recharts · без UI-библиотек
(вся дизайн-система на чистом CSS с токенами).
