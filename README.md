# Filmorate

Бэкенд-сервис социальной сети для киноманов. Помогает решить проблему выбора фильма для просмотра:
пользователи оставляют лайки фильмам, формируют круг друзей и получают персональную картину популярности
— топ фильмов строится на основе реальных оценок, а также можно выбрать список общих фильмов (лайкнули оба)
для двух пользователей.

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

- **Каталог фильмов:** создание, редактирование, просмотр списка и отдельного фильма.
- **Лайки и популярность:** оценка фильмов пользователями и рейтинг топ-N по количеству лайков.
- **Друзья:** добавление и удаление, список друзей пользователя, поиск общих друзей.
- **Общие фильмы:** список фильмов, которые понравились обоим пользователям.
- **Справочники:** жанры и возрастные рейтинги MPA.

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
REST API с архитектурой рассчитанной на масштабирование.
```
controller  →  service  →  dal (storage + row mapper)  →  H2
     ↑              ↑
   DTO         model / mapper
```

| Пакет | Назначение                                                                        |
|---|-----------------------------------------------------------------------------------|
| `controller` | REST-контроллеры + `GlobalExceptionHandler`                                       |
| `service` | Бизнес-логика (`FilmService`, `UserService`, `GenreService`, `RatingMpaaService`) |
| `dal` | Репозитории на `JdbcTemplate`                                                     |
| `dal.mappers` | `RowMapper`-реализации                                                            |
| `dto` | DTO запросов/ответов                                                              |
| `mapper` | Статические мапперы DTO ⇄ домен                                                   |
| `model` | Доменные модели + интерфейсы `FilmStorage`, `UserStorage`                         |
| `exception` | `NotFoundException`, `ValidationException`                                        |

## Системные требования

| Компонент | Требование |
|---|-----------------------------|
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
либо измените версию в pom
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
curl http://localhost:8080/films
curl http://localhost:8080/users
curl http://localhost:8080/genres
curl http://localhost:8080/mpa
```

Ответ в формате JSON — сервис работает. Интерактивная документация — 
на `http://localhost:8080/swagger-ui.html`.

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

### Фильмы

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `POST` | `/films` | Создать фильм | `201 Created` + `FilmDto` |
| `PUT` | `/films` | Обновить фильм | `200 OK` + `FilmDto` |
| `GET` | `/films/{filmId}` | Получить фильм по id | `200 OK` + `FilmDto` |
| `GET` | `/films` | Список фильмов | `200 OK` + `[FilmDto]` |
| `PUT` | `/films/{filmId}/like/{userId}` | Поставить лайк | `204 No Content` |
| `DELETE` | `/films/{filmId}/like/{userId}` | Снять лайк | `204 No Content` |
| `GET` | `/films/popular?count=10` | Топ-N по лайкам | `200 OK` + `[FilmDto]` |
| `GET` | `/films/common?userId={id}&friendId={id}` | Общие фильмы двух пользователей | `200 OK` + `[FilmDto]` |

Пример создания фильма:
```bash
curl -X POST http://localhost:8080/films \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Inception",
        "description": "A thief who steals corporate secrets...",
        "releaseDate": "2010-07-16",
        "duration": 148,
        "mpa": { "id": 1 },
        "genres": [ { "id": 1 }, { "id": 2 } ]
      }'
```
Обратите внимание: `mpa` и элементы `genres` передаются как объекты с полем `id`
— это контракт API.

### Пользователи

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `POST` | `/users` | Создать пользователя | `201 Created` + `UserDto` |
| `PUT` | `/users` | Обновить пользователя | `200 OK` + `UserDto` |
| `GET` | `/users/{id}` | Получить по id | `200 OK` + `UserDto` |
| `GET` | `/users` | Список пользователей | `200 OK` + `[UserDto]` |
| `PUT` | `/users/{id}/friends/{friendId}` | Добавить друга | `204 No Content` |
| `DELETE` | `/users/{id}/friends/{friendId}` | Удалить друга | `204 No Content` |
| `GET` | `/users/{id}/friends` | Список друзей | `200 OK` + `[UserDto]` |
| `GET` | `/users/{id}/friends/common/{friendId}` | Общие друзья | `200 OK` + `[UserDto]` |

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

### Справочники

| Метод | Путь | Назначение | Успех |
|---|---|---|---|
| `GET` | `/genres` | Все жанры | `200 OK` + `[GenreDto]` |
| `GET` | `/genres/{genreId}` | Жанр по id | `200 OK` + `GenreDto` |
| `GET` | `/mpa` | Все рейтинги MPA | `200 OK` + `[RatingMpaaDto]` |
| `GET` | `/mpa/{ratingId}` | Рейтинг по id | `200 OK` + `RatingMpaaDto` |

Справочники предзаполнены при первом запуске: **6 жанров** и **5 рейтингов MPA**.
Данные сохраняются между перезапусками.

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

![Database Schema](https://raw.githubusercontent.com/vd89zen/java-filmorate/main/QuickDBD-filmorate.png)

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
| `ValidationException` | `400 Bad Request` |
| `NotFoundException` | `404 Not Found` |
| любое другое | `500 Internal Server Error` |

## Тестирование

```bash
mvn test
```

Тесты интеграционные: поднимают Spring-контекст и работают с H2. Mock-фреймворки не используются.

| Тест-класс | Что проверяет |
|---|---|
| `FilmorateApplicationTests` | Загрузку Spring-контекста |
| `FilmDbStorageTest` | CRUD и выборки фильмов |
| `UserDbStorageTest` | CRUD и выборки пользователей |
| `FriendshipDbStorageTest` | Дружбу: добавление, удаление, общие друзья, счётчики |
| `FilmLikesDbStorageTest` | Лайки: добавление, удаление, топ-N, счётчики |
| `FilmGenresDbStorageTest` | Связь фильмов и жанров |
| `GenreDbStorageTest` | Справочник жанров |
| `RatingMpaaDbStorageTest` | Справочник рейтингов MPA |

---
Проект разработан в рамках программы по Java-разработке на платформе Яндекс Практикум.
Реализация и доработки выполнены автором.