# Конфигурация

Основной файл — `src/main/resources/application.yml`.
Тесты — `src/test/resources/application.yml` (профиль `test`).

```yaml
spring:
  profiles:
    active: dev
  sql:
    init:
      mode: always                       # schema.sql/data.sql при каждом старте
  datasource:
    url: jdbc:h2:file:./db/filmorate     # файловый H2, данные в ./db/
    driverClassName: org.h2.Driver
    username: sa
    password: password

logging:
  level:
    org.zalando.logbook: TRACE           # без этого Logbook не выводит запросы

app:
  admin:
    email: ${ADMIN_EMAIL:admin@filmorate.local}
    login: ${ADMIN_LOGIN:admin}
    name: ${ADMIN_NAME:Administrator}
    birthday: ${ADMIN_BIRTHDAY:1990-01-01}
    password: ${ADMIN_PASSWORD:admin123}
  jwt:
    secret: ${JWT_SECRET:ZmlsbW9yYXRlLXNlY3JldC1rZXktZm9yLWp3dC1zaWduaW5nLW1pbi0zMi1ieXRlcy1sb25n}
    expirationMinutes: 60
```

## Переменные окружения

| Переменная | Назначение | Значение по умолчанию |
|---|---|---|
| `ADMIN_EMAIL` | Email админа | `admin@filmorate.local` |
| `ADMIN_LOGIN` | Login админа | `admin` |
| `ADMIN_NAME` | Имя админа | `Administrator` |
| `ADMIN_BIRTHDAY` | День рождения админа | `1990-01-01` |
| `ADMIN_PASSWORD` | Пароль админа | `admin123` |
| `JWT_SECRET` | Base64-секрет для подписи JWT (≥ 32 байта) | дефолт (только для разработки) |

⚠️ **`JWT_SECRET`** — Base64-строка, декодируется в ключ длиной ≥ 32 байта (HS256).
В продакшене обязательно задайте свой секрет через env.

⚠️ **`ADMIN_PASSWORD`** — дефолт `admin123` только для локальной разработки.
На реальном сервере переопределите.

## Профиль `test`

`src/test/resources/application.yml` активирует профиль `test` и задаёт:

- отдельный `app.jwt.secret` (не совпадает с продовым);
- `AdminInitializer` не запускается (`@Profile("!test")`);
- H2 — in-memory.

Это гарантирует, что тесты не пишут в файловую БД и не создают админа.

## Про H2

Данные сохраняются между перезапусками.

⚠️**Папку `db/` стоит добавить в `.gitignore`,
чтобы файлы H2 не попадали в репозиторий.**