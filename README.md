# Filmorate

Бэкенд-сервис социальной сети для киноманов. Помогает решить проблему выбора фильма для просмотра:
пользователи оставляют лайки фильмам, формируют круг друзей, оставляют отзывы на фильмы и получают
персональную картину популярности — топ фильмов строится на основе реальных оценок, доступны общие
фильмы (лайкнули оба), рейтинг полезности отзывов, лента событий пользователя и его друзей.

## Содержание

- [Возможности](#возможности)
- [Стек технологий](#стек-технологий)
- [Архитектура](#архитектура)
- [Системные требования](#системные-требования)
- [Установка и запуск](#установка-и-запуск)
- [Конфигурация](#конфигурация)
- [API](#api)
- [Модель данных](#модель-данных)
- [Обработка ошибок](#обработка-ошибок)
- [Тестирование](#тестирование)

## Возможности

- **Каталог фильмов:** создание фильма, его редактирование, удаление, просмотр списка и отдельного фильма.
- **Лайки и популярность:** оценка фильмов пользователями и рейтинг топ-N по количеству лайков.
- **Друзья:** добавление и удаление, список друзей пользователя, поиск общих друзей.
- **Общие фильмы:** список фильмов, которые понравились обоим пользователям.
- **Отзывы:** пользователи оставляют отзывы на фильмы с оценкой (положительный/негативный),
  ставят лайки/дизлайки другим отзывам; рейтинг полезности формируется динамически,
  лента отзывов сортируется по нему. Один пользователь — один отзыв на фильм.
- **Лента событий:** история действий пользователя и его друзей — добавление в друзья, удаление из друзей,
  лайки и снятие лайков фильмов, действия с отзывами. Доступна в двух видах: «сырая» (только id сущностей)
  и обогащённая (с вложенными краткими карточками).
- **Режиссёры:** справочник режиссёров (CRUD), связь «многие-ко-многим» с фильмами;
  выборка фильмов режиссёра с сортировкой по году выпуска или количеству лайков.
- **Справочники:** жанры фильмов и возрастные рейтинги MPA.
- **Пагинация:** постраничная выдача списков пользователей и фильмов с параметрами.

## Стек технологий

| Слой | Технология |
|---|---|
| Язык | Java 21 (совместим с Java 17) |
| Фреймворк | Spring Boot 3.2.4 |
| Web | `spring-boot-starter-web` |
| Валидация | `spring-boot-starter-validation` (Jakarta Bean Validation) |
| Доступ к БД | Spring JDBC |
| БД | H2 |
| Кэширование | Spring Cache |
| Логирование HTTP | Zalando Logbook 3.7.2 |
| Логирование | SLF4J |
| Кодогенерация | Lombok 1.18.38 |
| Документация API | springdoc-openapi 2.5.0 |
| Тесты | JUnit 5, AssertJ |
| Сборка | Maven |
| Качество кода | Checkstyle 3.3.1 |

## Архитектура

REST API с архитектурой, рассчитанной на масштабирование.

```
controller  →  service  →  dal (storage + row mapper)  →  H2
     ↑              ↑
   DTO         model / mapper
```

| Пакет | Назначение |
|---|---|
| `controller` | REST-контроллеры + `GlobalExceptionHandler` |
| `service` | Бизнес-логика (`FilmService`, `UserService`, `EventService`, `FeedService`, `ReviewService`, `DirectorService`, `GenreService`, `RatingMpaaService`) |
| `dal` | Репозитории на `JdbcOperations` |
| `dal.mappers` | `RowMapper`-реализации |
| `dto` | DTO запросов/ответов, включая краткие `FilmShortDto` / `UserShortDto` / `ReviewShortDto` для лент |
| `mapper` | Статические мапперы DTO ⇄ домен |
| `model` | Доменные модели + enums `EventTypes`, `OperationTypes` |
| `exception` | `NotFoundException`, `ValidationException` |

### Разделение ответственности

- **`FeedService`** — обогащение «сырых» событий из `EventService` вложенными краткими DTO.
  Обогащение делается batch-запросами.
- **Нормализация данных** (`email`, `login` — приведение к нижнему регистру) выполняется
  в сервисном слое (`UserService.normalizeUser`).
- **Рейтинг полезности отзыва** (`useful`) хранится денормализованно в таблице `reviews`
  и меняется атомарно (`UPDATE reviews SET useful = useful ± 1`) в одной транзакции
  с записью мнения в `review_opinions`. Лайки/дизлайки **отзывов** событий в ленте не создают —
  событие пишется только при `REVIEW/ADD|UPDATE|REMOVE`.
- **Порядок в выборках, где он не задаётся SQL,** восстанавливается явно в сервисе
  (например, `getTopPopularFilms` сортирует фильмы по `LinkedHashMap` из `filmLikesDbStorage`,
  потому что `findBySeveralIds` возвращает фильмы в произвольном порядке СУБД).

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

```bash
curl http://localhost:8080/films?from=0&size=10
curl http://localhost:8080/users?from=0&size=10
curl http://localhost:8080/genres
curl http://localhost:8080/mpa
curl http://localhost:8080/directors
curl http://localhost:8080/reviews?count=10
```

Ответ в формате JSON — сервис работает.
Интерактивная документация — `http://localhost:8080/swagger-ui.html`.

## Конфигурация

Настройки находятся в `src/main/resources/application.yaml` (при активном профиле `test` — в соответствующем `application-test.yaml`).

```yaml
spring:
  profiles:
    active: test
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
```

Данные сохраняются между перезапусками:
скрипты инициализации не создают дубликатов и не стирают существующие записи.

> 💡 Папку `db/` стоит добавить в `.gitignore`, чтобы файлы H2 не попадали в репозиторий.

## API

Базовый URL: `http://localhost:8080`. Интерактивная документация — Swagger UI:

| URL | Что это |
|---|---|
| `/swagger-ui.html` | Интерфейс Swagger UI |
| `/v3/api-docs` | OpenAPI-спецификация (JSON) |
| `/v3/api-docs.yaml` | То же в YAML |

### Пагинация

Все «списочные» эндпоинты (`GET /films`, `GET /users`) поддерживают query-параметры:

| Параметр | По умолчанию | Ограничение |
|---|---|---|
| `from` | `0` | `>= 0` |
| `size` | `10` | `1..100` |

Если `from` или `size` выходят за границы — возвращается `400 Bad Request`.

### Фильмы

| Метод | Путь                                               | Назначение | Успех |
|---|----------------------------------------------------|---|---|
| `POST` | `/films`                                           | Создать фильм | `201 Created` + `FilmDto` |
| `PUT` | `/films`                                           | Обновить фильм | `200 OK` + `FilmDto` |
| `GET` | `/films/{filmId}`                                  | Получить фильм по id | `200 OK` + `FilmDto` |
| `DELETE` | `/films/{filmId}`                                  | Удалить фильм | `204 No Content` |
| `GET` | `/films?from=0&size=10`                            | Страница фильмов | `200 OK` + `[FilmDto]` |
| `PUT` | `/films/{filmId}/like/{userId}`                    | Поставить лайк | `204 No Content` |
| `DELETE` | `/films/{filmId}/like/{userId}`                    | Снять лайк | `204 No Content` |
| `GET` | `/films/popular?count=10&genreId=&year=` | Топ-N по лайкам с опциональной фильтрацией | `200 OK` + `[FilmDto]` |
| `GET` | `/films/common?userId={id}&friendId={id}`          | Общие фильмы двух пользователей | `200 OK` + `[FilmDto]` |
| `GET` | `/films/director/{directorId}?sortBy=year\|likes` | Фильмы режиссёра, сортировка по году или лайкам | `200 OK` + `[FilmDto]` |
| `GET` | `/films/search?...` | Полноценный поиск по фильмам | `200 OK` + `[FilmDto]` |

**Пример создания фильма:**
```bash
curl -X POST http://localhost:8080/films \
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

Обратите внимание: `mpa`, элементы `genres` и `directors` передаются как объекты с полем `id` — это контракт API.

**Топ популярных фильмов**

```http
GET /films/popular?count=10&genreId=1&year=2010
```

| Параметр | Обязательный | Описание |
|---|---|---|
| `count` | нет (по умолчанию `10`) | Сколько фильмов вернуть |
| `genreId` | нет | Фильтр по жанру |
| `year` | нет | Фильтр по году выпуска |

Оба фильтра опциональны: без них возвращается общий топ по лайкам, с ними — топ только
среди фильмов указанного жанра и/или года. Сортировка — по убыванию количества лайков,
при равенстве — по возрастанию `id`. Фильмы без лайков в топ не попадают.

Если указан `genreId`, но жанра с таким `id` нет — `404 Not Found`.
Если совпадений нет — `200 OK` с пустым массивом.

**Фильмы режиссёра**

```http
GET /films/director/{directorId}?sortBy=year|likes
```

| Параметр | По умолчанию | Допустимые значения |
|---|---|---|
| `sortBy` | `year` | `year`, `likes` |

- `year` — сортировка по возрастанию даты релиза;
- `likes` — сортировка по убыванию количества лайков.

Пример:

```bash
curl "http://localhost:8080/films/director/1?sortBy=likes"
```

**Поиск фильмов**
```http
GET /films/search?query=нолан&by=director,title&yearFrom=2000&yearTo=2020&mpaIds=2,3&from=0&size=10
```

| Параметр | Тип | Описание |
|---|---|---|
| `query` | строка | Текст для поиска (подстрока, регистронезависимо) |
| `by` | строка | Где искать: `title`, `director`, `description` — любые через запятую. По умолчанию `title`, если задан `query` |
| `year` | int | Точный год выпуска |
| `yearFrom` | int | Год от (включительно) |
| `yearTo` | int | Год до (включительно) |
| `duration` | int | Точная длительность в минутах |
| `durationFrom` | int | Длительность от (включительно) |
| `durationTo` | int | Длительность до (включительно) |
| `mpaIds` | список int через запятую | Фильтр по рейтингам MPA |
| `from` | int, по умолчанию `0` | Пагинация |
| `size` | int, по умолчанию `10`, макс `100` | Пагинация |

Правила:

- Все фильтры соединяются через `AND`. Внутри `query` — `OR` между выбранными `by`.
- `year` и `yearFrom`/`yearTo` взаимоисключающие — иначе `400`.
- `duration` и `durationFrom`/`durationTo` взаимоисключающие — иначе `400`.
- Если `by` задан, а `query` пуст — `400` (вероятно, клиент забыл `query`).
- Спецсимволы LIKE (`%`, `_`) в `query` трактуются буквально.
- Без параметров возвращает всё, отсортированное по лайкам.

Результаты отсортированы по убыванию лайков, при равенстве — по возрастанию `id`.

### Пользователи

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `POST` | `/users` | Создать пользователя | `201 Created` + `UserDto` |
| `PUT` | `/users` | Обновить пользователя | `200 OK` + `UserDto` |
| `GET` | `/users/{userId}` | Получить пользователя | `200 OK` + `UserDto` |
| `DELETE` | `/users/{userId}` | Удалить пользователя | `204 No Content` |
| `GET` | `/users?from=0&size=10` | Страница пользователей | `200 OK` + `[UserDto]` |
| `PUT` | `/users/{userId}/friends/{friendId}` | Добавить друга | `204 No Content` |
| `DELETE` | `/users/{userId}/friends/{friendId}` | Удалить друга | `204 No Content` |
| `GET` | `/users/{userId}/friends` | Список друзей | `200 OK` + `[UserDto]` |
| `GET` | `/users/{userId}/friends/common/{friendId}` | Общие друзья | `200 OK` + `[UserDto]` |

Пример создания пользователя:

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
        "email": "user@example.com",
        "login": "userlogin",
        "name": "Иван",
        "birthday": "1990-01-15"
      }'
```

Если поле `name` не указано или пустое — в качестве имени будет использован `login`.

### Отзывы

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `POST` | `/reviews` | Создать отзыв | `201 Created` + `ReviewDto` |
| `PUT` | `/reviews` | Обновить отзыв | `200 OK` + `ReviewDto` |
| `DELETE` | `/reviews/{id}` | Удалить отзыв | `204 No Content` |
| `GET` | `/reviews/{id}` | Получить отзыв | `200 OK` + `ReviewDto` |
| `GET` | `/reviews?filmId=&count=10` | Отзывы по фильму (или все) | `200 OK` + `[ReviewDto]` |
| `PUT` | `/reviews/{id}/like/{userId}` | Поставить лайк | `204 No Content` |
| `PUT` | `/reviews/{id}/dislike/{userId}` | Поставить дизлайк | `204 No Content` |
| `DELETE` | `/reviews/{id}/like/{userId}` | Снять лайк | `204 No Content` |
| `DELETE` | `/reviews/{id}/dislike/{userId}` | Снять дизлайк | `204 No Content` |

Один пользователь может оставить **только один отзыв** на фильм (`UNIQUE (user_id, film_id)`).
Повторная попытка — `400 Bad Request`.

`useful` начинается с 0: лайк увеличивает его на 1, дизлайк уменьшает на 1.
Сортировка — по убыванию `useful`, при равенстве — по возрастанию `reviewId`.
Параметр `count` ограничен значением `100`.

Создание отзыва:

```bash
curl -X POST http://localhost:8080/reviews \
  -H "Content-Type: application/json" \
  -d '{
        "content": "This film is sooo baad.",
        "isPositive": false,
        "userId": 1,
        "filmId": 1
      }'
```

Ответ:

```json
{
  "reviewId": 1,
  "content": "This film is sooo baad.",
  "isPositive": false,
  "userId": 1,
  "filmId": 1,
  "useful": 0
}
```

`PUT /reviews` — частичное обновление: можно передать только `content`, только `isPositive`
или оба поля. Пустой запрос (без обоих полей) → `400 Bad Request`.

### Режиссёры

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `POST` | `/directors` | Создать режиссёра | `201 Created` + `DirectorDto` |
| `PUT` | `/directors` | Обновить режиссёра | `200 OK` + `DirectorDto` |
| `DELETE` | `/directors/{id}` | Удалить режиссёра | `204 No Content` |
| `GET` | `/directors/{id}` | Получить режиссёра | `200 OK` + `DirectorDto` |
| `GET` | `/directors` | Все режиссёры | `200 OK` + `[DirectorDto]` |

Имя режиссёра уникально (`UNIQUE (name)`): повторная попытка создания → `400 Bad Request`.

Создание режиссёра:

```bash
curl -X POST http://localhost:8080/directors \
  -H "Content-Type: application/json" \
  -d '{ "name": "Christopher Nolan" }'
```

Обновление:

```bash
curl -X PUT http://localhost:8080/directors \
  -H "Content-Type: application/json" \
  -d '{ "id": 1, "name": "Новое имя" }'
```

### Лента событий

Пользователь может получить два типа лент:

- **лента пользователя** — его собственные события;
- **лента друзей** — что делают его друзья:
    - пользователя добавили в друзья / удалили из друзей (событие с `entity_id = userId`);
    - друг поставил / снял лайк фильму;
    - друг создал / обновил / удалил отзыв.

Каждая лента доступна в двух вариантах: «сырая» (только id) и обогащённая (с краткими карточками).

В ленту попадают события:

- `LIKE/ADD` и `LIKE/REMOVE` — лайки **фильмов** (в ленте друзей — только от друзей);
- `FRIEND/ADD` и `FRIEND/REMOVE` — добавление в друзья / удаление из друзей;
- `REVIEW/ADD`, `REVIEW/UPDATE`, `REVIEW/REMOVE` — действия с отзывами (в ленте друзей — только от друзей).

Лайки и дизлайки **отзывов** событий не создают — меняется только поле `useful` у отзыва.

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `GET` | `/users/{userId}/feed/user` | События пользователя (сырые) | `200 OK` + `[Event]` |
| `GET` | `/users/{userId}/feed/friends` | События друзей (сырые) | `200 OK` + `[Event]` |
| `GET` | `/users/{userId}/feed/user/enriched` | События пользователя (обогащённые) | `200 OK` + `[EnrichedEventDto]` |
| `GET` | `/users/{userId}/feed/friends/enriched` | События друзей (обогащённые) | `200 OK` + `[EnrichedEventDto]` |

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

Для `REVIEW/REMOVE` поле `review` будет `null`: сам отзыв уже удалён, в событии остаётся только
факт удаления.

### Справочники

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `GET` | `/genres` | Все жанры | `200 OK` + `[GenreDto]` |
| `GET` | `/genres/{genreId}` | Жанр по id | `200 OK` + `GenreDto` |
| `GET` | `/mpa` | Все рейтинги MPA | `200 OK` + `[RatingMpaaDto]` |
| `GET` | `/mpa/{ratingId}` | Рейтинг по id | `200 OK` + `RatingMpaaDto` |

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

## Модель данных

Реляционная схема, H2.

| Таблица | Колонки |
|---|---|
| `users` | `id`, `email`, `login`, `name`, `birthday` |
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
| `NotFoundException` | `404 Not Found` |
| любое другое | `500 Internal Server Error` |

## Тестирование

```bash
mvn test
```

Тесты интеграционные: поднимают Spring-контекст и работают с настоящей H2 (in-memory),
mock-фреймворки не используются. Покрыты все слои:

- **Хранилища** (`dal`) — CRUD, выборки, связи, краевые случаи (пустые коллекции, `null`, дубли).
- **Сервисы** — бизнес-логика, валидация, транзакционность, события.
- **Контроллеры** — REST-эндпоинты через `MockMvc`, коды ответов, валидация query-параметров.

Тесты используют `@Nested` для группировки сценариев и очищают БД между запусками,
поэтому их можно запускать в любом порядке.

---
Проект разработан в рамках программы по Java-разработке на платформе Яндекс Практикум.
Реализация и доработки выполнены автором.