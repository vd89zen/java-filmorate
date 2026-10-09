# Аутентификация и авторизация

**JWT (Bearer)**, `SessionCreationPolicy.STATELESS`. Пароли — BCrypt.

## Уровни доступа

| Группа | Эндпоинты | Доступ |
|---|---|---|
| Public | `/auth/**`, `GET /films/**`, `GET /directors/**`, `GET /genres/**`, `GET /mpa/**`, `GET /reviews/**`, `POST /users`, `GET /users/{id}` | без аутентификации |
| Authenticated | `/me/**` | любой залогиненный |
| Admin | `/admin/**` | роль `ADMIN` |

**Принцип:** для авторизованных эндпоинтов `userId` **никогда не передаётся в URL** — берётся
из токена через `@AuthenticationPrincipal UserPrincipal principal`.

## Логин

```http
POST /auth/login
Content-Type: application/json

{ "email": "user@example.com", "password": "secret123" }
```

Ответ:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Дальше:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Коды ответов:

| Ситуация | Код |
|---|---|
| Успех | `200` |
| Неверный пароль | `401` |
| Несуществующий email | `401` |
| Пустой email/пароль | `400` |
| Некорректный email | `400` |
| Запрос к `/me/**` без токена | `401` |
| Запрос к `/admin/**` с токеном без `ADMIN` | `403` |

## Роли

Одна роль на пользователя: `USER` (по умолчанию) или `ADMIN`. Роль в JWT-клейме `role`
превращается в `GrantedAuthority` вида `ROLE_USER` / `ROLE_ADMIN`.

## Администратор

`AdminInitializer` (профиль ≠ `test`) создаёт админа, если его нет:

| Поле | Env | По умолчанию |
|---|---|---|
| Email | `ADMIN_EMAIL` | `admin@filmorate.local` |
| Login | `ADMIN_LOGIN` | `admin` |
| Name | `ADMIN_NAME` | `Administrator` |
| Birthday | `ADMIN_BIRTHDAY` | `1990-01-01` |
| Password | `ADMIN_PASSWORD` | `admin123` |

⚠️ Дефолты — только для локальной разработки.

## Swagger UI

1. `POST /auth/login` → скопировать `accessToken`.
2. Кнопка **Authorize** → вставить токен **без** префикса `Bearer` (springdoc добавит сам).
3. Защищённые ручки подписываются автоматически.

Для `/admin/**` логиньтесь под админом, для `/me/**` — под обычным пользователем.

## Разделение ответственности за 401/403

Два разных слоя:

- **Spring Security** — если запрос не дошёл до контроллера (анонимный на `/me/**` → 401,
  без `ADMIN` на `/admin/**` → 403). Настраивается в `SecurityConfig.exceptionHandling`.
- **`GlobalExceptionHandler`** — если исключение возникло внутри контроллера
  (`BadCredentialsException` из `AuthController.login` → 401).