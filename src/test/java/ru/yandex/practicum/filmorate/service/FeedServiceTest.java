package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dal.EventDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.EnrichedEventDto;
import ru.yandex.practicum.filmorate.dto.NewReviewRequest;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.dto.ReviewDto;
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
@DisplayName("Тесты FeedService — обогащённая лента событий")
class FeedServiceTest {

    private final FeedService feedService;
    private final EventDbStorage eventDbStorage;
    private final UserDbStorage userDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final FriendshipDbStorage friendshipDbStorage;
    private final ReviewService reviewService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long user1Id;
    private Long user2Id;
    private Long user3Id;
    private Long filmId;

    @BeforeEach
    void setUp() {
        cleanUp();
        user1Id = createUser("user1@mail.com", "User1");
        user2Id = createUser("user2@mail.com", "User2");
        user3Id = createUser("user3@mail.com", "User3");
        filmId = createFilm("Test Film", LocalDate.of(2000, 1, 1));
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
        jdbcTemplate.execute("DELETE FROM review_opinions");
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private Long createUser(String email, String name) {
        User user = User.builder()
                .email(email)
                .login(email)
                .name(name)
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        return userDbStorage.create(user).getId();
    }

    private Long createFilm(String name, LocalDate releaseDate) {
        Film film = Film.builder()
                .name(name)
                .description("desc")
                .releaseDate(releaseDate)
                .duration(120)
                .mpa(new RatingMpaaId(1L))
                .build();
        return filmDbStorage.create(film).getId();
    }

    @Nested
    @DisplayName("getEnrichedFeedUser()")
    class GetEnrichedFeedUserTests {

        @Test
        @DisplayName("Пустая лента возвращает пустой список")
        void emptyFeed_ReturnsEmptyList_Test() {
            List<EnrichedEventDto> feed = feedService.getEnrichedFeedUser(user1Id);

            assertThat(feed).isEmpty();
        }

        @Test
        @DisplayName("LIKE-событие обогащается FilmShortDto, user == null")
        void likeEvent_EnrichedWithFilm_Test() {
            eventDbStorage.addEvent(user1Id, EventTypes.LIKE, OperationTypes.ADD, filmId);

            List<EnrichedEventDto> feed = feedService.getEnrichedFeedUser(user1Id);

            assertThat(feed).hasSize(1);
            EnrichedEventDto event = feed.get(0);
            assertThat(event.getEventType()).isEqualTo(EventTypes.LIKE.name());
            assertThat(event.getOperation()).isEqualTo(OperationTypes.ADD.name());
            assertThat(event.getFilm()).isNotNull();
            assertThat(event.getFilm().getId()).isEqualTo(filmId);
            assertThat(event.getFilm().getName()).isEqualTo("Test Film");
            assertThat(event.getFilm().getReleaseDate()).isEqualTo(LocalDate.of(2000, 1, 1));
            assertThat(event.getUser()).isNull();
        }

        @Test
        @DisplayName("FRIEND-событие обогащается UserShortDto, film == null")
        void friendEvent_EnrichedWithUser_Test() {
            eventDbStorage.addEvent(user1Id, EventTypes.FRIEND, OperationTypes.ADD, user2Id);

            List<EnrichedEventDto> feed = feedService.getEnrichedFeedUser(user1Id);

            assertThat(feed).hasSize(1);
            EnrichedEventDto event = feed.get(0);
            assertThat(event.getEventType()).isEqualTo(EventTypes.FRIEND.name());
            assertThat(event.getUser()).isNotNull();
            assertThat(event.getUser().getId()).isEqualTo(user2Id);
            assertThat(event.getUser().getName()).isEqualTo("User2");
            assertThat(event.getFilm()).isNull();
        }

        @Test
        @DisplayName("Смешанные события обогащаются каждое правильно")
        void mixedEvents_EachEnrichedCorrectly_Test() {
            eventDbStorage.addEvent(user1Id, EventTypes.LIKE, OperationTypes.ADD, filmId);
            eventDbStorage.addEvent(user1Id, EventTypes.FRIEND, OperationTypes.ADD, user2Id);

            List<EnrichedEventDto> feed = feedService.getEnrichedFeedUser(user1Id);

            assertThat(feed).hasSize(2);

            EnrichedEventDto likeEvent = feed.stream()
                    .filter(e -> EventTypes.LIKE.name().equals(e.getEventType()))
                    .findFirst().orElseThrow();
            assertThat(likeEvent.getFilm()).isNotNull();
            assertThat(likeEvent.getFilm().getId()).isEqualTo(filmId);
            assertThat(likeEvent.getUser()).isNull();

            EnrichedEventDto friendEvent = feed.stream()
                    .filter(e -> EventTypes.FRIEND.name().equals(e.getEventType()))
                    .findFirst().orElseThrow();
            assertThat(friendEvent.getUser()).isNotNull();
            assertThat(friendEvent.getUser().getId()).isEqualTo(user2Id);
            assertThat(friendEvent.getFilm()).isNull();
        }

        @Test
        @DisplayName("UserShortDto не раскрывает email / login / birthday")
        void userShortDto_DoesNotExposePrivateFields_Test() {
            eventDbStorage.addEvent(user1Id, EventTypes.FRIEND, OperationTypes.ADD, user2Id);

            List<EnrichedEventDto> feed = feedService.getEnrichedFeedUser(user1Id);

            var userShort = feed.get(0).getUser();
            assertThat(userShort).isNotNull();
            assertThat(userShort.getClass().getDeclaredFields())
                    .extracting("name")
                    .containsExactlyInAnyOrder("id", "name");
        }

        @Test
        @DisplayName("REVIEW-событие обогащается ReviewShortDto, film и user == null")
        void reviewEvent_EnrichedWithReview_Test() {
            NewReviewRequest request = new NewReviewRequest();
            request.setUserId(user1Id);
            request.setFilmId(filmId);
            request.setContent("bad film");
            request.setIsPositive(false);

            ReviewDto created = reviewService.create(request);

            List<EnrichedEventDto> feed = feedService.getEnrichedFeedUser(user1Id);

            EnrichedEventDto event = feed.stream()
                    .filter(e -> EventTypes.REVIEW.name().equals(e.getEventType()))
                    .findFirst().orElseThrow();

            assertThat(event.getOperation()).isEqualTo(OperationTypes.ADD.name());
            assertThat(event.getReview()).isNotNull();
            assertThat(event.getReview().getReviewId()).isEqualTo(created.getReviewId());
            assertThat(event.getReview().getContent()).isEqualTo("bad film");
            assertThat(event.getReview().getIsPositive()).isFalse();
            assertThat(event.getFilm()).isNull();
            assertThat(event.getUser()).isNull();
            assertThat(event.getReview().getFilm()).isNotNull();
            assertThat(event.getReview().getFilm().getId()).isEqualTo(filmId);
        }
    }

    @Nested
    @DisplayName("getEnrichedFeedFriends()")
    class GetEnrichedFeedFriendsTests {

        @Test
        @DisplayName("Лайк от друга обогащается фильмом")
        void likeFromFriend_Enriched_Test() {
            friendshipDbStorage.addFriend(user1Id, user2Id);
            eventDbStorage.addEvent(user2Id, EventTypes.LIKE, OperationTypes.ADD, filmId);

            List<EnrichedEventDto> feed = feedService.getEnrichedFeedFriends(user1Id);

            assertThat(feed).hasSize(1);
            EnrichedEventDto event = feed.get(0);
            assertThat(event.getEventType()).isEqualTo(EventTypes.LIKE.name());
            assertThat(event.getUserId()).isEqualTo(user2Id);
            assertThat(event.getFilm()).isNotNull();
            assertThat(event.getFilm().getId()).isEqualTo(filmId);
        }

        @Test
        @DisplayName("Лайк от не-друга не попадает в ленту")
        void likeFromNonFriend_NotIncluded_Test() {
            eventDbStorage.addEvent(user3Id, EventTypes.LIKE, OperationTypes.ADD, filmId);

            List<EnrichedEventDto> feed = feedService.getEnrichedFeedFriends(user1Id);

            assertThat(feed).isEmpty();
        }

        @Test
        @DisplayName("FRIEND/ADD с entity_id = user1 обогащается UserShortDto")
        void friendAddedUser_Enriched_Test() {
            eventDbStorage.addEvent(user2Id, EventTypes.FRIEND, OperationTypes.ADD, user1Id);

            List<EnrichedEventDto> feed = feedService.getEnrichedFeedFriends(user1Id);

            assertThat(feed).hasSize(1);
            EnrichedEventDto event = feed.get(0);
            assertThat(event.getUser()).isNotNull();
            assertThat(event.getUser().getId()).isEqualTo(user1Id);
            assertThat(event.getFilm()).isNull();
        }

        @Test
        @DisplayName("Смешанные события: попадают только события друзей")
        void mixedEvents_OnlyFriendEventsIncluded_Test() {
            friendshipDbStorage.addFriend(user1Id, user2Id);

            eventDbStorage.addEvent(user2Id, EventTypes.LIKE, OperationTypes.ADD, filmId);
            eventDbStorage.addEvent(user3Id, EventTypes.LIKE, OperationTypes.ADD, filmId);
            eventDbStorage.addEvent(user3Id, EventTypes.FRIEND, OperationTypes.ADD, user2Id);

            List<EnrichedEventDto> feed = feedService.getEnrichedFeedFriends(user1Id);

            assertThat(feed).hasSize(1);
            assertThat(feed.get(0).getUserId()).isEqualTo(user2Id);
        }
    }
}