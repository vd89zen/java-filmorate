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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.UpdatePasswordRequest;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.Role;
import ru.yandex.practicum.filmorate.security.UserPrincipal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("UserController Тесты (админские эндпоинты)")
class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

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
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM events");
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
    @DisplayName("GET /admin/users")
    class FindAllTests {

        @Test
        @DisplayName("200 для админа, список пользователей")
        void findAll_Should_ReturnOk_ForAdmin_Test() throws Exception {
            mockMvc.perform(get("/admin/users")
                            .with(user(adminPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("size=101 → 400")
        void findAll_Should_ReturnBadRequest_ForSizeOverMax_Test() throws Exception {
            mockMvc.perform(get("/admin/users")
                            .with(user(adminPrincipal))
                            .param("size", "101"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("from=-1 → 400")
        void findAll_Should_ReturnBadRequest_ForNegativeFrom_Test() throws Exception {
            mockMvc.perform(get("/admin/users")
                            .with(user(adminPrincipal))
                            .param("from", "-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void findAll_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("С ролью USER → 403")
        void findAll_Should_ReturnForbidden_ForUser_Test() throws Exception {
            mockMvc.perform(get("/admin/users")
                            .with(user(userPrincipal)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /admin/users/{userId}")
    class DeleteTests {

        @Test
        @DisplayName("204 для админа, пользователь удалён")
        void delete_Should_ReturnNoContent_ForAdmin_Test() throws Exception {
            Long targetId = createUser("target@test.com", Role.USER).getId();

            mockMvc.perform(delete("/admin/users/{id}", targetId)
                            .with(user(adminPrincipal)))
                    .andExpect(status().isNoContent());

            assertThat(userStorage.findById(targetId)).isEmpty();
        }

        @Test
        @DisplayName("404 для несуществующего")
        void delete_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(delete("/admin/users/{id}", 999L)
                            .with(user(adminPrincipal)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void delete_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(delete("/admin/users/{id}", 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("С ролью USER → 403")
        void delete_Should_ReturnForbidden_ForUser_Test() throws Exception {
            Long targetId = createUser("target@test.com", Role.USER).getId();

            mockMvc.perform(delete("/admin/users/{id}", targetId)
                            .with(user(userPrincipal)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /admin/users/{userId}/password")
    class ResetPasswordTests {

        @Test
        @DisplayName("204 для админа, пароль обновлён")
        void resetPassword_Should_ReturnNoContent_AndUpdatePassword_Test() throws Exception {
            Long targetId = createUser("target@test.com", Role.USER).getId();

            UpdatePasswordRequest req = new UpdatePasswordRequest();
            req.setPassword("newSecret123");

            mockMvc.perform(put("/admin/users/{id}/password", targetId)
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNoContent());

            String hash = userStorage.findById(targetId).orElseThrow().getPassword();
            assertThat(passwordEncoder.matches("newSecret123", hash)).isTrue();
        }

        @Test
        @DisplayName("404 для несуществующего пользователя")
        void resetPassword_Should_ReturnNotFound_Test() throws Exception {
            UpdatePasswordRequest req = new UpdatePasswordRequest();
            req.setPassword("newSecret123");

            mockMvc.perform(put("/admin/users/{id}/password", 999L)
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Пустой пароль → 400")
        void resetPassword_Should_ReturnBadRequest_ForBlankPassword_Test() throws Exception {
            Long targetId = createUser("target@test.com", Role.USER).getId();

            UpdatePasswordRequest req = new UpdatePasswordRequest();
            req.setPassword("");

            mockMvc.perform(put("/admin/users/{id}/password", targetId)
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Слишком короткий пароль → 400")
        void resetPassword_Should_ReturnBadRequest_ForShortPassword_Test() throws Exception {
            Long targetId = createUser("target@test.com", Role.USER).getId();

            UpdatePasswordRequest req = new UpdatePasswordRequest();
            req.setPassword("123");

            mockMvc.perform(put("/admin/users/{id}/password", targetId)
                            .with(user(adminPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void resetPassword_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            UpdatePasswordRequest req = new UpdatePasswordRequest();
            req.setPassword("newSecret123");

            mockMvc.perform(put("/admin/users/{id}/password", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("С ролью USER → 403")
        void resetPassword_Should_ReturnForbidden_ForUser_Test() throws Exception {
            Long targetId = createUser("target@test.com", Role.USER).getId();

            UpdatePasswordRequest req = new UpdatePasswordRequest();
            req.setPassword("newSecret123");

            mockMvc.perform(put("/admin/users/{id}/password", targetId)
                            .with(user(userPrincipal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }
}