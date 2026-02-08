<a id="readme-top"></a>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Оглавление</summary>
  <ol>
    <li>
      <a href="#about-the-project">О проекте</a>
      <ul>
        <li><a href="#built-with">Стек</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Быстрый старт</a>
      <ul>
        <li><a href="#prerequisites">Требования</a></li>
        <li><a href="#installation">Установка и запуск</a></li>
      </ul>
    </li>
    <li><a href="#usage">Использование</a></li>
    <li><a href="#configuration">Конфигурация</a></li>
    <li><a href="#testing">Тестирование</a></li>
    <li><a href="#roadmap">Планы</a></li>
    <li><a href="#contact">Контакты</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## О проекте

**BankSystem** — это Spring Boot приложение для банковского домена с JWT-аутентификацией, интеграцией с PostgreSQL, почтовым сервисом и простой фронтенд частью. Проект так же ориентирован на удобный локальный запуск и контейнеризацию.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Стек

* [![Java][java-shield]][java-url]
* [![Spring Boot][spring-boot-shield]][spring-boot-url]
* [![PostgreSQL][postgres-shield]][postgres-url]
* [![JWT][jwt-shield]][jwt-url]
* [![Docker][docker-shield]][docker-url]
* ![Kafka](https://img.shields.io/badge/Kafka-Event%20Streaming-231F20?style=for-the-badge&logo=apachekafka)


<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Особенности

- Idempotency для транзакций через заголовок `Idempotency-Key` + автоочистка старых записей

<!-- GETTING STARTED -->
## Быстрый старт

### Требования

- **Java 21**
- **Docker** и **Docker Compose** (для контейнерного запуска)
- **PostgreSQL** (если запускать без Docker)

### Установка и запуск

1. Скопируйте пример окружения:
   ```bash
   cp .env.example .env
   ```
2. Заполните переменные в `.env`.
3. Выберите сценарий запуска.

#### Локально (без Docker)

1. Убедитесь, что PostgreSQL запущен.
2. Укажите локальный URL базы данных, например:
   ```bash
   DB_URL=jdbc:postgresql://localhost:5432/bank_db
   ```
3. Запустите приложение:
   ```bash
   ./gradlew bootRun
   ```

#### Через Docker Compose

```bash
docker compose up --build
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->
## Использование

- Приложение будет находится на: [http://localhost:8080](http://localhost:8080)
- PostgreSQL пробрасывается на `localhost:5432` *(если порт свободен)*.
- Так же можно перейти на html страницу для просмотра общей информации [http://localhost:8080](http://localhost:8080)
- Так же учтите что в проекте стоит RateLimiterFilter с ограничениями 50 токенов в минуту

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONFIGURATION -->
## Конфигурация

- Приложение использует `.env` файл, который автоматически подхватывается через `spring.config.import: optional:file:.env[.properties]`.
- CORS настроен для dev-сценария (когда frontend запускается отдельно) и включается через профиль `dev`
- Проект использует Spring Profiles (`dev`, `test`, `prod`). Активный профиль задаётся через `SPRING_PROFILES_ACTIVE`.

| Переменная | Описание |
| --- | --- |
| `JWT_SECRET` | Секрет для подписи JWT токенов. |
| `JWT_EXPIRATION` | Время жизни токена (мс). |
| `DB_URL` | JDBC URL до PostgreSQL. |
| `DB_USERNAME` / `DB_PASSWORD` | Логин/пароль БД. |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Настройки контейнера Postgres. |
| `EMAIL_USERNAME` / `EMAIL_PASSWORD` | Данные SMTP (пример — Gmail). |
| `SPRING_PROFILES_ACTIVE` | Активный профиль Spring (`dev/test/prod`). |

<p align="right">(<a href="#readme-top">back to top</a>)</p>


## Архитектура и Kafka интеграция

**BankSystem** является частью event-driven архитектуры и взаимодействует с внешним AI-сервисом через **Apache Kafka**.

Это позволяет разделить ответственность сервисов, обеспечить асинхронную обработку запросов и повысить отказоустойчивость системы.

### Поток сообщений
- User → BankSystem → Kafka → AI Assistant → Kafka → BankSystem

1. BankSystem отправляет пользовательские запросы в Kafka  
2. AI Assistant потребляет сообщения и выполняет обработку  
3. Ответ возвращается обратно через Kafka  
4. BankSystem получает результат и завершает операцию

   ### Kafka топики

| Topic | Назначение |
|------|-------------|
| `ai_messages` | Запросы от BankSystem к AI сервису |
| `ai_answers` | Ответы AI сервиса обратно в BankSystem |

### Поток сообщений (request–reply)

AI-ассистент реализован как отдельный микросервис:

 **AI Assistant Project:**  
`https://github.com/favian1967/Ai_Assistant`

## Kafka локальный запуск

Для взаимодействия между сервисами требуется локально запущенный Kafka брокер.

Запуск single-node Kafka:

```bash
docker run -d \
  --name main_bank_kafka \
  -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_LOG_DIRS=/tmp/kraft-combined-logs \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  apache/kafka:latest
```

## API Overview

### Authentication
POST /api/auth/register  
POST /api/auth/login  
POST /api/auth/logout  
POST /api/auth/send 


### Accounts
POST   /api/accounts/add  
GET    /api/accounts/getById/{id}  
GET    /api/accounts/{id}/balance  
PATCH  /api/accounts/{id}/status  
DELETE /api/accounts/{id}/close  

### Cards
POST /api/cards/createCard  
GET  /api/cards/getMyCards  
POST /api/cards/block/{id}  
POST /api/cards/unblock/{id}  

### Transactions
POST /api/transactions/deposit  
POST /api/transactions/withdraw  
POST /api/transactions/transfer  

> Проект содержит так же другие менее значимые эндпоинты с которыми можно ознакомится в Controllers


<!-- TESTING -->
## Тестирование

```bash
./gradlew test
```

Тесты используют Testcontainers, поэтому требуется установленный Docker.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## CI

В проекте настроен GitHub Actions CI: на каждом push/PR автоматически выполняется `./gradlew test` в профиле `test`.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Database schema

<p align="center">
  <img src="src/main/resources/static/DB_DIAGRAM.png" width="800">
</p>

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->
## Контакты

Автор проекта: tg - @Rafink, x - https://x.com/Favian4747

Project Link: [https://github.com/favian1967/BankSystem](https://github.com/favian1967/BankSystem)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
[forks-shield]: https://img.shields.io/github/forks/your_username/bank-system.svg?style=for-the-badge
[forks-url]: https://github.com/your_username/bank-system/network/members
[stars-shield]: https://img.shields.io/github/stars/your_username/bank-system.svg?style=for-the-badge
[stars-url]: https://github.com/your_username/bank-system/stargazers
[issues-shield]: https://img.shields.io/github/issues/your_username/bank-system.svg?style=for-the-badge
[issues-url]: https://github.com/your_username/bank-system/issues
[license-shield]: https://img.shields.io/github/license/your_username/bank-system.svg?style=for-the-badge
[license-url]: https://github.com/your_username/bank-system/blob/main/LICENSE

[java-shield]: https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[java-url]: https://openjdk.org/
[spring-boot-shield]: https://img.shields.io/badge/Spring%20Boot-4.0.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white
[spring-boot-url]: https://spring.io/projects/spring-boot
[postgres-shield]: https://img.shields.io/badge/PostgreSQL-15-316192?style=for-the-badge&logo=postgresql&logoColor=white
[postgres-url]: https://www.postgresql.org/
[jwt-shield]: https://img.shields.io/badge/JWT-0.12.x-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white
[jwt-url]: https://github.com/jwtk/jjwt
[docker-shield]: https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white
[docker-url]: https://docs.docker.com/compose/
