package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.NewReviewRequest;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.dto.UpdateReviewRequest;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты ReviewController")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;

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
                .email(email).login(email).name(name)
                .birthday(LocalDate.of(1990, 1, 1)).build()).getId();
    }

    private Long createFilm(String name) {
        return filmStorage.create(Film.builder()
                .name(name).description("desc")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120).mpa(new RatingMpaaId(1L)).build()).getId();
    }

    private String createReviewJson() throws Exception {
        NewReviewRequest request = new NewReviewRequest();
        request.setUserId(user1Id);
        request.setFilmId(filmId);
        request.setContent("bad film");
        request.setIsPositive(false);
        return objectMapper.writeValueAsString(request);
    }

    @Nested
    @DisplayName("Тесты POST /reviews")
    class CreateTests {

        @Test
        @DisplayName("Создание: 201 и тело с useful = 0")
        void create_Should_ReturnCreatedReviewWithZeroUseful_Test() throws Exception {
            mockMvc.perform(post("/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createReviewJson()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reviewId").isNumber())
                    .andExpect(jsonPath("$.content").value("bad film"))
                    .andExpect(jsonPath("$.isPositive").value(false))
                    .andExpect(jsonPath("$.userId").value(user1Id))
                    .andExpect(jsonPath("$.filmId").value(filmId))
                    .andExpect(jsonPath("$.useful").value(0));
        }

        @Test
        @DisplayName("Пустой content → 400")
        void create_Should_ReturnBadRequest_ForBlankContent_Test() throws Exception {
            NewReviewRequest request = new NewReviewRequest();
            request.setUserId(user1Id);
            request.setFilmId(filmId);
            request.setContent("");
            request.setIsPositive(false);

            mockMvc.perform(post("/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Без userId → 400")
        void create_Should_ReturnBadRequest_WhenUserIdIsMissing_Test() throws Exception {
            NewReviewRequest request = new NewReviewRequest();
            request.setFilmId(filmId);
            request.setContent("x");
            request.setIsPositive(true);

            mockMvc.perform(post("/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Повторный отзыв от того же пользователя → 400")
        void create_Should_ReturnBadRequest_OnDuplicateReview_Test() throws Exception {
            mockMvc.perform(post("/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createReviewJson()))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createReviewJson()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Тесты PUT /reviews")
    class UpdateTests {

        @Test
        @DisplayName("Обновление только isPositive")
        void update_Should_UpdateOnlyIsPositive_Test() throws Exception {
            Long reviewId = createReview();

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(reviewId);
            request.setIsPositive(true);

            mockMvc.perform(put("/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("bad film"))
                    .andExpect(jsonPath("$.isPositive").value(true));
        }

        @Test
        @DisplayName("Обновление только content")
        void update_Should_UpdateOnlyContent_Test() throws Exception {
            Long reviewId = createReview();

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(reviewId);
            request.setContent("new content");

            mockMvc.perform(put("/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("new content"))
                    .andExpect(jsonPath("$.isPositive").value(false));
        }

        @Test
        @DisplayName("Пустой апдейт → 400")
        void update_Should_ReturnBadRequest_ForEmptyUpdate_Test() throws Exception {
            Long reviewId = createReview();

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(reviewId);

            mockMvc.perform(put("/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Тесты GET /reviews/{id}")
    class FindByIdTests {

        @Test
        @DisplayName("Успешное получение")
        void findById_Should_ReturnReview_Test() throws Exception {
            Long reviewId = createReview();

            mockMvc.perform(get("/reviews/{id}", reviewId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewId").value(reviewId));
        }

        @Test
        @DisplayName("Несуществующий → 404")
        void findById_Should_ReturnNotFound_ForNonExistingReview_Test() throws Exception {
            mockMvc.perform(get("/reviews/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Тесты GET /reviews")
    class FindAllTests {

        @Test
        @DisplayName("Без filmId → все отзывы")
        void findAll_Should_ReturnAllReviews_WhenFilmIdIsMissing_Test() throws Exception {
            createReview();

            mockMvc.perform(get("/reviews"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("С filmId → только отзывы фильма")
        void findAll_Should_ReturnOnlyReviewsOfFilm_Test() throws Exception {
            createReview();
            Long otherFilm = createFilm("Film2");
            NewReviewRequest other = new NewReviewRequest();
            other.setUserId(user2Id);
            other.setFilmId(otherFilm);
            other.setContent("other");
            other.setIsPositive(true);
            mockMvc.perform(post("/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(other)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/reviews").param("filmId", String.valueOf(filmId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("count = 101 → 400 (@Max)")
        void findAll_Should_ReturnBadRequest_WhenCountExceedsMax_Test() throws Exception {
            mockMvc.perform(get("/reviews").param("count", "101"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Тесты PUT/DELETE like и dislike")
    class OpinionTests {

        @Test
        @DisplayName("PUT like: useful становится 1")
        void addLike_Should_IncrementUseful_Test() throws Exception {
            Long reviewId = createReview();

            mockMvc.perform(put("/reviews/{id}/like/{userId}", reviewId, user2Id))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/reviews/{id}", reviewId))
                    .andExpect(jsonPath("$.useful").value(1));
        }

        @Test
        @DisplayName("PUT dislike: useful становится -1")
        void addDislike_Should_DecrementUseful_Test() throws Exception {
            Long reviewId = createReview();

            mockMvc.perform(put("/reviews/{id}/dislike/{userId}", reviewId, user2Id))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/reviews/{id}", reviewId))
                    .andExpect(jsonPath("$.useful").value(-1));
        }

        @Test
        @DisplayName("DELETE like: useful возвращается к 0")
        void removeLike_Should_RestoreUseful_Test() throws Exception {
            Long reviewId = createReview();

            mockMvc.perform(put("/reviews/{id}/like/{userId}", reviewId, user2Id))
                    .andExpect(status().isNoContent());
            mockMvc.perform(delete("/reviews/{id}/like/{userId}", reviewId, user2Id))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/reviews/{id}", reviewId))
                    .andExpect(jsonPath("$.useful").value(0));
        }

        @Test
        @DisplayName("DELETE dislike: useful возвращается к 0")
        void removeDislike_Should_RestoreUseful_Test() throws Exception {
            Long reviewId = createReview();

            mockMvc.perform(put("/reviews/{id}/dislike/{userId}", reviewId, user2Id))
                    .andExpect(status().isNoContent());
            mockMvc.perform(delete("/reviews/{id}/dislike/{userId}", reviewId, user2Id))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/reviews/{id}", reviewId))
                    .andExpect(jsonPath("$.useful").value(0));
        }

        @Test
        @DisplayName("Лайк несуществующего отзыва → 404")
        void addLike_Should_ReturnNotFound_ForNonExistingReview_Test() throws Exception {
            mockMvc.perform(put("/reviews/{id}/like/{userId}", 999L, user2Id))
                    .andExpect(status().isNotFound());
        }
    }

    private Long createReview() throws Exception {
        String response = mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReviewJson()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("reviewId").asLong();
    }
}