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
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmGenresDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.NewGenreRequest;
import ru.yandex.practicum.filmorate.dto.UpdateGenreRequest;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.Role;
import ru.yandex.practicum.filmorate.security.UserPrincipal;

import java.time.LocalDate;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("GenreController Тесты (админские эндпоинты)")
class GenreAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final FilmGenresDbStorage filmGenresStorage;
    private final FilmDbStorage filmStorage;
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
        jdbcTemplate.update("DELETE FROM film_genres WHERE genre_id IN (SELECT id FROM genres WHERE name LIKE 'TestGenre%')");
        jdbcTemplate.update("DELETE FROM films WHERE name = 'TestFilm'");
        jdbcTemplate.update("DELETE FROM genres WHERE name LIKE 'TestGenre%'");
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

    private Long createFilm(String name) {
        return filmStorage.create(Film.builder()
                .name(name)
                .description("desc")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpa(new RatingMpaaId(1L))
                .build()).getId();
    }

    private Long createGenre(String name) throws Exception {
        NewGenreRequest req = new NewGenreRequest();
        req.setName(name);

        String response = mockMvc.perform(post("/admin/genres")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    @Nested
    @DisplayName("POST /admin/genres")
    class CreateTests {

        @Test
        @DisplayName("Создание админом: 201 + тело")
        void create_Should_ReturnCreated_Test() throws Exception {
            NewGenreRequest req = new NewGenreRequest();
            req.setName("TestGenre1");

            mockMvc.perform(post("/admin/genres")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("TestGenre1"));
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void create_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            NewGenreRequest req = new NewGenreRequest();
            req.setName("TestGenre1");

            mockMvc.perform(post("/admin/genres")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("С ролью USER → 403")
        void create_Should_ReturnForbidden_ForUser_Test() throws Exception {
            NewGenreRequest req = new NewGenreRequest();
            req.setName("TestGenre1");

            mockMvc.perform(post("/admin/genres")
                            .with(user(userPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Пустое имя → 400")
        void create_Should_ReturnBadRequest_ForBlankName_Test() throws Exception {
            NewGenreRequest req = new NewGenreRequest();
            req.setName("");

            mockMvc.perform(post("/admin/genres")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Дубль имени → 400")
        void create_Should_ReturnBadRequest_OnDuplicate_Test() throws Exception {
            NewGenreRequest req = new NewGenreRequest();
            req.setName("TestGenre1");
            String body = objectMapper.writeValueAsString(req);

            mockMvc.perform(post("/admin/genres")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/admin/genres")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /admin/genres")
    class UpdateTests {

        @Test
        @DisplayName("Обновление имени")
        void update_Should_ChangeName_Test() throws Exception {
            Long id = createGenre("TestGenre1");

            UpdateGenreRequest req = new UpdateGenreRequest();
            req.setId(id);
            req.setName("TestGenre1Updated");

            mockMvc.perform(put("/admin/genres")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("TestGenre1Updated"));
        }

        @Test
        @DisplayName("Несуществующий id → 404")
        void update_Should_ReturnNotFound_Test() throws Exception {
            UpdateGenreRequest req = new UpdateGenreRequest();
            req.setId(999L);
            req.setName("TestGenre1");

            mockMvc.perform(put("/admin/genres")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/genres/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Удаление существующего → 204")
        void delete_Should_ReturnNoContent_Test() throws Exception {
            Long id = createGenre("TestGenre1");

            mockMvc.perform(delete("/admin/genres/{id}", id)
                            .with(user(adminPrincipal)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Несуществующий → 404")
        void delete_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(delete("/admin/genres/{id}", 999L)
                            .with(user(adminPrincipal)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Занятый фильмом жанр → 400")
        void delete_Should_ReturnBadRequest_WhenGenreInUse_Test() throws Exception {
            Long genreId = createGenre("TestGenre1");
            Long filmId = createFilm("TestFilm");

            filmGenresStorage.insert(filmId, Set.of(genreId));

            mockMvc.perform(delete("/admin/genres/{id}", genreId)
                            .with(user(adminPrincipal)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void delete_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(delete("/admin/genres/{id}", 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("С ролью USER → 403")
        void delete_Should_ReturnForbidden_ForUser_Test() throws Exception {
            mockMvc.perform(delete("/admin/genres/{id}", 1L)
                            .with(user(userPrincipal)))
                    .andExpect(status().isForbidden());
        }
    }
}