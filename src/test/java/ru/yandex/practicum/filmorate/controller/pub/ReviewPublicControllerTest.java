package ru.yandex.practicum.filmorate.controller.pub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.ReviewDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.Role;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("ReviewController Тесты (публичные эндпоинты)")
class ReviewPublicControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;
    private final ReviewDbStorage reviewStorage;

    private Long user1Id;
    private Long user2Id;
    private Long film1Id;
    private Long film2Id;

    @BeforeEach
    void setUp() {
        cleanUp();
        user1Id = createUser("user1@mail.com", "User1");
        user2Id = createUser("user2@mail.com", "User2");
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
        jdbcTemplate.execute("DELETE FROM film_directors");
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

    private Long createReview(Long userId, Long filmId, String content, boolean isPositive) {
        return reviewStorage.create(Review.builder()
                .content(content).isPositive(isPositive)
                .userId(userId).filmId(filmId).useful(0).build()).getReviewId();
    }

    @Nested
    @DisplayName("GET /reviews/{id}")
    class FindByIdTests {

        @Test
        @DisplayName("Успешное получение — без аутентификации")
        void findById_Should_ReturnReview_Test() throws Exception {
            Long reviewId = createReview(user1Id, film1Id, "bad film", false);

            mockMvc.perform(get("/reviews/{id}", reviewId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewId").value(reviewId))
                    .andExpect(jsonPath("$.content").value("bad film"));
        }

        @Test
        @DisplayName("Несуществующий → 404")
        void findById_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(get("/reviews/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /reviews")
    class FindAllTests {

        @Test
        @DisplayName("Без filmId → все отзывы")
        void findAll_Should_ReturnAllReviews_Test() throws Exception {
            createReview(user1Id, film1Id, "r1", true);
            createReview(user2Id, film2Id, "r2", false);

            mockMvc.perform(get("/reviews"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("С filmId → только отзывы указанного фильма")
        void findAll_Should_ReturnOnlyReviewsOfFilm_Test() throws Exception {
            createReview(user1Id, film1Id, "r1", true);
            createReview(user2Id, film2Id, "r2", false);

            mockMvc.perform(get("/reviews").param("filmId", String.valueOf(film1Id)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].filmId").value(film1Id));
        }

        @Test
        @DisplayName("count = 101 → 400 (@Max)")
        void findAll_Should_ReturnBadRequest_WhenCountExceedsMax_Test() throws Exception {
            mockMvc.perform(get("/reviews").param("count", "101"))
                    .andExpect(status().isBadRequest());
        }
    }
}