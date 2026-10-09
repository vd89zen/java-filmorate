package ru.yandex.practicum.filmorate.controller.pub;

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
import ru.yandex.practicum.filmorate.dto.NewUserRequest;
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
@DisplayName("UserController Тесты (публичные эндпоинты)")
class UserPublicControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final UserDbStorage userStorage;

    @BeforeEach
    void setUp() {
        cleanUp();
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

    private String userJson(String email, String login) throws Exception {
        NewUserRequest r = new NewUserRequest();
        r.setEmail(email);
        r.setLogin(login);
        r.setName(login);
        r.setBirthday(LocalDate.of(1990, 1, 1));
        r.setPassword("secret123");
        return objectMapper.writeValueAsString(r);
    }

    private Long createUserDirect(String email, String name) {
        return userStorage.create(User.builder()
                .email(email)
                .login(email)
                .name(name)
                .birthday(LocalDate.of(1990, 1, 1))
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .role(Role.USER)
                .build()).getId();
    }

    @Nested
    @DisplayName("POST /users")
    class CreateTests {

        @Test
        @DisplayName("201 + тело с id, без email в ответе")
        void create_Should_ReturnCreated_Test() throws Exception {
            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJson("a@mail.com", "a")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.login").value("a"));
        }

        @Test
        @DisplayName("Некорректный email → 400")
        void create_Should_ReturnBadRequest_ForBadEmail_Test() throws Exception {
            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJson("not-an-email", "a")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Без пароля → 400")
        void create_Should_ReturnBadRequest_WithoutPassword_Test() throws Exception {
            NewUserRequest r = new NewUserRequest();
            r.setEmail("a@mail.com");
            r.setLogin("a");
            r.setName("a");
            r.setBirthday(LocalDate.of(1990, 1, 1));

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(r)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Дубль email → 400")
        void create_Should_ReturnBadRequest_OnDuplicateEmail_Test() throws Exception {
            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJson("a@mail.com", "a")))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJson("a@mail.com", "b")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /users/{id} — публичный профиль")
    class FindByIdTests {

        @Test
        @DisplayName("200, отдаётся без email и без пароля")
        void findById_Should_ReturnPublicProfile_Test() throws Exception {
            Long userId = createUserDirect("a@mail.com", "UserA");

            mockMvc.perform(get("/users/{id}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.login").value("a@mail.com"))
                    .andExpect(jsonPath("$.name").value("UserA"))
                    .andExpect(jsonPath("$.email").doesNotExist())
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.role").doesNotExist());
        }

        @Test
        @DisplayName("404 для несуществующего")
        void findById_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(get("/users/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }
}