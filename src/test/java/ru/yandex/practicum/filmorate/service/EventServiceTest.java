package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dal.EventDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;
import ru.yandex.practicum.filmorate.model.enums.OperationTypes;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("EventService Тесты")
class EventServiceTest {

    private final EventService eventService;
    private final EventDbStorage eventDbStorage;
    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long filmId;

    @BeforeEach
    void setUp() {
        cleanUp();
        userId = userStorage.create(User.builder()
                .email("user@mail.com").login("user").name("User")
                .birthday(LocalDate.of(1990, 1, 1)).build()).getId();

        filmId = filmStorage.create(Film.builder()
                .name("Film").description("desc")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120).mpa(new RatingMpaaId(1L)).build()).getId();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM film_directors");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Nested
    @DisplayName("Тесты addEvent()")
    class AddEventTests {

        @Test
        @DisplayName("Возвращает событие с id и корректными полями")
        void addEvent_Should_ReturnEventWithId_Test() {
            Event event = eventService.addEvent(
                    userId, EventTypes.LIKE, OperationTypes.ADD, filmId);

            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getEventType()).isEqualTo(EventTypes.LIKE.name());
            assertThat(event.getOperation()).isEqualTo(OperationTypes.ADD.name());
            assertThat(event.getEntityId()).isEqualTo(filmId);
            assertThat(event.getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Тесты getFeedUser()")
    class GetFeedUserTests {

        @Test
        @DisplayName("Пустая лента → пустой список")
        void getFeedUser_Should_ReturnEmpty_WhenNoEvents_Test() {
            assertThat(eventService.getFeedUser(userId)).isEmpty();
        }

        @Test
        @DisplayName("Возвращает только события пользователя")
        void getFeedUser_Should_ReturnOnlyUserEvents_Test() {
            eventService.addEvent(userId, EventTypes.LIKE, OperationTypes.ADD, filmId);

            List<Event> feed = eventService.getFeedUser(userId);
            assertThat(feed).hasSize(1);
            assertThat(feed.get(0).getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Сортировка по id DESC")
        void getFeedUser_Should_SortByIdDesc_Test() {
            eventService.addEvent(userId, EventTypes.FRIEND, OperationTypes.ADD, filmId);
            eventService.addEvent(userId, EventTypes.LIKE, OperationTypes.ADD, filmId);

            List<Event> feed = eventService.getFeedUser(userId);
            assertThat(feed).hasSize(2);
            assertThat(feed.get(0).getEventType()).isEqualTo(EventTypes.LIKE.name());
            assertThat(feed.get(1).getEventType()).isEqualTo(EventTypes.FRIEND.name());
        }
    }

    @Nested
    @DisplayName("Тесты getFeedFriends()")
    class GetFeedFriendsTests {

        @Test
        @DisplayName("Пустая лента → пустой список")
        void getFeedFriends_Should_ReturnEmpty_WhenNoFriends_Test() {
            assertThat(eventService.getFeedFriends(userId)).isEmpty();
        }

        @Test
        @DisplayName("Возвращает события друзей")
        void getFeedFriends_Should_ReturnFriendsEvents_Test() {
            Long friendId = userStorage.create(User.builder()
                    .email("friend@mail.com").login("friend").name("Friend")
                    .birthday(LocalDate.of(1990, 1, 1)).build()).getId();

            jdbcTemplate.update("INSERT INTO friendship (user_id, friend_id) VALUES (?, ?)",
                    userId, friendId);
            eventService.addEvent(friendId, EventTypes.LIKE, OperationTypes.ADD, filmId);

            List<Event> feed = eventService.getFeedFriends(userId);
            assertThat(feed).hasSize(1);
            assertThat(feed.get(0).getUserId()).isEqualTo(friendId);
        }
    }
}