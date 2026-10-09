# Архитектура

## Слои

```
controller  →  service  →  dal (storage + row mapper)  →  H2
     ↑              ↑
   DTO         model / mapper
```

## Пакеты

| Пакет | Назначение |
|---|---|
| `controller/pub` | Публичные эндпоинты (без аутентификации) |
| `controller/auth` | Для залогиненных (`/me/**`) |
| `controller/admin` | Только роль `ADMIN` (`/admin/**`) |
| `controller` | `AuthController`, `GlobalExceptionHandler` |
| `service` | Бизнес-логика |
| `dal` | Репозитории на `JdbcOperations` |
| `dal.mappers` | `RowMapper`-реализации |
| `dto` | DTO запросов/ответов (включая краткие `*ShortDto` для лент) |
| `mapper` | Статические мапперы DTO ⇄ домен |
| `model` | Доменные модели + enums `EventTypes`, `OperationTypes`, `Role` |
| `security` | `JwtService`, `JwtAuthenticationFilter`, `UserDetailsServiceImpl`, `UserPrincipal` |
| `config` | `SecurityConfig`, `JwtProperties`, `AdminProperties`, `AdminInitializer`, `PasswordEncoderConfig`, `OpenApiConfig` |
| `exception` | `NotFoundException`, `ValidationException` |

## Разделение ответственности

- **`FeedService`** — обогащение «сырых» событий из `EventService` вложенными краткими DTO,
  batch-запросами (без N+1).
- **Нормализация email/login** — в `UserService.normalizeUser` (trim + toLowerCase).
  Email в JSON-теле дополнительно тримится `TrimDeserializer` **до** Bean Validation —
  так `@Email` работает на чистом значении, а пользователь может вставить
  `"  user@mail.com  "`.
- **Пароли** хранятся только как BCrypt-хеши. Открытый пароль никогда не сохраняется и не логируется.
- **`useful` отзыва** хранится денормализованно и меняется атомарно
  (`UPDATE reviews SET useful = useful ± 1`) в одной транзакции с записью в `review_opinions`.
  Лайки/дизлайки отзывов события в ленте **не** создают — событие только при `REVIEW/ADD|UPDATE|REMOVE`.
- **Проверка владельца отзыва** при `update`/`delete` — `ReviewService` сверяет `review.userId`
  с `userId` из токена; при несовпадении возвращает `404`, чтобы не раскрывать
  существование чужих отзывов.
- **Порядок в выборках, где SQL его не задаёт,** восстанавливается явно в сервисе
  (`findBySeveralIds` возвращает строки в произвольном порядке СУБД).