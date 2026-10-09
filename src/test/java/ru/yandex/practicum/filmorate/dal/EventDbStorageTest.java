package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;
import ru.yandex.practicum.filmorate.model.enums.OperationTypes;
import ru.yandex.practicum.filmorate.model.enums.Role;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("EventDbStorage Тесты")
class EventDbStorageTest {

    private final EventDbStorage eventDbStorage;
    private final UserDbStorage userDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final FriendshipDbStorage friendshipDbStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long user1Id;
    private Long user2Id;
    private Long user3Id;
    private Long filmId;

    @BeforeEach
    void setUp() {
        cleanUp();
        User user1 = User.builder()
                .id(null)
                .email("user1@mail.com")
                .login("login1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .role(Role.USER)
                .build();
        user1Id = userDbStorage.create(user1).getId();

        User user2 = User.builder()
                .id(null)
                .email("user2@mail.com")
                .login("login2")
                .name("User2")
                .birthday(LocalDate.of(1991, 1, 1))
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .role(Role.USER)
                .build();
        user2Id = userDbStorage.create(user2).getId();

        User user3 = User.builder()
                .id(null)
                .email("user3@mail.com")
                .login("login3")
                .name("User3")
                .birthday(LocalDate.of(1991, 1, 1))
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .role(Role.USER)
                .build();
        user3Id = userDbStorage.create(user3).getId();

        Film film = Film.builder()
                .id(null)
                .name("Test Film")
                .description("Test Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpa(new RatingMpaaId(3L))
                .build();
        filmId = filmDbStorage.create(film).getId();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        // порядок важен из-за foreign key
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    @DisplayName("addEvent + getFeedUser: лента событий самого пользователя")
    void addEvent_And_GetFeedUser_Test() {
        // given
        Event friendEvent = eventDbStorage.addEvent(
                user1Id, EventTypes.FRIEND, OperationTypes.ADD, user2Id);
        Event likeEvent = eventDbStorage.addEvent(
                user1Id, EventTypes.LIKE, OperationTypes.ADD, filmId);

        // when
        List<Event> eventsUser = eventDbStorage.getFeedUser(user1Id);

        // then
        assertNotNull(friendEvent);
        assertEquals(EventTypes.FRIEND.name(), friendEvent.getEventType());
        assertEquals(OperationTypes.ADD.name(), friendEvent.getOperation());
        assertEquals(user2Id, friendEvent.getEntityId());
        assertEquals(user1Id, friendEvent.getUserId());

        assertNotNull(likeEvent);
        assertEquals(EventTypes.LIKE.name(), likeEvent.getEventType());
        assertEquals(OperationTypes.ADD.name(), likeEvent.getOperation());
        assertEquals(filmId, likeEvent.getEntityId());

        assertNotNull(eventsUser);
        assertEquals(2, eventsUser.size());
        // сортировка DESC по id — последнее добавленное первым
        assertEquals(EventTypes.LIKE.name(), eventsUser.get(0).getEventType());
        assertEquals(EventTypes.FRIEND.name(), eventsUser.get(1).getEventType());
    }

    @Test
    @DisplayName("getFeedFriends: лента событий друзей пользователя")
    void getFeedFriends_Test() {
        // given
        // user2 — друг user1 (запись в таблице friendship)
        friendshipDbStorage.addFriend(user1Id, user2Id);

        // 1. user1 добавил user2 в друзья.
        //    В ленте user1 (getFeedUser) — есть.
        //    В ленте user2 как "пользователя добавили в друзья" — тоже учитывается через entity_id.
        eventDbStorage.addEvent(user1Id, EventTypes.FRIEND, OperationTypes.ADD, user2Id);

        // 2. user2 (друг user1) лайкнул фильм.
        //    В ленту друзей user1 должно попасть как LIKE/ADD от друга.
        eventDbStorage.addEvent(user2Id, EventTypes.LIKE, OperationTypes.ADD, filmId);

        // 3. user2 добавил user1 в друзья.
        //    В ленту друзей user1 должно попасть как FRIEND/ADD с entity_id = user1.
        eventDbStorage.addEvent(user2Id, EventTypes.FRIEND, OperationTypes.ADD, user1Id);

        // 4. События не-друга user3 — НЕ должны попасть в ленту друзей user1.
        eventDbStorage.addEvent(user3Id, EventTypes.LIKE, OperationTypes.ADD, filmId);
        eventDbStorage.addEvent(user3Id, EventTypes.FRIEND, OperationTypes.ADD, user2Id);

        // when
        List<Event> feedFriends = eventDbStorage.getFeedFriends(user1Id);

        // then
        assertNotNull(feedFriends);
        assertEquals(2, feedFriends.size());

        boolean hasFriendAddedUser1 = feedFriends.stream()
                .anyMatch(e -> EventTypes.FRIEND.name().equals(e.getEventType())
                        && OperationTypes.ADD.name().equals(e.getOperation())
                        && user1Id.equals(e.getEntityId()));

        boolean hasFriendLikedFilm = feedFriends.stream()
                .anyMatch(e -> EventTypes.LIKE.name().equals(e.getEventType())
                        && OperationTypes.ADD.name().equals(e.getOperation())
                        && user2Id.equals(e.getUserId())
                        && filmId.equals(e.getEntityId()));

        assertTrue(hasFriendAddedUser1,
                "В ленте друзей должно быть событие FRIEND/ADD с entityId = user1");
        assertTrue(hasFriendLikedFilm,
                "В ленте друзей должно быть событие LIKE/ADD от друга user2");

        // Событий от user3 быть не должно
        boolean hasUser3Event = feedFriends.stream()
                .anyMatch(e -> user3Id.equals(e.getUserId()));
        assertFalse(hasUser3Event, "События не-друзей не должны попадать в ленту друзей");
    }

    @Test
    @DisplayName("getFeedFriends: лайки не-друзей не попадают в ленту, даже если они добавили пользователя в друзья")
    void getFeedFriends_ExcludesLikesFromNonFriends_Test() {
        // given
        // user2 добавил user1 в друзья (event FRIEND/ADD entity_id=user1).
        // Но friendship-запись user1 -> user2 отсутствует: user1 не добавлял user2.
        // Значит для ленты user1 этот user2 — не друг.
        eventDbStorage.addEvent(user2Id, EventTypes.FRIEND, OperationTypes.ADD, user1Id);

        // user2 (не друг user1 с точки зрения user1) лайкнул фильм
        eventDbStorage.addEvent(user2Id, EventTypes.LIKE, OperationTypes.ADD, filmId);

        // when
        List<Event> feedFriends = eventDbStorage.getFeedFriends(user1Id);

        // then
        assertNotNull(feedFriends);

        // Событие "user2 добавил user1 в друзья" проходит по entity_id = user1
        // и должно быть в ленте.
        boolean hasFriendAddedUser1 = feedFriends.stream()
                .anyMatch(e -> EventTypes.FRIEND.name().equals(e.getEventType())
                        && OperationTypes.ADD.name().equals(e.getOperation())
                        && user1Id.equals(e.getEntityId()));
        assertTrue(hasFriendAddedUser1,
                "Событие FRIEND/ADD с entityId = user1 должно попасть в ленту");

        // Лайк от user2 не должен попасть: user2 не входит в user_friends для user1.
        boolean hasLikeFromUser2 = feedFriends.stream()
                .anyMatch(e -> EventTypes.LIKE.name().equals(e.getEventType())
                        && user2Id.equals(e.getUserId()));
        assertFalse(hasLikeFromUser2,
                "Лайки не-друзей не должны попадать в ленту друзей user1");

        // В ленте ровно одно событие — FRIEND/ADD.
        assertEquals(1, feedFriends.size());
    }
}