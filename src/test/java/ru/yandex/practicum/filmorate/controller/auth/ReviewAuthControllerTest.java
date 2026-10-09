package ru.yandex.practicum.filmorate.controller.auth;

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
import ru.yandex.practicum.filmorate.dal.ReviewDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.NewReviewRequest;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.dto.UpdateReviewRequest;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.Role;
import ru.yandex.practicum.filmorate.security.UserPrincipal;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("ReviewController Тесты (аутентифицированные эндпоинты)")
class ReviewAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;
    private final ReviewDbStorage reviewStorage;

    private Long user1Id;
    private Long user2Id;
    private Long film1Id;
    private Long film2Id;

    private UserPrincipal user1Principal;
    private UserPrincipal user2Principal;

    @BeforeEach
    void setUp() {
        cleanUp();

        User u1 = createUser("user1@mail.com", "User1");
        User u2 = createUser("user2@mail.com", "User2");
        user1Id = u1.getId();
        user2Id = u2.getId();
        user1Principal = new UserPrincipal(u1);
        user2Principal = new UserPrincipal(u2);

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

    private User createUser(String email, String name) {
        return userStorage.create(User.builder()
                .email(email)
                .login(email)
                .name(name)
                .birthday(LocalDate.of(1990, 1, 1))
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .role(Role.USER)
                .build());
    }

    private Long createFilm(String name) {
        return filmStorage.create(Film.builder()
                .name(name).description("desc")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120).mpa(new RatingMpaaId(1L)).build()).getId();
    }

    private String createReviewJson(Long filmId, String content, boolean isPositive) throws Exception {
        NewReviewRequest request = new NewReviewRequest();
        request.setFilmId(filmId);
        request.setContent(content);
        request.setIsPositive(isPositive);
        return objectMapper.writeValueAsString(request);
    }

    private Long createReview(Long filmId, String content, boolean isPositive) throws Exception {
        String response = mockMvc.perform(post("/me/reviews")
                        .with(user(user1Principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createReviewJson(filmId, content, isPositive)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("reviewId").asLong();
    }

    @Nested
    @DisplayName("POST /me/reviews")
    class CreateTests {

        @Test
        @DisplayName("Создание: 201 и тело с useful = 0, userId из токена")
        void create_Should_ReturnCreatedReviewWithZeroUseful_Test() throws Exception {
            mockMvc.perform(post("/me/reviews")
                            .with(user(user1Principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createReviewJson(film1Id, "bad film", false)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reviewId").isNumber())
                    .andExpect(jsonPath("$.content").value("bad film"))
                    .andExpect(jsonPath("$.isPositive").value(false))
                    .andExpect(jsonPath("$.userId").value(user1Id))
                    .andExpect(jsonPath("$.filmId").value(film1Id))
                    .andExpect(jsonPath("$.useful").value(0));
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void create_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(post("/me/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createReviewJson(film1Id, "bad film", false)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Пустой content → 400")
        void create_Should_ReturnBadRequest_ForBlankContent_Test() throws Exception {
            mockMvc.perform(post("/me/reviews")
                            .with(user(user1Principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createReviewJson(film1Id, "", false)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Без filmId → 400")
        void create_Should_ReturnBadRequest_WhenFilmIdIsMissing_Test() throws Exception {
            NewReviewRequest request = new NewReviewRequest();
            request.setContent("x");
            request.setIsPositive(true);

            mockMvc.perform(post("/me/reviews")
                            .with(user(user1Principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Повторный отзыв от того же пользователя на тот же фильм → 400")
        void create_Should_ReturnBadRequest_OnDuplicateReview_Test() throws Exception {
            createReview(film1Id, "first", true);

            mockMvc.perform(post("/me/reviews")
                            .with(user(user1Principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createReviewJson(film1Id, "second", false)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /me/reviews")
    class UpdateTests {

        @Test
        @DisplayName("Обновление только isPositive")
        void update_Should_UpdateOnlyIsPositive_Test() throws Exception {
            Long reviewId = createReview(film1Id, "bad film", false);

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(reviewId);
            request.setIsPositive(true);

            mockMvc.perform(put("/me/reviews")
                            .with(user(user1Principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("bad film"))
                    .andExpect(jsonPath("$.isPositive").value(true));
        }

        @Test
        @DisplayName("Обновление только content")
        void update_Should_UpdateOnlyContent_Test() throws Exception {
            Long reviewId = createReview(film1Id, "bad film", false);

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(reviewId);
            request.setContent("new content");

            mockMvc.perform(put("/me/reviews")
                            .with(user(user1Principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("new content"))
                    .andExpect(jsonPath("$.isPositive").value(false));
        }

        @Test
        @DisplayName("Пустой апдейт → 400")
        void update_Should_ReturnBadRequest_ForEmptyUpdate_Test() throws Exception {
            Long reviewId = createReview(film1Id, "bad film", false);

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(reviewId);

            mockMvc.perform(put("/me/reviews")
                            .with(user(user1Principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void update_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(1L);
            request.setContent("x");

            mockMvc.perform(put("/me/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Обновление чужого отзыва → 404")
        void update_Should_ReturnNotFound_ForAnotherUsersReview_Test() throws Exception {
            Long reviewId = createReview(film1Id, "x", true);

            UpdateReviewRequest request = new UpdateReviewRequest();
            request.setReviewId(reviewId);
            request.setContent("hacked");

            mockMvc.perform(put("/me/reviews")
                            .with(user(user2Principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /me/reviews/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Удаление → 204")
        void delete_Should_ReturnNoContent_Test() throws Exception {
            Long reviewId = createReview(film1Id, "x", true);

            mockMvc.perform(delete("/me/reviews/{id}", reviewId)
                            .with(user(user1Principal)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Несуществующий → 404")
        void delete_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(delete("/me/reviews/{id}", 999L)
                            .with(user(user1Principal)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void delete_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(delete("/me/reviews/{id}", 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Удаление чужого отзыва → 404")
        void delete_Should_ReturnNotFound_ForAnotherUsersReview_Test() throws Exception {
            Long reviewId = createReview(film1Id, "x", true);

            mockMvc.perform(delete("/me/reviews/{id}", reviewId)
                            .with(user(user2Principal)))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/reviews/{id}", reviewId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewId").value(reviewId));
        }
    }

    @Nested
    @DisplayName("PUT/DELETE /me/reviews/{id}/like и /dislike")
    class OpinionTests {

        @Test
        @DisplayName("PUT like: useful становится 1")
        void addLike_Should_IncrementUseful_Test() throws Exception {
            Long reviewId = createReview(film1Id, "x", true);

            mockMvc.perform(put("/me/reviews/{id}/like", reviewId)
                            .with(user(user2Principal)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/reviews/{id}", reviewId))
                    .andExpect(jsonPath("$.useful").value(1));
        }

        @Test
        @DisplayName("PUT dislike: useful становится -1")
        void addDislike_Should_DecrementUseful_Test() throws Exception {
            Long reviewId = createReview(film1Id, "x", true);

            mockMvc.perform(put("/me/reviews/{id}/dislike", reviewId)
                            .with(user(user2Principal)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/reviews/{id}", reviewId))
                    .andExpect(jsonPath("$.useful").value(-1));
        }

        @Test
        @DisplayName("DELETE like: useful возвращается к 0")
        void removeLike_Should_RestoreUseful_Test() throws Exception {
            Long reviewId = createReview(film1Id, "x", true);

            mockMvc.perform(put("/me/reviews/{id}/like", reviewId)
                            .with(user(user2Principal)))
                    .andExpect(status().isNoContent());
            mockMvc.perform(delete("/me/reviews/{id}/like", reviewId)
                            .with(user(user2Principal)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/reviews/{id}", reviewId))
                    .andExpect(jsonPath("$.useful").value(0));
        }

        @Test
        @DisplayName("DELETE dislike: useful возвращается к 0")
        void removeDislike_Should_RestoreUseful_Test() throws Exception {
            Long reviewId = createReview(film1Id, "x", true);

            mockMvc.perform(put("/me/reviews/{id}/dislike", reviewId)
                            .with(user(user2Principal)))
                    .andExpect(status().isNoContent());
            mockMvc.perform(delete("/me/reviews/{id}/dislike", reviewId)
                            .with(user(user2Principal)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/reviews/{id}", reviewId))
                    .andExpect(jsonPath("$.useful").value(0));
        }

        @Test
        @DisplayName("Лайк несуществующего отзыва → 404")
        void addLike_Should_ReturnNotFound_ForNonExistingReview_Test() throws Exception {
            mockMvc.perform(put("/me/reviews/{id}/like", 999L)
                            .with(user(user2Principal)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void addLike_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            Long reviewId = createReview(film1Id, "x", true);

            mockMvc.perform(put("/me/reviews/{id}/like", reviewId))
                    .andExpect(status().isUnauthorized());
        }
    }
}