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
import ru.yandex.practicum.filmorate.dto.NewUserRequest;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        return objectMapper.writeValueAsString(r);
    }

    private Long createUser(String email, String login) throws Exception {
        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson(email, login)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    @Nested
    @DisplayName("POST /users")
    class CreateTests {

        @Test
        @DisplayName("201")
        void create_Should_ReturnCreated_Test() throws Exception {
            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJson("a@mail.com", "a")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber());
        }

        @Test
        @DisplayName("Некорректный email → 400")
        void create_Should_ReturnBadRequest_ForBadEmail_Test() throws Exception {
            mockMvc.perform(post("/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(userJson("not-an-email", "a")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /users")
    class FindAllTests {

        @Test
        @DisplayName("size=101 → 400")
        void findAll_Should_ReturnBadRequest_ForSizeOverMax_Test() throws Exception {
            mockMvc.perform(get("/users").param("size", "101"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("from=-1 → 400")
        void findAll_Should_ReturnBadRequest_ForNegativeFrom_Test() throws Exception {
            mockMvc.perform(get("/users").param("from", "-1"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /users/{id}/friends/{friendId}")
    class AddFriendTests {

        @Test
        @DisplayName("204 при успехе")
        void addFriend_Should_ReturnNoContent_Test() throws Exception {
            Long u1 = createUser("a@mail.com", "a");
            Long u2 = createUser("b@mail.com", "b");

            mockMvc.perform(put("/users/{userId}/friends/{friendId}", u1, u2))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Несуществующий пользователь → 404")
        void addFriend_Should_ReturnNotFound_Test() throws Exception {
            Long u1 = createUser("a@mail.com", "a");

            mockMvc.perform(put("/users/{userId}/friends/{friendId}", u1, 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /users/{id}/friends")
    class GetFriendsTests {

        @Test
        @DisplayName("Пустой список друзей")
        void getFriends_Should_ReturnEmpty_Test() throws Exception {
            Long u1 = createUser("a@mail.com", "a");

            mockMvc.perform(get("/users/{id}/friends", u1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /users/{id}/feed/*")
    class FeedTests {

        @Test
        @DisplayName("GET /feed/user → пустой список")
        void getFeedUser_Should_ReturnEmpty_Test() throws Exception {
            Long u1 = createUser("a@mail.com", "a");

            mockMvc.perform(get("/users/{id}/feed/user", u1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("GET /feed/user/enriched → пустой список")
        void getEnrichedFeedUser_Should_ReturnEmpty_Test() throws Exception {
            Long u1 = createUser("a@mail.com", "a");

            mockMvc.perform(get("/users/{id}/feed/user/enriched", u1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}