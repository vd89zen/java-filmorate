# Filmorate

Бэкенд-сервис социальной сети для киноманов. Помогает решить проблему выбора фильма для просмотра:
пользователи оставляют лайки фильмам, формируют круг друзей, оставляют отзывы на фильмы и получают
персональную картину популярности — топ фильмов строится на основе реальных оценок, доступны общие
фильмы (лайкнули оба), рейтинг полезности отзывов, лента событий пользователя и его друзей.

## Содержание

- [Возможности](#возможности)
- [Стек технологий](#стек-технологий)
- [Архитектура](#архитектура)
- [Аутентификация и авторизация](#аутентификация-и-авторизация)
- [Системные требования](#системные-требования)
- [Установка и запуск](#установка-и-запуск)
- [Конфигурация](#конфигурация)
- [API](#api)
- [Модель данных](#модель-данных)
- [Обработка ошибок](#обработка-ошибок)
- [Тестирование](#тестирование)

## Возможности

- **Каталог фильмов:** CRUD по фильмам, пагинация списка.
- **Лайки и популярность:** оценка фильмов пользователями и рейтинг топ-N по количеству лайков,
  с опциональной фильтрацией по жанру и году выпуска.
- **Поиск фильмов:** по тексту (название, описание, режиссёр — регистронезависимая подстрока)
  и фильтры по году выпуска, длительности и рейтингу MPA. Результаты сортируются по популярности.
- **Рекомендации:** коллаборативная фильтрация по лайкам — «пользователи с похожими вкусами
  смотрели это, а вы ещё нет».
- **Друзья:** добавление и удаление, список друзей пользователя, поиск общих друзей.
- **Общие фильмы:** список фильмов, которые понравились обоим пользователям.
- **Отзывы:** пользователи оставляют отзывы на фильмы с оценкой (положительный/негативный),
  ставят лайки/дизлайки другим отзывам; рейтинг полезности формируется динамически,
  лента отзывов сортируется по нему. Один пользователь — один отзыв на фильм.
  Редактировать и удалять отзыв может только его автор.
- **Лента событий:** история действий пользователя и его друзей — добавление в друзья, удаление из друзей,
  лайки и снятие лайков фильмов, действия с отзывами. Доступна в двух видах: «сырая» (только id сущностей)
  и обогащённая (с вложенными краткими карточками).
- **Режиссёры:** справочник режиссёров (CRUD), связь «многие-ко-многим» с фильмами;
  выборка фильмов режиссёра с сортировкой по году выпуска или количеству лайков.
- **Справочники:** жанры фильмов и возрастные рейтинги MPA.
- **Пагинация:** постраничная выдача списков пользователей и фильмов.
- **JWT-аутентификация:** `POST /auth/login` → access-токен, роли `USER` и `ADMIN`,
  эндпоинты разделены по уровням доступа (public / authenticated / admin).

## Стек технологий

| Слой | Технология |
|---|---|
| Язык | Java 21 (совместим с Java 17) |
| Фреймворк | Spring Boot 3.2.4 |
| Web | `spring-boot-starter-web` |
| Безопасность | Spring Security + `jjwt` 0.12.5 (JWT) |
| Хеширование паролей | BCrypt (`spring-security-crypto`) |
| Валидация | `spring-boot-starter-validation` (Jakarta Bean Validation) |
| Доступ к БД | Spring JDBC |
| БД | H2 |
| Кэширование | Spring Cache |
| Логирование HTTP | Zalando Logbook 3.7.2 |
| Логирование | SLF4J |
| Кодогенерация | Lombok 1.18.38 |
| Документация API | springdoc-openapi 2.5.0 |
| Тесты | JUnit 5, AssertJ, Spring Security Test |
| Сборка | Maven |
| Качество кода | Checkstyle 3.3.1 |

## Архитектура

**REST API с архитектурой, рассчитанной на масштабирование.**

```
controller  →  service  →  dal (storage + row mapper)  →  H2
     ↑              ↑
   DTO         model / mapper
```

| Пакет | Назначение |
|---|---|
| `controller/pub` | Публичные REST-контроллеры (доступ без аутентификации) |
| `controller/auth` | Контроллеры для залогиненных (`/me/**`) |
| `controller/admin` | Контроллеры только для роли `ADMIN` (`/admin/**`) |
| `controller` | `AuthController`, `GlobalExceptionHandler` |
| `service` | Бизнес-логика (`FilmService`, `UserService`, `EventService`, `FeedService`, `ReviewService`, `DirectorService`, `GenreService`, `RatingMpaaService`) |
| `dal` | Репозитории на `JdbcOperations` |
| `dal.mappers` | `RowMapper`-реализации |
| `dto` | DTO запросов/ответов, включая краткие `FilmShortDto` / `UserShortDto` / `ReviewShortDto` для лент |
| `mapper` | Статические мапперы DTO ⇄ домен |
| `model` | Доменные модели + enums `EventTypes`, `OperationTypes`, `Role` |
| `security` | JWT-фильтр, `JwtService`, `UserDetailsServiceImpl`, `UserPrincipal` |
| `config` | `SecurityConfig`, `JwtProperties`, `AdminProperties`, `AdminInitializer`, `PasswordEncoderConfig`, `OpenApiConfig` |
| `exception` | `NotFoundException`, `ValidationException` |

### Разделение ответственности

- **`FeedService`** — обогащение «сырых» событий из `EventService` вложенными краткими DTO.
  Обогащение делается batch-запросами.
- **Нормализация данных** (`email`, `login` — приведение к нижнему регистру) выполняется
  в сервисном слое (`UserService.normalizeUser`). Email в JSON-теле дополнительно тримится
  десериализатором `TrimDeserializer` **до** Bean Validation — так `@Email` работает на уже
  очищенном значении, а пользователь может вставить `"  user@mail.com  "`.
- **Пароли** хранятся только как BCrypt-хеши. Открытый пароль никогда не сохраняется и не логируется.
- **Рейтинг полезности отзыва** (`useful`) хранится денормализованно в таблице `reviews`
  и меняется атомарно (`UPDATE reviews SET useful = useful ± 1`) в одной транзакции
  с записью мнения в `review_opinions`. Лайки/дизлайки **отзывов** событий в ленте не создают —
  событие пишется только при `REVIEW/ADD|UPDATE|REMOVE`.
- **Проверка владельца отзыва** при `update`/`delete` — сервис сверяет `review.userId` с `userId`
  из токена; при несовпадении возвращается `404`, чтобы не раскрывать факт существования чужих отзывов.
- **Порядок в выборках, где он не задаётся SQL,** восстанавливается явно в сервисе
  (например, `getTopPopularFilms` сортирует фильмы по `LinkedHashMap` из `filmLikesDbStorage`,
  потому что `findBySeveralIds` возвращает фильмы в произвольном порядке СУБД).

## Аутентификация и авторизация

Используется **JWT (Bearer)**, `SessionCreationPolicy.STATELESS`. Пароли хешируются BCrypt.

### Уровни доступа

| Группа | Префикс / эндпоинт | Доступ |
|---|---|---|
| Public | `/auth/**`, `GET /films/**`, `GET /directors/**`, `GET /genres/**`, `GET /mpa/**`, `GET /reviews/**`, `POST /users`, `GET /users/{id}` | без аутентификации |
| Authenticated | `/me/**` | любой залогиненный пользователь |
| Admin | `/admin/**` | роль `ADMIN` |

**Принцип:** `userId` **никогда не передаётся в URL** для авторизованных эндпоинтов — он берётся
из токена через `@AuthenticationPrincipal UserPrincipal principal`. То есть нельзя подсмотреть
чужой id и что-то от его имени сделать — сервер просто не читает id из пути.

### Логин

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

Дальше токен передаётся в заголовке:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Неверный пароль или несуществующий email → `401 Unauthorized`.
Запрос к `/me/**` без токена → `401`. Запрос к `/admin/**` с токеном без роли `ADMIN` → `403`.

### Роли

Одна роль на пользователя: `USER` (по умолчанию) или `ADMIN`. Роль зашита в JWT-клейм `role`
и превращается в `GrantedAuthority` вида `ROLE_USER` / `ROLE_ADMIN`.

### Администратор

При старте приложения (профиль, отличный от `test`) `AdminInitializer` создаёт администратора,
если его ещё нет:

| Поле | Источник | Значение по умолчанию |
|---|---|---|
| Email | `ADMIN_EMAIL` | `admin@filmorate.local` |
| Login | `ADMIN_LOGIN` | `admin` |
| Name | `ADMIN_NAME` | `Administrator` |
| Birthday | `ADMIN_BIRTHDAY` | `1990-01-01` |
| Password | `ADMIN_PASSWORD` | `admin123` |

⚠️ Дефолтные значения — только для локальной разработки. В проде обязательно переопределяйте
`ADMIN_EMAIL` и `ADMIN_PASSWORD` через env.

### Swagger UI

В Swagger UI есть кнопка **Authorize** с Bearer-схемой. Порядок:

1. Выполнить `POST /auth/login`.
2. Скопировать `accessToken` из ответа.
3. Нажать **Authorize** → вставить токен **без** префикса `Bearer` (springdoc сам его добавит).
4. Теперь защищённые ручки подписываются автоматически.

Для `/admin/**` логиньтесь под админом (`admin@filmorate.local` / `admin123` в dev-профиле),
для `/me/**` — под любым зарегистрированным пользователем.

## Системные требования

| Компонент | Требование |
|---|---|
| **JDK** | **21** (или 17 — см. ниже) |
| Maven | 3.6+ |
| ОС | любая с поддержкой JDK 17+ |
| Свободный порт | 8080 |

### Про версию Java

Проект собирается на JDK 21.
Если у вас JDK 17, проект тоже соберётся — код совместим с Java 17. Для этого при сборке передайте свойство:

```bash
mvn clean package -Djava.version=17
```

либо измените версию в pom:

```xml
<java.version>17</java.version>
```

JDK 16 и ниже не подойдут: Spring Boot 3.2.4 требует минимум Java 17.

## Установка и запуск

### 1. Клонировать репозиторий

```bash
git clone https://github.com/vd89zen/java-filmorate.git
cd java-filmorate
```

### 2. Собрать

```bash
mvn clean package
```

Во время сборки запускается Checkstyle и валит её при нарушениях стиля. Чтобы временно пропустить проверку:

```bash
mvn clean package -Dcheckstyle.skip=true
```

### 3. Запустить

```bash
java -jar target/filmorate-0.0.1-SNAPSHOT.jar
```

Приложение стартует на `http://localhost:8080`.

### 4. Проверить работоспособность

Публичные эндпоинты — без токена:

```bash
curl http://localhost:8080/films?from=0&size=10
curl http://localhost:8080/genres
curl http://localhost:8080/mpa
curl http://localhost:8080/directors
curl http://localhost:8080/reviews?count=10
```

Регистрация и логин:

```bash
# Регистрация
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
        "email": "user@example.com",
        "login": "userlogin",
        "name": "Иван",
        "birthday": "1990-01-15",
        "password": "secret123"
      }'

# Логин
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "secret123"}'
```

Ответ в формате JSON — сервис работает.
Интерактивная документация — `http://localhost:8080/swagger-ui.html`.

## Конфигурация

Настройки находятся в `src/main/resources/application.yml`.
Тесты используют `src/test/resources/application.yml` (профиль `test`).

```yaml
spring:
  profiles:
    active: dev
  sql:
    init:
      mode: always                       # schema.sql/data.sql выполняются при каждом старте
  datasource:
    url: jdbc:h2:file:./db/filmorate     # файловый режим H2, данные в ./db/
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

Данные сохраняются между перезапусками:
скрипты инициализации не создают дубликатов и не стирают существующие записи.

⚠️ **`JWT_SECRET`** — Base64-строка, декодируется в ключ длиной **≥ 32 байта** (HS256).
В проде задавайте свой секрет через env, не оставляйте дефолт.

> 💡 Папку `db/` стоит добавить в `.gitignore`, чтобы файлы H2 не попадали в репозиторий.

## API

Базовый URL: `http://localhost:8080`. Интерактивная документация — Swagger UI:

| URL | Что это |
|---|---|
| `/swagger-ui.html` | Интерфейс Swagger UI |
| `/v3/api-docs` | OpenAPI-спецификация (JSON) |
| `/v3/api-docs.yaml` | То же в YAML |

### Аутентификация

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `POST` | `/auth/login` | Логин, выдача access-токена | `200 OK` + `AuthResponse` |

### Пагинация

Все «списочные» эндпоинты (`GET /films`, `GET /admin/users`) поддерживают query-параметры:

| Параметр | По умолчанию | Ограничение |
|---|---|---|
| `from` | `0` | `>= 0` |
| `size` | `10` | `1..100` |

Если `from` или `size` выходят за границы — возвращается `400 Bad Request`.

### Пользователи (публичное)

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `POST` | `/users` | Регистрация | `201 Created` + `UserDto` |
| `GET` | `/users/{userId}` | Публичный профиль (`UserPublicDto`, без `email`/`password`/`role`) | `200 OK` + `UserPublicDto` |

**Пример регистрации:**

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
        "email": "user@example.com",
        "login": "userlogin",
        "name": "Иван",
        "birthday": "1990-01-15",
        "password": "secret123"
      }'
```

Поля `password` — обязательное, `6..100` символов. Если `name` не указан или пуст —
в качестве имени будет использован `login`. Email уникален, повторная попытка → `400`.

### Пользователи (`/me`, требуют токен)

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `PUT` | `/me` | Обновить свой профиль | `200 OK` + `UserDto` |
| `DELETE` | `/me` | Удалить свой аккаунт | `204 No Content` |
| `PUT` | `/me/friends/{friendId}` | Добавить друга | `204 No Content` |
| `DELETE` | `/me/friends/{friendId}` | Удалить друга | `204 No Content` |
| `GET` | `/me/friends` | Список друзей | `200 OK` + `[UserPublicDto]` |
| `GET` | `/me/friends/common/{friendId}` | Общие друзья | `200 OK` + `[UserPublicDto]` |
| `GET` | `/me/recommendations` | Рекомендации фильмов | `200 OK` + `[FilmDto]` |

`userId` берётся из токена. В `PUT /me` поле `id` в теле игнорируется — подставляется id
из `principal`.

**Рекомендации фильмов**

Возвращает фильмы, рекомендованные пользователю, отсортированные по силе рекомендации.
Алгоритм — коллаборативная фильтрация:

1. Находятся пользователи, которые лайкнули те же фильмы, что и целевой.
2. Определяются фильмы, которые эти пользователи лайкнули, а целевой — нет.
3. Результат сортируется по сумме пересечений с похожими пользователями: фильм,
   который лайкнули пользователи с большим пересечением, весит больше.

Если у пользователя нет лайков или нет похожих — возвращается пустой массив.

### Фильмы (публичное)

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `GET` | `/films?from=0&size=10` | Страница фильмов | `200 OK` + `[FilmDto]` |
| `GET` | `/films/{filmId}` | Получить фильм | `200 OK` + `FilmDto` |
| `GET` | `/films/popular?count=10&genreId=&year=` | Топ-N по лайкам | `200 OK` + `[FilmDto]` |
| `GET` | `/films/director/{directorId}?sortBy=year\|likes` | Фильмы режиссёра | `200 OK` + `[FilmDto]` |
| `GET` | `/films/search?...` | Поиск фильмов | `200 OK` + `[FilmDto]` |

**Топ популярных фильмов**

| Параметр | Обязательный | Описание |
|---|---|---|
| `count` | нет (по умолчанию `10`) | Сколько фильмов вернуть |
| `genreId` | нет | Фильтр по жанру |
| `year` | нет | Фильтр по году выпуска |

Сортировка — по убыванию количества лайков, при равенстве — по возрастанию `id`.
Фильмы без лайков в топ не попадают. Если указан `genreId`, но жанра нет — `404`.

**Фильмы режиссёра**

| Параметр | По умолчанию | Допустимые значения |
|---|---|---|
| `sortBy` | `year` | `year`, `likes` |

- `year` — по возрастанию даты релиза;
- `likes` — по убыванию количества лайков.

Несуществующий режиссёр → `404`, `sortBy=bad` → `400`.

**Поиск фильмов**

```http
GET /films/search?query=нолан&by=director,title&yearFrom=2000&yearTo=2020&mpaIds=2,3&from=0&size=10
```

| Параметр | Тип | Описание |
|---|---|---|
| `query` | строка | Текст поиска (подстрока, регистронезависимо) |
| `by` | строка | Где искать: `title`, `director`, `description` через запятую. По умолчанию `title`, если задан `query` |
| `year` | int | Точный год выпуска |
| `yearFrom` / `yearTo` | int | Диапазон годов включительно |
| `duration` | int | Точная длительность в минутах |
| `durationFrom` / `durationTo` | int | Диапазон длительностей включительно |
| `mpaIds` | список int через запятую | Фильтр по рейтингам MPA |
| `from` / `size` | int | Пагинация |

Правила:

- Все фильтры соединяются через `AND`. Внутри `query` — `OR` между выбранными `by`.
- `year` и `yearFrom`/`yearTo` взаимоисключающие — иначе `400`.
- `duration` и `durationFrom`/`durationTo` взаимоисключающие — иначе `400`.
- Если `by` задан, а `query` пуст — `400`.
- Спецсимволы LIKE (`%`, `_`) в `query` трактуются буквально.
- Без параметров возвращает всё, отсортированное по лайкам.

### Фильмы (`/me`, требуют токен)

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `PUT` | `/me/films/{filmId}/like` | Поставить лайк | `204 No Content` |
| `DELETE` | `/me/films/{filmId}/like` | Снять лайк | `204 No Content` |
| `GET` | `/me/films/common?friendId={id}` | Общие фильмы двух пользователей | `200 OK` + `[FilmDto]` |

Первый пользователь всегда берётся из токена. В `getCommonFilms` `friendId` — обязателен,
иначе `400`.

### Отзывы (публичное)

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `GET` | `/reviews/{id}` | Получить отзыв | `200 OK` + `ReviewDto` |
| `GET` | `/reviews?filmId=&count=10` | Отзывы по фильму (или все) | `200 OK` + `[ReviewDto]` |

`count` ограничен значением `100`.

### Отзывы (`/me`, требуют токен)

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `POST` | `/me/reviews` | Создать отзыв | `201 Created` + `ReviewDto` |
| `PUT` | `/me/reviews` | Обновить свой отзыв | `200 OK` + `ReviewDto` |
| `DELETE` | `/me/reviews/{id}` | Удалить свой отзыв | `204 No Content` |
| `PUT` | `/me/reviews/{id}/like` | Поставить лайк | `204 No Content` |
| `PUT` | `/me/reviews/{id}/dislike` | Поставить дизлайк | `204 No Content` |
| `DELETE` | `/me/reviews/{id}/like` | Снять лайк | `204 No Content` |
| `DELETE` | `/me/reviews/{id}/dislike` | Снять дизлайк | `204 No Content` |

Автор отзыва берётся из токена. Один пользователь — один отзыв на фильм
(`UNIQUE (user_id, film_id)`), повторная попытка → `400`.

**`update`/`delete` чужого отзыва → `404`** (не раскрываем существование чужих отзывов).

`useful` начинается с 0: лайк увеличивает на 1, дизлайк уменьшает на 1.
Сортировка — по убыванию `useful`, при равенстве — по возрастанию `reviewId`.

**Создание отзыва:**

```bash
curl -X POST http://localhost:8080/me/reviews \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "content": "This film is sooo baad.",
        "isPositive": false,
        "filmId": 1
      }'
```

Обратите внимание: `userId` в теле **нет** — он подставляется из токена.

`PUT /me/reviews` — частичное обновление: можно передать только `content`, только `isPositive`
или оба. Пустой запрос → `400`.

### Лента событий (требует токен)

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `GET` | `/me/feed/user` | События пользователя (сырые) | `200 OK` + `[EventDto]` |
| `GET` | `/me/feed/friends` | События друзей (сырые) | `200 OK` + `[EventDto]` |
| `GET` | `/me/feed/user/enriched` | События пользователя (обогащённые) | `200 OK` + `[EnrichedEventDto]` |
| `GET` | `/me/feed/friends/enriched` | События друзей (обогащённые) | `200 OK` + `[EnrichedEventDto]` |

В ленту попадают события:

- `LIKE/ADD` и `LIKE/REMOVE` — лайки **фильмов** (в ленте друзей — только от друзей);
- `FRIEND/ADD` и `FRIEND/REMOVE` — добавление в друзья / удаление из друзей;
- `REVIEW/ADD`, `REVIEW/UPDATE`, `REVIEW/REMOVE` — действия с отзывами (в ленте друзей — только от друзей).

Лайки/дизлайки **отзывов** событий не создают — меняется только `useful` у отзыва.

Пример «сырого» события:

```json
{
  "eventId": 42,
  "timestamp": 1700000000000,
  "userId": 7,
  "eventType": "LIKE",
  "operation": "ADD",
  "entityId": 13
}
```

`eventType` — одно из `LIKE`, `REVIEW`, `FRIEND`; `operation` — одно из `ADD`, `REMOVE`, `UPDATE`.

Пример обогащённого события `LIKE`:

```json
{
  "eventId": 42,
  "timestamp": 1700000000000,
  "userId": 7,
  "eventType": "LIKE",
  "operation": "ADD",
  "film": { "id": 13, "name": "Inception", "releaseDate": "2010-07-16" },
  "user": null,
  "review": null
}
```

Пример обогащённого события `REVIEW`:

```json
{
  "eventId": 51,
  "timestamp": 1700000000000,
  "userId": 7,
  "eventType": "REVIEW",
  "operation": "ADD",
  "film": null,
  "user": null,
  "review": {
    "reviewId": 13,
    "content": "This film is sooo baad.",
    "isPositive": false,
    "film": { "id": 13, "name": "Inception", "releaseDate": "2010-07-16" }
  }
}
```

Поля `film` / `user` / `review` заполняются в зависимости от типа события:
`film` — для `LIKE`, `user` — для `FRIEND`, `review` — для `REVIEW`.
Поле `entityId` в обогащённый ответ не включается — его роль выполняет
`film.id` / `user.id` / `review.reviewId`.

Для `REVIEW/REMOVE` поле `review` будет `null`: отзыв уже удалён, в событии остаётся только
факт удаления.

### Справочники (публичное)

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `GET` | `/genres` | Все жанры | `200 OK` + `[GenreDto]` |
| `GET` | `/genres/{genreId}` | Жанр по id | `200 OK` + `GenreDto` |
| `GET` | `/mpa` | Все рейтинги MPA | `200 OK` + `[RatingMpaaDto]` |
| `GET` | `/mpa/{ratingId}` | Рейтинг по id | `200 OK` + `RatingMpaaDto` |
| `GET` | `/directors` | Все режиссёры | `200 OK` + `[DirectorDto]` |
| `GET` | `/directors/{id}` | Режиссёр по id | `200 OK` + `DirectorDto` |

Справочники предзаполнены при первом запуске: **6 жанров** и **5 рейтингов MPA**.
Данные сохраняются между перезапусками.

**Как добавить новый жанр**

Справочник жанров предзаполняется в `src/main/resources/data.sql`.
Чтобы добавить жанр, допишите строку по образцу:

```sql
INSERT INTO genres (name)
SELECT 'Новый жанр' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Новый жанр');
```

После перезапуска приложения жанр появится в `GET /genres`.

### Admin (только роль `ADMIN`)

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `POST` | `/admin/films` | Создать фильм | `201 Created` + `FilmDto` |
| `PUT` | `/admin/films` | Обновить фильм | `200 OK` + `FilmDto` |
| `DELETE` | `/admin/films/{filmId}` | Удалить фильм | `204 No Content` |
| `GET` | `/admin/users?from=0&size=10` | Страница пользователей | `200 OK` + `[UserDto]` |
| `DELETE` | `/admin/users/{userId}` | Удалить пользователя | `204 No Content` |
| `POST` | `/admin/directors` | Создать режиссёра | `201 Created` + `DirectorDto` |
| `PUT` | `/admin/directors` | Обновить режиссёра | `200 OK` + `DirectorDto` |
| `DELETE` | `/admin/directors/{id}` | Удалить режиссёра | `204 No Content` |

**Создание фильма (админ):**

```bash
curl -X POST http://localhost:8080/admin/films \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Inception",
        "description": "A thief who steals corporate secrets...",
        "releaseDate": "2010-07-16",
        "duration": 148,
        "mpa": { "id": 1 },
        "genres": [ { "id": 1 }, { "id": 2 } ],
        "directors": [ { "id": 1 } ]
      }'
```

Обратите внимание: `mpa`, элементы `genres` и `directors` передаются как объекты с полем `id` —
это контракт API.

**Создание режиссёра:**

```bash
curl -X POST http://localhost:8080/admin/directors \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "name": "Christopher Nolan" }'
```

Имя режиссёра уникально (`UNIQUE (name)`): повторная попытка → `400`.

Без токена → `401`. С токеном без роли `ADMIN` → `403`.

## Модель данных

Реляционная схема, H2.

| Таблица | Колонки |
|---|---|
| `users` | `id`, `email`, `login`, `name`, `birthday`, `password`, `role`, `created_at` |
| `films` | `id`, `name`, `description`, `release_date`, `duration`, `rating_mpaa_id` |
| `genres` | `id`, `name` |
| `rating_mpaa` | `id`, `name` |
| `film_genres` | `film_id`, `genre_id` (many-to-many) |
| `film_likes` | `film_id`, `user_id` |
| `friendship` | `user_id`, `friend_id` |
| `directors` | `id`, `name` (`UNIQUE`) |
| `film_directors` | `film_id`, `director_id` (many-to-many) |
| `reviews` | `id`, `content`, `is_positive`, `user_id`, `film_id`, `useful`, `UNIQUE (user_id, film_id)` |
| `review_opinions` | `review_id`, `user_id`, `is_useful` (PK: `review_id, user_id`) |
| `events` | `id`, `time_stamp`, `user_id`, `event_type_id`, `operation_type_id`, `entity_id` |
| `event_types` | `id`, `name` (`LIKE`, `REVIEW`, `FRIEND`) |
| `operation_types` | `id`, `name` (`ADD`, `UPDATE`, `REMOVE`) |

Особенности:

- `users.password` — BCrypt-хеш, никогда не отдаётся наружу (`UserPublicDto` его не содержит).
- `users.role` — `USER` или `ADMIN`, `VARCHAR(20)`.
- `users.created_at` — момент регистрации, `TIMESTAMP`.
- Каскадное удаление настроено для `film_likes`, `friendship`, `film_directors`, `reviews`,
  `review_opinions`, `events` — при удалении пользователя или фильма связанные строки подчищаются
  автоматически.

## Обработка ошибок

Все ошибки возвращаются в едином формате. Формат ответа (`ErrorResponse`):

```json
{
  "errors": [
    {
      "field": "email",
      "message": "Неверный формат адреса электронной почты.",
      "rejectedValue": "invalid-email"
    }
  ],
  "timestamp": "2026-09-16T15:20:11.123"
}
```

| Исключение | HTTP-статус |
|---|---|
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `ConstraintViolationException` | `400 Bad Request` |
| `ValidationException` | `400 Bad Request` |
| `MissingServletRequestParameterException` | `400 Bad Request` |
| `NotFoundException` | `404 Not Found` |
| `NoResourceFoundException` | `404 Not Found` |
| `HttpRequestMethodNotSupportedException` | `405 Method Not Allowed` |
| `AuthenticationException` (в т.ч. `BadCredentialsException`, `UsernameNotFoundException`) | `401 Unauthorized` |
| Анонимный доступ к защищённому эндпоинту (Security) | `401 Unauthorized` |
| Доступ без нужной роли (Security) | `403 Forbidden` |
| любое другое | `500 Internal Server Error` |

**Разделение ответственности за коды 401/403:** два разных слоя.

- **Spring Security** отвечает за 401/403, когда запрос не доходит до контроллера
  (анонимный на `/me/**` → 401, аутентифицированный без `ADMIN` на `/admin/**` → 403).
  Настраивается в `SecurityConfig` через `exceptionHandling`.
- **`GlobalExceptionHandler`** отвечает за те же коды, когда исключение возникло **внутри**
  контроллера (`BadCredentialsException` из `AuthController.login` → 401).

## Тестирование

```bash
mvn test
```

Тесты интеграционные: поднимают Spring-контекст и работают с настоящей H2 (in-memory),
mock-фреймворки не используются. Покрыты все слои:

- **Хранилища** (`dal`) — CRUD, выборки, связи, краевые случаи (пустые коллекции, `null`, дубли).
- **Сервисы** — бизнес-логика, валидация, транзакционность, события, проверки владельца.
- **Контроллеры** — REST-эндпоинты через `MockMvc`, коды ответов, валидация query-параметров,
  уровни доступа (`pub` / `auth` / `admin`), JWT-аутентификация и роли.

Тесты используют `@Nested` для группировки сценариев и очищают БД между запусками,
поэтому их можно запускать в любом порядке. `src/test/resources/application.yml` активирует
профиль `test` — `AdminInitializer` в тестах не запускается, а `app.jwt.secret` задан
отдельным тестовым значением (не совпадает с продовым).

---
Проект разработан в рамках программы по Java-разработке на платформе Яндекс Практикум.
Реализация и дальнейшие доработки выполнены автором.