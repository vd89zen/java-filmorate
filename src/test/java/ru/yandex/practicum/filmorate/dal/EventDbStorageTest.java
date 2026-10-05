package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;
import ru.yandex.practicum.filmorate.model.enums.OperationTypes;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты EventDbStorage")
class EventDbStorageTest {
    private final EventDbStorage eventDbStorage;
    private final UserDbStorage userDbStorage;
    private final FilmDbStorage filmDbStorage;
    private Long user1Id;
    private Long user2Id;
    private Long filmId;

    @BeforeEach
    void setUp() {
        User user1 = User.builder()
                .id(null)
                .email("user1@mail.com")
                .login("login1")
                .name("User1")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        user1Id = userDbStorage.create(user1).getId();

        User user2 = User.builder()
                .id(null)
                .email("user2@mail.com")
                .login("login2")
                .name("User2")
                .birthday(LocalDate.of(1991, 1, 1))
                .build();
        user2Id = userDbStorage.create(user2).getId();

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

    @Test
    @DisplayName("Тесты: добавления событий и получения ленты событий")
    public void addEvent_And_GetFeed_Test() {
        // given
        int expectedSize = 2;
        // when
        Event eventFriend = eventDbStorage.addEvent(user1Id, EventTypes.FRIEND, OperationTypes.ADD, user2Id);
        Event eventLike = eventDbStorage.addEvent(user1Id, EventTypes.LIKE, OperationTypes.ADD, filmId);
        List<Event> events = eventDbStorage.getFeed(user1Id);
        // then
        assertNotNull(eventFriend);
        assertEquals(EventTypes.FRIEND.name(), eventFriend.getEventType());
        assertEquals(OperationTypes.ADD.name(), eventFriend.getOperation());
        assertEquals(user2Id, eventFriend.getEntityId());

        assertNotNull(eventLike);
        assertEquals(EventTypes.LIKE.name(), eventLike.getEventType());
        assertEquals(OperationTypes.ADD.name(), eventLike.getOperation());
        assertEquals(filmId, eventLike.getEntityId());

        assertNotNull(events);
        assertEquals(expectedSize, events.size());
    }
}