# Filmorate

Бэкенд-сервис социальной сети для киноманов. Помогает решить проблему выбора фильма:
пользователи ставят лайки, формируют круг друзей, оставляют отзывы и получают персональную
картину популярности — топ строится на реальных оценках, доступны общие фильмы, рейтинг
полезности отзывов, лента событий и рекомендации.

## Возможности

- **Каталог фильмов:** CRUD, пагинация, фильтр по жанру и году.
- **Поиск:** по названию, описанию, режиссёру, году, длительности, рейтингу MPA.
- **Лайки и популярность:** топ-N с сортировкой по количеству лайков.
- **Рекомендации:** коллаборативная фильтрация по пересечению лайков.
- **Друзья:** добавление/удаление, список, общие друзья.
- **Общие фильмы:** лайкнутые двумя пользователями.
- **Отзывы:** с оценкой полезности (`useful`); один пользователь — один отзыв на фильм;
  редактировать и удалять может только автор.
- **Лента событий:** действия пользователя и его друзей — в «сыром» и обогащённом виде.
- **Режиссёры:** CRUD, many-to-many с фильмами, сортировка фильмов по году или лайкам.
- **Справочники:** жанры и возрастные рейтинги MPA.
- **JWT-аутентификация:** `POST /auth/login`, роли `USER` и `ADMIN`.

## Стек

| Слой | Технология |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 3.2.4 |
| Безопасность | Spring Security + jjwt 0.12.5, BCrypt |
| БД | H2 (файловая), Spring JDBC |
| Валидация | Jakarta Bean Validation |
| Документация | springdoc-openapi 2.5.0 |
| Логирование HTTP | Zalando Logbook |
| Тесты | JUnit 5, AssertJ, Spring Security Test |
| Сборка | Maven, Checkstyle |

## Быстрый старт

```bash
git clone https://github.com/vd89zen/java-filmorate.git
cd java-filmorate
mvn clean package
java -jar target/filmorate-0.0.1-SNAPSHOT.jar
```

Приложение поднимется на `http://localhost:8080`.

Проверка публичных эндпоинтов:

```bash
curl http://localhost:8080/films?from=0&size=10
curl http://localhost:8080/genres
```

Регистрация и логин:

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","login":"userlogin","name":"Иван","birthday":"1990-01-15","password":"secret123"}'

curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secret123"}'
```

Ответ содержит `accessToken` — дальше передаётся как `Authorization: Bearer <token>`.

Интерактивная документация: **`http://localhost:8080/swagger-ui.html`**
(кнопка **Authorize** → вставить токен без префикса `Bearer`).

## Требования

| Компонент | Версия |
|---|---|
| JDK | 21 (совместим с 17) |
| Maven | 3.6+ |
| Свободный порт | 8080 |

JDK 16 и ниже не подойдут — Spring Boot 3.2.4 требует Java 17+.

## Администратор

При первом запуске (профиль ≠ `test`) `AdminInitializer` создаёт админа:

| Поле | Значение по умолчанию |
|---|---|
| Email | `admin@filmorate.local` |
| Password | `admin123` |

Переопределяется через env: `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_LOGIN`, `ADMIN_NAME`, `ADMIN_BIRTHDAY`.

⚠️ Дефолты — только для локальной разработки.

## Тестирование

```bash
mvn test
```

Интеграционные тесты на in-memory H2, mock-фреймворки не используются. Покрыты все слои: DAL,
сервисы, контроллеры (включая JWT и разграничение доступа).

## Документация

| Раздел | Файл |
|---|---|
| Архитектура и структура пакетов | [`docs/architecture.md`](docs/architecture.md) |
| Аутентификация и авторизация | [`docs/authentication.md`](docs/authentication.md) |
| API (эндпоинты, примеры) | [`docs/api.md`](docs/api.md) |
| Модель данных | [`docs/database.md`](docs/database.md) |
| Обработка ошибок | [`docs/errors.md`](docs/errors.md) |
| Конфигурация | [`docs/configuration.md`](docs/configuration.md) |

---
Проект разработан в рамках программы по Java-разработке на платформе Яндекс Практикум.
Реализация и дальнейшие доработки выполнены автором.