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
import ru.yandex.practicum.filmorate.dto.NewDirectorRequest;
import ru.yandex.practicum.filmorate.dto.UpdateDirectorRequest;
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
@DisplayName("DirectorController Тесты (админские эндпоинты)")
class DirectorAdminControllerTest {

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
        jdbcTemplate.execute("DELETE FROM film_directors");
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

    @Nested
    @DisplayName("POST /admin/directors")
    class CreateTests {

        @Test
        @DisplayName("Создание админом: 201 + тело")
        void create_Should_ReturnCreated_Test() throws Exception {
            NewDirectorRequest req = new NewDirectorRequest();
            req.setName("Nolan");

            mockMvc.perform(post("/admin/directors")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("Nolan"));
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void create_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            NewDirectorRequest req = new NewDirectorRequest();
            req.setName("Nolan");

            mockMvc.perform(post("/admin/directors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("С ролью USER → 403")
        void create_Should_ReturnForbidden_ForUser_Test() throws Exception {
            NewDirectorRequest req = new NewDirectorRequest();
            req.setName("Nolan");

            mockMvc.perform(post("/admin/directors")
                            .with(user(userPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Пустое имя → 400")
        void create_Should_ReturnBadRequest_ForBlankName_Test() throws Exception {
            NewDirectorRequest req = new NewDirectorRequest();
            req.setName("");

            mockMvc.perform(post("/admin/directors")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Дубль имени → 400")
        void create_Should_ReturnBadRequest_OnDuplicate_Test() throws Exception {
            NewDirectorRequest req = new NewDirectorRequest();
            req.setName("Nolan");
            String body = objectMapper.writeValueAsString(req);

            mockMvc.perform(post("/admin/directors")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/admin/directors")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /admin/directors")
    class UpdateTests {

        @Test
        @DisplayName("Обновление имени")
        void update_Should_ChangeName_Test() throws Exception {
            Long id = createDirector("Old");

            UpdateDirectorRequest req = new UpdateDirectorRequest();
            req.setId(id);
            req.setName("New");

            mockMvc.perform(put("/admin/directors")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New"));
        }

        @Test
        @DisplayName("Несуществующий id → 404")
        void update_Should_ReturnNotFound_Test() throws Exception {
            UpdateDirectorRequest req = new UpdateDirectorRequest();
            req.setId(999L);
            req.setName("X");

            mockMvc.perform(put("/admin/directors")
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/directors/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Удаление существующего → 204")
        void delete_Should_ReturnNoContent_Test() throws Exception {
            Long id = createDirector("ToDelete");

            mockMvc.perform(delete("/admin/directors/{id}", id)
                            .with(user(adminPrincipal)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Несуществующий → 404")
        void delete_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(delete("/admin/directors/{id}", 999L)
                            .with(user(adminPrincipal)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void delete_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(delete("/admin/directors/{id}", 999L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("С ролью USER → 403")
        void delete_Should_ReturnForbidden_ForUser_Test() throws Exception {
            mockMvc.perform(delete("/admin/directors/{id}", 999L)
                            .with(user(userPrincipal)))
                    .andExpect(status().isForbidden());
        }
    }

    private Long createDirector(String name) throws Exception {
        NewDirectorRequest req = new NewDirectorRequest();
        req.setName(name);

        String response = mockMvc.perform(post("/admin/directors")
                        .with(user(adminPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}