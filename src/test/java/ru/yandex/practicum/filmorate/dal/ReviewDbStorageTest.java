package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("ReviewDbStorage Тесты")
class ReviewDbStorageTest {

    private final ReviewDbStorage reviewStorage;
    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long user1Id;
    private Long user2Id;
    private Long user3Id;
    private Long film1Id;
    private Long film2Id;

    @BeforeEach
    void setUp() {
        cleanUp();
        user1Id = createUser("user1@mail.com", "User1");
        user2Id = createUser("user2@mail.com", "User2");
        user3Id = createUser("user3@mail.com", "User3");
        film1Id = createFilm("Film1");
        film2Id = createFilm("Film2");
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
                .email(email).login(email).name(name)
                .birthday(LocalDate.of(1990, 1, 1)).build()).getId();
    }

    private Long createFilm(String name) {
        return filmStorage.create(Film.builder()
                .name(name).description("desc")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120).mpa(new RatingMpaaId(1L)).build()).getId();
    }

    private Review newReview(Long userId, Long filmId, String content, boolean isPositive) {
        return Review.builder()
                .content(content).isPositive(isPositive)
                .userId(userId).filmId(filmId).useful(0).build();
    }

    @Nested
    @DisplayName("Тесты create() и findById()")
    class CreateAndFindTests {

        @Test
        @DisplayName("Создание отзыва: useful инициализируется нулём")
        void create_Should_InitializeUsefulToZero_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "bad film", false));

            assertThat(saved.getReviewId()).isNotNull();
            assertThat(saved.getUseful()).isZero();

            Review fromDb = reviewStorage.findById(saved.getReviewId()).orElseThrow();
            assertThat(fromDb.getUseful()).isZero();
            assertThat(fromDb.getContent()).isEqualTo("bad film");
            assertThat(fromDb.getIsPositive()).isFalse();
            assertThat(fromDb.getUserId()).isEqualTo(user1Id);
            assertThat(fromDb.getFilmId()).isEqualTo(film1Id);
        }

        @Test
        @DisplayName("findById: пустой Optional для несуществующего отзыва")
        void findById_Should_ReturnEmptyOptional_For_NonExistingReview_Test() {
            assertThat(reviewStorage.findById(999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты update()")
    class UpdateTests {

        @Test
        @DisplayName("Обновление контента и оценки")
        void update_Should_ChangeContentAndIsPositive_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "old", false));
            saved.setContent("new");
            saved.setIsPositive(true);

            reviewStorage.update(saved);

            Review fromDb = reviewStorage.findById(saved.getReviewId()).orElseThrow();
            assertThat(fromDb.getContent()).isEqualTo("new");
            assertThat(fromDb.getIsPositive()).isTrue();
            assertThat(fromDb.getUseful()).isZero();
        }
    }

    @Nested
    @DisplayName("Тесты delete()")
    class DeleteTests {

        @Test
        @DisplayName("Удаление существующего отзыва → true")
        void delete_Should_ReturnTrue_AndRemoveReview_ForExisting_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            assertThat(reviewStorage.delete(saved.getReviewId())).isTrue();
            assertThat(reviewStorage.findById(saved.getReviewId())).isEmpty();
        }

        @Test
        @DisplayName("Удаление несуществующего → false")
        void delete_Should_ReturnFalse_ForNonExistingReview_Test() {
            assertThat(reviewStorage.delete(999L)).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты isReviewExists()")
    class IsReviewExistsTests {

        @Test
        @DisplayName("true для существующего отзыва")
        void isReviewExists_Should_ReturnTrue_ForExistingReview_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            assertThat(reviewStorage.isReviewExists(saved.getReviewId())).isTrue();
        }

        @Test
        @DisplayName("false для несуществующего")
        void isReviewExists_Should_ReturnFalse_ForNonExistingReview_Test() {
            assertThat(reviewStorage.isReviewExists(999L)).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты isReviewExistsByUserAndFilm()")
    class IsReviewExistsByUserAndFilmTests {

        @Test
        @DisplayName("true, если пользователь уже оставил отзыв на этот фильм")
        void isReviewExistsByUserAndFilm_Should_ReturnTrue_WhenExists_Test() {
            reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            assertThat(reviewStorage.isReviewExistsByUserAndFilm(user1Id, film1Id)).isTrue();
        }

        @Test
        @DisplayName("false, если пользователь оставил отзыв на другой фильм")
        void isReviewExistsByUserAndFilm_Should_ReturnFalse_ForOtherFilm_Test() {
            reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            assertThat(reviewStorage.isReviewExistsByUserAndFilm(user1Id, film2Id)).isFalse();
        }

        @Test
        @DisplayName("false, если это другой пользователь")
        void isReviewExistsByUserAndFilm_Should_ReturnFalse_ForOtherUser_Test() {
            reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            assertThat(reviewStorage.isReviewExistsByUserAndFilm(user2Id, film1Id)).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты findByFilmId() и findAll()")
    class FindByFilmTests {

        @Test
        @DisplayName("findByFilmId возвращает только отзывы указанного фильма")
        void findByFilmId_Should_ReturnOnlyReviewsOfFilm_Test() {
            reviewStorage.create(newReview(user1Id, film1Id, "r1", true));
            reviewStorage.create(newReview(user2Id, film1Id, "r2", false));
            reviewStorage.create(newReview(user3Id, film2Id, "r3", true));

            List<Review> forFilm1 = reviewStorage.findByFilmId(film1Id, 10);
            assertThat(forFilm1).hasSize(2)
                    .allMatch(r -> r.getFilmId().equals(film1Id));
        }

        @Test
        @DisplayName("findAll возвращает все отзывы")
        void findAll_Should_ReturnAllReviews_Test() {
            reviewStorage.create(newReview(user1Id, film1Id, "r1", true));
            reviewStorage.create(newReview(user2Id, film2Id, "r2", false));

            assertThat(reviewStorage.findAll(10)).hasSize(2);
        }

        @Test
        @DisplayName("count ограничивает размер выборки")
        void findByFilmId_Should_LimitResultByCount_Test() {
            reviewStorage.create(newReview(user1Id, film1Id, "r1", true));
            reviewStorage.create(newReview(user2Id, film1Id, "r2", false));
            reviewStorage.create(newReview(user3Id, film1Id, "r3", true));

            assertThat(reviewStorage.findByFilmId(film1Id, 2)).hasSize(2);
            assertThat(reviewStorage.findAll(1)).hasSize(1);
        }

        @Test
        @DisplayName("Сортировка по useful DESC, при равенстве — по id ASC")
        void findByFilmId_Should_SortByUsefulDescThenIdAsc_Test() {
            Review bad = reviewStorage.create(newReview(user1Id, film1Id, "bad", false));
            Review good = reviewStorage.create(newReview(user2Id, film1Id, "good", true));
            Review neutral = reviewStorage.create(newReview(user3Id, film1Id, "neutral", true));

            reviewStorage.addLikeIfNotExists(good.getReviewId(), user1Id);
            reviewStorage.addLikeIfNotExists(good.getReviewId(), user3Id);
            reviewStorage.addDislikeIfNotExists(bad.getReviewId(), user1Id);

            List<Review> reviews = reviewStorage.findByFilmId(film1Id, 10);

            assertThat(reviews).hasSize(3);
            assertThat(reviews.get(0).getReviewId()).isEqualTo(good.getReviewId());
            assertThat(reviews.get(1).getReviewId()).isEqualTo(neutral.getReviewId());
            assertThat(reviews.get(2).getReviewId()).isEqualTo(bad.getReviewId());
        }
    }

    @Nested
    @DisplayName("Тесты addLikeIfNotExists()")
    class AddLikeTests {

        @Test
        @DisplayName("Лайк увеличивает useful на 1")
        void addLikeIfNotExists_Should_IncrementUseful_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));

            assertThat(reviewStorage.addLikeIfNotExists(saved.getReviewId(), user1Id)).isTrue();
            assertThat(reviewStorage.findById(saved.getReviewId()).orElseThrow().getUseful()).isEqualTo(1);
        }

        @Test
        @DisplayName("Повторный лайк от того же пользователя не меняет useful")
        void addLikeIfNotExists_Should_NotChangeUseful_OnDuplicate_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            reviewStorage.addLikeIfNotExists(saved.getReviewId(), user1Id);

            assertThat(reviewStorage.addLikeIfNotExists(saved.getReviewId(), user1Id)).isFalse();
            assertThat(reviewStorage.findById(saved.getReviewId()).orElseThrow().getUseful()).isEqualTo(1);
        }

        @Test
        @DisplayName("Лайк не проходит, если уже стоит дизлайк того же пользователя")
        void addLikeIfNotExists_Should_NotAddLike_WhenDislikeExists_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            reviewStorage.addDislikeIfNotExists(saved.getReviewId(), user1Id);

            assertThat(reviewStorage.addLikeIfNotExists(saved.getReviewId(), user1Id)).isFalse();
            assertThat(reviewStorage.findById(saved.getReviewId()).orElseThrow().getUseful()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Тесты addDislikeIfNotExists()")
    class AddDislikeTests {

        @Test
        @DisplayName("Дизлайк уменьшает useful на 1")
        void addDislikeIfNotExists_Should_DecrementUseful_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));

            assertThat(reviewStorage.addDislikeIfNotExists(saved.getReviewId(), user1Id)).isTrue();
            assertThat(reviewStorage.findById(saved.getReviewId()).orElseThrow().getUseful()).isEqualTo(-1);
        }

        @Test
        @DisplayName("Повторный дизлайк не меняет useful")
        void addDislikeIfNotExists_Should_NotChangeUseful_OnDuplicate_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            reviewStorage.addDislikeIfNotExists(saved.getReviewId(), user1Id);

            assertThat(reviewStorage.addDislikeIfNotExists(saved.getReviewId(), user1Id)).isFalse();
            assertThat(reviewStorage.findById(saved.getReviewId()).orElseThrow().getUseful()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Тесты deleteLikeIfExists() / deleteDislikeIfExists()")
    class RemoveOpinionTests {

        @Test
        @DisplayName("Снятие лайка уменьшает useful обратно")
        void deleteLikeIfExists_Should_RestoreUseful_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            reviewStorage.addLikeIfNotExists(saved.getReviewId(), user1Id);

            assertThat(reviewStorage.deleteLikeIfExists(saved.getReviewId(), user1Id)).isTrue();
            assertThat(reviewStorage.findById(saved.getReviewId()).orElseThrow().getUseful()).isZero();
        }

        @Test
        @DisplayName("Снятие дизлайка увеличивает useful обратно")
        void deleteDislikeIfExists_Should_RestoreUseful_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            reviewStorage.addDislikeIfNotExists(saved.getReviewId(), user1Id);

            assertThat(reviewStorage.deleteDislikeIfExists(saved.getReviewId(), user1Id)).isTrue();
            assertThat(reviewStorage.findById(saved.getReviewId()).orElseThrow().getUseful()).isZero();
        }

        @Test
        @DisplayName("Снятие несуществующего лайка → false, useful не меняется")
        void deleteLikeIfExists_Should_ReturnFalse_ForNonExistingLike_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            assertThat(reviewStorage.deleteLikeIfExists(saved.getReviewId(), user1Id)).isFalse();
            assertThat(reviewStorage.findById(saved.getReviewId()).orElseThrow().getUseful()).isZero();
        }

        @Test
        @DisplayName("Снятие дизлайка, которого не было → false")
        void deleteDislikeIfExists_Should_ReturnFalse_ForNonExistingDislike_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            assertThat(reviewStorage.deleteDislikeIfExists(saved.getReviewId(), user1Id)).isFalse();
            assertThat(reviewStorage.findById(saved.getReviewId()).orElseThrow().getUseful()).isZero();
        }

        @Test
        @DisplayName("Снятие чужого лайка не работает")
        void deleteLikeIfExists_Should_NotDeleteLike_OfAnotherUser_Test() {
            Review saved = reviewStorage.create(newReview(user1Id, film1Id, "x", true));
            reviewStorage.addLikeIfNotExists(saved.getReviewId(), user1Id);

            assertThat(reviewStorage.deleteLikeIfExists(saved.getReviewId(), user2Id)).isFalse();
            assertThat(reviewStorage.findById(saved.getReviewId()).orElseThrow().getUseful()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Тесты findBySeveralIds()")
    class FindBySeveralIdsTests {

        @Test
        @DisplayName("Возвращает только запрошенные отзывы")
        void findBySeveralIds_Should_ReturnRequestedReviews_Test() {
            Review r1 = reviewStorage.create(newReview(user1Id, film1Id, "r1", true));
            Review r2 = reviewStorage.create(newReview(user2Id, film1Id, "r2", false));
            reviewStorage.create(newReview(user3Id, film1Id, "r3", true));

            List<Review> result = reviewStorage.findBySeveralIds(List.of(r1.getReviewId(), r2.getReviewId()));

            assertThat(result).hasSize(2)
                    .extracting(Review::getReviewId)
                    .containsExactlyInAnyOrder(r1.getReviewId(), r2.getReviewId());
        }

        @Test
        @DisplayName("Пустой список id → пустой результат")
        void findBySeveralIds_Should_ReturnEmptyList_ForEmptyIds_Test() {
            assertThat(reviewStorage.findBySeveralIds(List.of())).isEmpty();
        }

        @Test
        @DisplayName("Несуществующие id игнорируются")
        void findBySeveralIds_Should_IgnoreNonExistingIds_Test() {
            Review r1 = reviewStorage.create(newReview(user1Id, film1Id, "r1", true));
            List<Review> result = reviewStorage.findBySeveralIds(List.of(r1.getReviewId(), 999L));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReviewId()).isEqualTo(r1.getReviewId());
        }
    }
}