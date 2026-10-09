package ru.yandex.practicum.filmorate.controller.auth;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmLikesDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.Role;
import ru.yandex.practicum.filmorate.security.UserPrincipal;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("FilmController Тесты (аутентифицированные эндпоинты)")
class FilmAuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;
    private final FilmLikesDbStorage filmLikesStorage;

    private UserPrincipal user1Principal;
    private UserPrincipal user2Principal;
    private Long user1Id;
    private Long user2Id;
    private Long film1Id;
    private Long film2Id;

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

    @Nested
    @DisplayName("PUT /me/films/{filmId}/like")
    class AddLikeTests {

        @Test
        @DisplayName("Ставит лайк: 204, лайк от текущего пользователя")
        void addLike_Should_ReturnNoContent_AndCreateLike_Test() throws Exception {
            mockMvc.perform(put("/me/films/{filmId}/like", film1Id)
                            .with(user(user1Principal)))
                    .andExpect(status().isNoContent());

            assertThat(filmLikesStorage.hasUserLikedFilm(film1Id, user1Id)).isTrue();
        }

        @Test
        @DisplayName("Повторный лайк → 400")
        void addLike_Should_ReturnBadRequest_OnDuplicate_Test() throws Exception {
            mockMvc.perform(put("/me/films/{filmId}/like", film1Id)
                            .with(user(user1Principal)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(put("/me/films/{filmId}/like", film1Id)
                            .with(user(user1Principal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Несуществующий фильм → 404")
        void addLike_Should_ReturnNotFound_ForMissingFilm_Test() throws Exception {
            mockMvc.perform(put("/me/films/{filmId}/like", 999L)
                            .with(user(user1Principal)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void addLike_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(put("/me/films/{filmId}/like", film1Id))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /me/films/{filmId}/like")
    class RemoveLikeTests {

        @Test
        @DisplayName("Снимает лайк: 204")
        void removeLike_Should_ReturnNoContent_Test() throws Exception {
            filmLikesStorage.addLikeIfNotExists(film1Id, user1Id);

            mockMvc.perform(delete("/me/films/{filmId}/like", film1Id)
                            .with(user(user1Principal)))
                    .andExpect(status().isNoContent());

            assertThat(filmLikesStorage.hasUserLikedFilm(film1Id, user1Id)).isFalse();
        }

        @Test
        @DisplayName("Нет лайка → 404")
        void removeLike_Should_ReturnNotFound_WhenNoLike_Test() throws Exception {
            mockMvc.perform(delete("/me/films/{filmId}/like", film1Id)
                            .with(user(user1Principal)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Нельзя снять лайк другого пользователя (свой userId из токена)")
        void removeLike_Should_NotAffectOtherUsersLike_Test() throws Exception {
            filmLikesStorage.addLikeIfNotExists(film1Id, user2Id);

            mockMvc.perform(delete("/me/films/{filmId}/like", film1Id)
                            .with(user(user1Principal)))
                    .andExpect(status().isNotFound());

            assertThat(filmLikesStorage.hasUserLikedFilm(film1Id, user2Id)).isTrue();
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void removeLike_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(delete("/me/films/{filmId}/like", film1Id))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /me/films/common")
    class CommonFilmsTests {

        @Test
        @DisplayName("Возвращает общие фильмы пользователей")
        void getCommonFilms_Should_ReturnCommon_Test() throws Exception {
            filmLikesStorage.addLikeIfNotExists(film1Id, user1Id);
            filmLikesStorage.addLikeIfNotExists(film1Id, user2Id);
            filmLikesStorage.addLikeIfNotExists(film2Id, user1Id);

            mockMvc.perform(get("/me/films/common")
                            .with(user(user1Principal))
                            .param("friendId", String.valueOf(user2Id)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(film1Id));
        }

        @Test
        @DisplayName("Нет общих фильмов → пустой список")
        void getCommonFilms_Should_ReturnEmpty_WhenNoCommon_Test() throws Exception {
            filmLikesStorage.addLikeIfNotExists(film1Id, user1Id);
            filmLikesStorage.addLikeIfNotExists(film2Id, user2Id);

            mockMvc.perform(get("/me/films/common")
                            .with(user(user1Principal))
                            .param("friendId", String.valueOf(user2Id)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void getCommonFilms_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(get("/me/films/common")
                            .param("friendId", String.valueOf(user2Id)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Без friendId → 400")
        void getCommonFilms_Should_ReturnBadRequest_WithoutFriendId_Test() throws Exception {
            mockMvc.perform(get("/me/films/common")
                            .with(user(user1Principal)))
                    .andExpect(status().isBadRequest());
        }
    }
}