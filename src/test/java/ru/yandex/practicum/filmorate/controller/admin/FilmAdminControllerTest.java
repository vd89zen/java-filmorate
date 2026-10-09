package ru.yandex.practicum.filmorate.controller.admin;

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
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.NewFilmRequest;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.Role;
import ru.yandex.practicum.filmorate.security.UserPrincipal;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("FilmController Тесты (админские эндпоинты)")
class FilmAdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final UserDbStorage userStorage;

    private UserPrincipal adminPrincipal;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        cleanUp();
        adminPrincipal = new UserPrincipal(createUser("admin@test.com", Role.ADMIN));
        userPrincipal = new UserPrincipal(createUser("user@test.com", Role.USER));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM film_directors");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM directors");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private User createUser(String email, Role role) {
        return userStorage.create(User.builder()
                .email(email)
                .login(email)
                .name(role.name())
                .birthday(LocalDate.of(1990, 1, 1))
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .role(role)
                .build());
    }

    private String newFilmJson(String name) throws Exception {
        NewFilmRequest r = new NewFilmRequest();
        r.setName(name);
        r.setDescription("desc");
        r.setReleaseDate(LocalDate.of(2000, 1, 1));
        r.setDuration(120);
        r.setMpa(new RatingMpaaId(1L));
        return objectMapper.writeValueAsString(r);
    }

    @Nested
    @DisplayName("POST /admin/films")
    class CreateTests {

        @Test
        @DisplayName("Создание админом: 201 + тело")
        void create_Should_ReturnCreated_Test() throws Exception {
            mockMvc.perform(post("/admin/films")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newFilmJson("Film")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("Film"));
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void create_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(post("/admin/films")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newFilmJson("Film")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("С ролью USER → 403")
        void create_Should_ReturnForbidden_ForUser_Test() throws Exception {
            mockMvc.perform(post("/admin/films")
                            .with(user(userPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newFilmJson("Film")))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Пустое имя → 400")
        void create_Should_ReturnBadRequest_ForBlankName_Test() throws Exception {
            mockMvc.perform(post("/admin/films")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newFilmJson("")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/films/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Удаление существующего → 204")
        void delete_Should_ReturnNoContent_Test() throws Exception {
            Long filmId = createFilm("FilmToDelete");

            mockMvc.perform(delete("/admin/films/{id}", filmId)
                            .with(user(adminPrincipal)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Удаление несуществующего → 404")
        void delete_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(delete("/admin/films/{id}", 999L)
                            .with(user(adminPrincipal)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void delete_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(delete("/admin/films/{id}", 999L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("С ролью USER → 403")
        void delete_Should_ReturnForbidden_ForUser_Test() throws Exception {
            mockMvc.perform(delete("/admin/films/{id}", 999L)
                            .with(user(userPrincipal)))
                    .andExpect(status().isForbidden());
        }
    }

    private Long createFilm(String name) throws Exception {
        String response = mockMvc.perform(post("/admin/films")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newFilmJson(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}