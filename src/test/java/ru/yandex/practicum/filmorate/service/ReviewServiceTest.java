package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.ReviewDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;
import ru.yandex.practicum.filmorate.model.enums.OperationTypes;
import ru.yandex.practicum.filmorate.model.enums.Role;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("ReviewService Тесты")
class ReviewServiceTest {

    private final ReviewService reviewService;
    private final ReviewDbStorage reviewStorage;
    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;
    private final EventService eventService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long user1Id;
    private Long user2Id;
    private Long filmId;

    @BeforeEach
    void setUp() {
        cleanUp();
        user1Id = createUser("user1@mail.com", "User1");
        user2Id = createUser("user2@mail.com", "User2");
        filmId = createFilm("Film1");
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM review_opinions");
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private Long createUser(String email, String name) {
        return userStorage.create(User.builder()
                .email(email)
                .login(email)
                .name(name)
                .birthday(LocalDate.of(1990, 1, 1))
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .role(Role.USER)
                .build()).getId();
    }

    private Long createFilm(String name) {
        return filmStorage.create(Film.builder()
                .name(name).description("desc")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120).mpa(new RatingMpaaId(1L)).build()).getId();
    }

    private NewReviewRequest newRequest(Long filmId, String content, boolean isPositive) {
        NewReviewRequest request = new NewReviewRequest();
        request.setFilmId(filmId);
        request.setContent(content);
        request.setIsPositive(isPositive);
        return request;
    }

    @Nested
    @DisplayName("Тесты create()")
    class CreateTests {

        @Test
        @DisplayName("Успешное создание: useful = 0, событие REVIEW/ADD записано")
        void create_Should_ReturnReviewWithZeroUseful_AndAddEvent_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "bad", false), user1Id);

            assertThat(created.getReviewId()).isNotNull();
            assertThat(created.getUseful()).isZero();
            assertThat(created.getContent()).isEqualTo("bad");
            assertThat(created.getIsPositive()).isFalse();
            assertThat(created.getUserId()).isEqualTo(user1Id);

            assertThat(eventService.getFeedUser(user1Id))
                    .filteredOn(e -> EventTypes.REVIEW.name().equals(e.getEventType()))
                    .filteredOn(e -> OperationTypes.ADD.name().equals(e.getOperation()))
                    .filteredOn(e -> e.getEntityId().equals(created.getReviewId()))
                    .hasSize(1);
        }

        @Test
        @DisplayName("Создание второго отзыва тем же пользователем на тот же фильм → ValidationException")
        void create_Should_ThrowValidationException_OnDuplicate_Test() {
            reviewService.create(newRequest(filmId, "first", true), user1Id);

            assertThatThrownBy(() -> reviewService.create(newRequest(filmId, "second", false), user1Id))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("уже оставил отзыв");
        }

        @Test
        @DisplayName("Несуществующий пользователь → NotFoundException")
        void create_Should_ThrowNotFoundException_ForNonExistingUser_Test() {
            assertThatThrownBy(() -> reviewService.create(newRequest(filmId, "x", true), 999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Несуществующий фильм → NotFoundException")
        void create_Should_ThrowNotFoundException_ForNonExistingFilm_Test() {
            assertThatThrownBy(() -> reviewService.create(newRequest(999L, "x", true), user1Id))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Тесты update()")
    class UpdateTests {

        @Test
        @DisplayName("Обновление только content")
        void update_Should_UpdateOnlyContent_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "old", true), user1Id);

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(created.getReviewId());
            request.setContent("new");

            ReviewDto updated = reviewService.update(request);

            assertThat(updated.getContent()).isEqualTo("new");
            assertThat(updated.getIsPositive()).isTrue();
        }

        @Test
        @DisplayName("Обновление только isPositive")
        void update_Should_UpdateOnlyIsPositive_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "old", true), user1Id);

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(created.getReviewId());
            request.setIsPositive(false);

            ReviewDto updated = reviewService.update(request);

            assertThat(updated.getContent()).isEqualTo("old");
            assertThat(updated.getIsPositive()).isFalse();
        }

        @Test
        @DisplayName("Обновление обоих полей")
        void update_Should_UpdateBothFields_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "old", true), user1Id);

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(created.getReviewId());
            request.setContent("new");
            request.setIsPositive(false);

            ReviewDto updated = reviewService.update(request);

            assertThat(updated.getContent()).isEqualTo("new");
            assertThat(updated.getIsPositive()).isFalse();
        }

        @Test
        @DisplayName("Обновление несуществующего отзыва → NotFoundException")
        void update_Should_ThrowNotFoundException_ForNonExistingReview_Test() {
            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(999L);
            request.setContent("new");

            assertThatThrownBy(() -> reviewService.update(request))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Пустой апдейт (без полей) → ValidationException")
        void update_Should_ThrowValidationException_ForEmptyUpdate_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "old", true), user1Id);

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(created.getReviewId());

            assertThatThrownBy(() -> reviewService.update(request))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("Тесты delete()")
    class DeleteTests {

        @Test
        @DisplayName("Удаление: запись исчезает, событие REVIEW/REMOVE записано")
        void delete_Should_RemoveReview_AndAddEvent_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "x", true), user1Id);

            reviewService.delete(created.getReviewId());

            assertThat(reviewStorage.findById(created.getReviewId())).isEmpty();

            assertThat(eventService.getFeedUser(user1Id))
                    .filteredOn(e -> EventTypes.REVIEW.name().equals(e.getEventType()))
                    .filteredOn(e -> OperationTypes.REMOVE.name().equals(e.getOperation()))
                    .filteredOn(e -> e.getEntityId().equals(created.getReviewId()))
                    .hasSize(1);
        }

        @Test
        @DisplayName("Удаление несуществующего → NotFoundException")
        void delete_Should_ThrowNotFoundException_ForNonExistingReview_Test() {
            assertThatThrownBy(() -> reviewService.delete(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Тесты findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Возвращает сохранённый отзыв")
        void findById_Should_ReturnReview_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "x", true), user1Id);

            ReviewDto found = reviewService.findById(created.getReviewId());

            assertThat(found.getReviewId()).isEqualTo(created.getReviewId());
            assertThat(found.getContent()).isEqualTo("x");
        }

        @Test
        @DisplayName("Несуществующий → NotFoundException")
        void findById_Should_ThrowNotFoundException_ForNonExistingReview_Test() {
            assertThatThrownBy(() -> reviewService.findById(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Тесты findAll()")
    class FindAllTests {

        @Test
        @DisplayName("filmId = null: возвращает все отзывы")
        void findAll_Should_ReturnAllReviews_WhenFilmIdIsNull_Test() {
            reviewService.create(newRequest(filmId, "r1", true), user1Id);
            reviewService.create(newRequest(filmId, "r2", false), user2Id);

            List<ReviewDto> all = reviewService.findAll(null, 10);
            assertThat(all).hasSize(2);
        }

        @Test
        @DisplayName("filmId указан: возвращает только его отзывы")
        void findAll_Should_ReturnOnlyReviewsOfFilm_Test() {
            Long otherFilm = createFilm("Film2");
            reviewService.create(newRequest(filmId, "r1", true), user1Id);
            reviewService.create(newRequest(otherFilm, "r2", false), user2Id);

            List<ReviewDto> filtered = reviewService.findAll(filmId, 10);
            assertThat(filtered).hasSize(1);
            assertThat(filtered.get(0).getFilmId()).isEqualTo(filmId);
        }

        @Test
        @DisplayName("count ограничивает результат")
        void findAll_Should_LimitResultByCount_Test() {
            reviewService.create(newRequest(filmId, "r1", true), user1Id);
            reviewService.create(newRequest(filmId, "r2", false), user2Id);

            assertThat(reviewService.findAll(filmId, 1)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Тесты лайков и дизлайков")
    class OpinionsTests {

        @Test
        @DisplayName("Лайк увеличивает useful")
        void addLike_Should_IncrementUseful_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "x", true), user1Id);
            reviewService.addLike(created.getReviewId(), user2Id);

            assertThat(reviewService.findById(created.getReviewId()).getUseful()).isEqualTo(1);
        }

        @Test
        @DisplayName("Дизлайк уменьшает useful")
        void addDislike_Should_DecrementUseful_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "x", true), user1Id);
            reviewService.addDislike(created.getReviewId(), user2Id);

            assertThat(reviewService.findById(created.getReviewId()).getUseful()).isEqualTo(-1);
        }

        @Test
        @DisplayName("Снятие лайка возвращает useful к 0")
        void removeLike_Should_RestoreUseful_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "x", true), user1Id);
            reviewService.addLike(created.getReviewId(), user2Id);
            reviewService.removeLike(created.getReviewId(), user2Id);

            assertThat(reviewService.findById(created.getReviewId()).getUseful()).isZero();
        }

        @Test
        @DisplayName("Снятие дизлайка возвращает useful к 0")
        void removeDislike_Should_RestoreUseful_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "x", true), user1Id);
            reviewService.addDislike(created.getReviewId(), user2Id);
            reviewService.removeDislike(created.getReviewId(), user2Id);

            assertThat(reviewService.findById(created.getReviewId()).getUseful()).isZero();
        }

        @Test
        @DisplayName("Лайк несуществующего отзыва → NotFoundException")
        void addLike_Should_ThrowNotFoundException_ForNonExistingReview_Test() {
            assertThatThrownBy(() -> reviewService.addLike(999L, user1Id))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Лайк от несуществующего пользователя → NotFoundException")
        void addLike_Should_ThrowNotFoundException_ForNonExistingUser_Test() {
            ReviewDto created = reviewService.create(newRequest(filmId, "x", true), user1Id);
            assertThatThrownBy(() -> reviewService.addLike(created.getReviewId(), 999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Тесты findShortByIds()")
    class FindShortByIdsTests {

        @Test
        @DisplayName("Пустой set → пустая карта")
        void findShortByIds_Should_ReturnEmptyMap_ForEmptySet_Test() {
            assertThat(reviewService.findShortByIds(Set.of())).isEmpty();
        }

        @Test
        @DisplayName("Возвращает карту с короткими DTO")
        void findShortByIds_Should_ReturnMapWithRequestedReviews_Test() {
            ReviewDto r1 = reviewService.create(newRequest(filmId, "r1", true), user1Id);
            ReviewDto r2 = reviewService.create(newRequest(filmId, "r2", false), user2Id);

            Map<Long, ?> map = reviewService.findShortByIds(Set.of(r1.getReviewId(), r2.getReviewId()));

            assertThat(map).hasSize(2).containsKeys(r1.getReviewId(), r2.getReviewId());
        }

        @Test
        @DisplayName("findShortByIds: короткое DTO содержит информацию о фильме")
        void findShortByIds_Should_IncludeFilm_Test() {
            ReviewDto r1 = reviewService.create(newRequest(filmId, "r1", true), user1Id);

            Map<Long, ReviewShortDto> map = reviewService.findShortByIds(Set.of(r1.getReviewId()));

            assertThat(map).hasSize(1);
            ReviewShortDto dto = map.get(r1.getReviewId());
            assertThat(dto.getFilm()).isNotNull();
            assertThat(dto.getFilm().getId()).isEqualTo(filmId);
            assertThat(dto.getFilm().getName()).isEqualTo("Film1");
        }
    }
}