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
import ru.yandex.practicum.filmorate.dal.EventDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.dto.UpdateUserRequest;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;
import ru.yandex.practicum.filmorate.model.enums.OperationTypes;
import ru.yandex.practicum.filmorate.model.enums.Role;
import ru.yandex.practicum.filmorate.security.UserPrincipal;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("UserController Тесты (аутентифицированные эндпоинты)")
class UserAuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final UserDbStorage userStorage;
    private final FriendshipDbStorage friendshipDbStorage;
    private final FilmDbStorage filmStorage;
    private final EventDbStorage eventDbStorage;

    private UserPrincipal user1Principal;
    private UserPrincipal user2Principal;
    private Long user1Id;
    private Long user2Id;

    @BeforeEach
    void setUp() {
        cleanUp();

        User u1 = createUser("user1@mail.com", "User1");
        User u2 = createUser("user2@mail.com", "User2");
        user1Id = u1.getId();
        user2Id = u2.getId();
        user1Principal = new UserPrincipal(u1);
        user2Principal = new UserPrincipal(u2);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM film_directors");
        jdbcTemplate.execute("DELETE FROM reviews");
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
    @DisplayName("PUT /me — обновление своего профиля")
    class UpdateProfileTests {

        @Test
        @DisplayName("Обновляет только свой профиль")
        void update_Should_ChangeOwnProfile_Test() throws Exception {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setName("NewName");

            mockMvc.perform(put("/me")
                            .with(user(user1Principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("NewName"));

            assertThat(userStorage.findById(user1Id).orElseThrow().getName()).isEqualTo("NewName");
            assertThat(userStorage.findById(user2Id).orElseThrow().getName()).isEqualTo("User2");
        }

        @Test
        @DisplayName("Попытка подсунуть чужой id — игнорируется, id берётся из токена")
        void update_Should_IgnoreIdFromBody_Test() throws Exception {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setId(user2Id);
            req.setName("Hacked");

            mockMvc.perform(put("/me")
                            .with(user(user1Principal))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());

            assertThat(userStorage.findById(user2Id).orElseThrow().getName()).isEqualTo("User2");
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void update_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setName("X");

            mockMvc.perform(put("/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /me — удаление своего аккаунта")
    class DeleteSelfTests {

        @Test
        @DisplayName("204, пользователь удалён")
        void deleteSelf_Should_ReturnNoContent_Test() throws Exception {
            mockMvc.perform(delete("/me")
                            .with(user(user1Principal)))
                    .andExpect(status().isNoContent());

            assertThat(userStorage.findById(user1Id)).isEmpty();
            assertThat(userStorage.findById(user2Id)).isPresent();
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void deleteSelf_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(delete("/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT/DELETE /me/friends/{friendId}")
    class FriendsManagementTests {

        @Test
        @DisplayName("PUT — 204, дружба создана")
        void addFriend_Should_ReturnNoContent_Test() throws Exception {
            mockMvc.perform(put("/me/friends/{friendId}", user2Id)
                            .with(user(user1Principal)))
                    .andExpect(status().isNoContent());

            assertThat(friendshipDbStorage.isFriend(user1Id, user2Id)).isTrue();
        }

        @Test
        @DisplayName("PUT — событие FRIEND/ADD записано")
        void addFriend_Should_AddEvent_Test() throws Exception {
            mockMvc.perform(put("/me/friends/{friendId}", user2Id)
                            .with(user(user1Principal)))
                    .andExpect(status().isNoContent());

            assertThat(eventDbStorage.getFeedUser(user1Id))
                    .filteredOn(e -> EventTypes.FRIEND.name().equals(e.getEventType()))
                    .filteredOn(e -> OperationTypes.ADD.name().equals(e.getOperation()))
                    .filteredOn(e -> e.getEntityId().equals(user2Id))
                    .hasSize(1);
        }

        @Test
        @DisplayName("PUT — несуществующий друг → 404")
        void addFriend_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(put("/me/friends/{friendId}", 999L)
                            .with(user(user1Principal)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE — 204, дружба удалена")
        void removeFriend_Should_ReturnNoContent_Test() throws Exception {
            friendshipDbStorage.addFriend(user1Id, user2Id);

            mockMvc.perform(delete("/me/friends/{friendId}", user2Id)
                            .with(user(user1Principal)))
                    .andExpect(status().isNoContent());

            assertThat(friendshipDbStorage.isFriend(user1Id, user2Id)).isFalse();
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void addFriend_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(put("/me/friends/{friendId}", user2Id))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /me/friends и /me/friends/common/{friendId}")
    class FriendsListTests {

        @Test
        @DisplayName("GET /me/friends — список своих друзей")
        void getFriends_Should_ReturnOwnFriends_Test() throws Exception {
            Long user3Id = createUser("user3@mail.com", "User3").getId();
            friendshipDbStorage.addFriend(user1Id, user2Id);
            friendshipDbStorage.addFriend(user1Id, user3Id);

            mockMvc.perform(get("/me/friends")
                            .with(user(user1Principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("GET /me/friends/common/{friendId} — общие друзья")
        void getCommonFriends_Should_ReturnCommon_Test() throws Exception {
            Long commonId = createUser("common@mail.com", "Common").getId();
            friendshipDbStorage.addFriend(user1Id, commonId);
            friendshipDbStorage.addFriend(user2Id, commonId);

            mockMvc.perform(get("/me/friends/common/{friendId}", user2Id)
                            .with(user(user1Principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(commonId));
        }

        @Test
        @DisplayName("GET /me/friends без аутентификации → 401")
        void getFriends_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(get("/me/friends"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /me/recommendations")
    class RecommendationsTests {

        @Test
        @DisplayName("200 — рекомендации для своего профиля")
        void getRecommendations_Should_ReturnOk_Test() throws Exception {
            mockMvc.perform(get("/me/recommendations")
                            .with(user(user1Principal)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void getRecommendations_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(get("/me/recommendations"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /me/feed/**")
    class FeedTests {

        @Test
        @DisplayName("GET /me/feed/user — пустой список")
        void getFeedUser_Should_ReturnEmpty_Test() throws Exception {
            mockMvc.perform(get("/me/feed/user")
                            .with(user(user1Principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("GET /me/feed/friends — пустой список")
        void getFeedFriends_Should_ReturnEmpty_Test() throws Exception {
            mockMvc.perform(get("/me/feed/friends")
                            .with(user(user1Principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("GET /me/feed/user/enriched — пустой список")
        void getEnrichedFeedUser_Should_ReturnEmpty_Test() throws Exception {
            mockMvc.perform(get("/me/feed/user/enriched")
                            .with(user(user1Principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("GET /me/feed/friends/enriched — пустой список")
        void getEnrichedFeedFriends_Should_ReturnEmpty_Test() throws Exception {
            mockMvc.perform(get("/me/feed/friends/enriched")
                            .with(user(user1Principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Без аутентификации → 401")
        void getFeedUser_Should_ReturnUnauthorized_WithoutAuth_Test() throws Exception {
            mockMvc.perform(get("/me/feed/user"))
                    .andExpect(status().isUnauthorized());
        }
    }
}