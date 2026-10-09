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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.LoginRequest;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.Role;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("AuthController Тесты")
class AuthControllerTest {

    private static final String USER_EMAIL = "user@mail.com";
    private static final String USER_PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final UserDbStorage userStorage;

    @BeforeEach
    void setUp() {
        cleanUp();
        createUser(USER_EMAIL, USER_PASSWORD, Role.USER);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private Long createUser(String email, String rawPassword, Role role) {
        return userStorage.create(User.builder()
                .email(email)
                .login(email)
                .name(role.name())
                .birthday(LocalDate.of(1990, 1, 1))
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build()).getId();
    }

    private String loginJson(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return objectMapper.writeValueAsString(req);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Nested
    @DisplayName("Успешный логин")
    class SuccessTests {

        @Test
        @DisplayName("200 + accessToken, tokenType, expiresIn")
        void login_Should_ReturnToken_Test() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(USER_EMAIL, USER_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isString())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").isNumber());
        }

        @Test
        @DisplayName("Email не чувствителен к регистру")
        void login_Should_BeCaseInsensitiveForEmail_Test() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("USER@MAIL.COM", USER_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isString());
        }

        @Test
        @DisplayName("Пробелы по краям email игнорируются")
        void login_Should_TrimEmailWhitespace_Test() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("  " + USER_EMAIL + "  ", USER_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isString());
        }
    }

    @Nested
    @DisplayName("Неуспешный логин")
    class FailureTests {

        @Test
        @DisplayName("Неверный пароль → 401")
        void login_Should_ReturnUnauthorized_OnWrongPassword_Test() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(USER_EMAIL, "wrong-password")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Несуществующий email → 401")
        void login_Should_ReturnUnauthorized_OnMissingUser_Test() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("nobody@mail.com", "any")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Пустой пароль → 400 (@NotBlank)")
        void login_Should_ReturnBadRequest_ForBlankPassword_Test() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(USER_EMAIL, "")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Пустой email → 400")
        void login_Should_ReturnBadRequest_ForBlankEmail_Test() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("", USER_PASSWORD)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Некорректный email → 400 (@Email)")
        void login_Should_ReturnBadRequest_ForInvalidEmail_Test() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson("not-an-email", USER_PASSWORD)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Битый JSON → 400")
        void login_Should_ReturnBadRequest_ForMalformedJson_Test() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ not a json }"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Использование полученного токена")
    class TokenUsageTests {

        @Test
        @DisplayName("Bearer-токен даёт доступ к /me/recommendations")
        void token_Should_GrantAccessToProtectedEndpoint_Test() throws Exception {
            String token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

            mockMvc.perform(get("/me/recommendations")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Мусорный Bearer-токен → 401")
        void garbageToken_Should_ReturnUnauthorized_Test() throws Exception {
            mockMvc.perform(get("/me/recommendations")
                            .header("Authorization", "Bearer not-a-jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Токен без префикса Bearer → 401")
        void tokenWithoutBearerPrefix_Should_ReturnUnauthorized_Test() throws Exception {
            String token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

            mockMvc.perform(get("/me/recommendations")
                            .header("Authorization", token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Токен админа даёт доступ к /admin/users")
        void adminToken_Should_GrantAccessToAdminEndpoint_Test() throws Exception {
            createUser("admin@mail.com", "admin123", Role.ADMIN);

            String token = loginAndGetToken("admin@mail.com", "admin123");

            mockMvc.perform(get("/admin/users")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Токен обычного пользователя не даёт доступ к /admin/users → 403")
        void userToken_Should_ReturnForbidden_OnAdminEndpoint_Test() throws Exception {
            String token = loginAndGetToken(USER_EMAIL, USER_PASSWORD);

            mockMvc.perform(get("/admin/users")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }
}