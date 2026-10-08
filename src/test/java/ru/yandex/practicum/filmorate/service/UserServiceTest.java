package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("UserService Тесты")
class UserServiceTest {

    private final UserService userService;

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanUp();
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

    private NewUserRequest newRequest(String email, String login) {
        NewUserRequest r = new NewUserRequest();
        r.setEmail(email);
        r.setLogin(login);
        r.setName(login);
        r.setBirthday(LocalDate.of(1990, 1, 1));
        return r;
    }

    @Nested
    @DisplayName("Тесты create()")
    class CreateTests {

        @Test
        @DisplayName("Нормализует email и login к нижнему регистру")
        void create_Should_NormalizeCredentials_Test() {
            NewUserRequest req = newRequest("USER@Mail.com", "LOGIN");

            UserDto created = userService.create(req);

            assertThat(created.getEmail()).isEqualTo("user@mail.com");
            assertThat(created.getLogin()).isEqualTo("login");
        }

        @Test
        @DisplayName("Пустое имя → login")
        void create_Should_UseLogin_WhenNameBlank_Test() {
            NewUserRequest req = newRequest("user@mail.com", "userlogin");
            req.setName("");

            assertThat(userService.create(req).getName()).isEqualTo("userlogin");
        }

        @Test
        @DisplayName("Дубль email → ValidationException")
        void create_Should_Throw_OnDuplicateEmail_Test() {
            userService.create(newRequest("user@mail.com", "user1"));

            assertThatThrownBy(() -> userService.create(newRequest("user@mail.com", "user2")))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("Тесты findById() и findAll()")
    class FindTests {

        @Test
        @DisplayName("findById: возвращает DTO")
        void findById_Should_Return_Test() {
            UserDto created = userService.create(newRequest("user@mail.com", "user"));
            assertThat(userService.findById(created.getId()).getId()).isEqualTo(created.getId());
        }

        @Test
        @DisplayName("findById: несуществующий → NotFoundException")
        void findById_Should_ThrowNotFound_Test() {
            assertThatThrownBy(() -> userService.findById(999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("findAll: пагинация")
        void findAll_Should_Paginate_Test() {
            userService.create(newRequest("a@mail.com", "a"));
            userService.create(newRequest("b@mail.com", "b"));
            userService.create(newRequest("c@mail.com", "c"));

            assertThat(userService.findAll(0, 2)).hasSize(2);
            assertThat(userService.findAll(2, 2)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Тесты addFriend()/removeFriend()/getUserFriends()/getCommonFriends()")
    class FriendshipTests {

        @Test
        @DisplayName("addFriend: связывает двух пользователей")
        void addFriend_Should_Link_Test() {
            Long u1 = userService.create(newRequest("a@mail.com", "a")).getId();
            Long u2 = userService.create(newRequest("b@mail.com", "b")).getId();

            userService.addFriend(u1, u2);

            assertThat(userService.getUserFriends(u1)).hasSize(1);
        }

        @Test
        @DisplayName("addFriend: себя → ValidationException")
        void addFriend_Should_Throw_ForSelf_Test() {
            Long u1 = userService.create(newRequest("a@mail.com", "a")).getId();

            assertThatThrownBy(() -> userService.addFriend(u1, u1))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("removeFriend: удаляет связь")
        void removeFriend_Should_Unlink_Test() {
            Long u1 = userService.create(newRequest("a@mail.com", "a")).getId();
            Long u2 = userService.create(newRequest("b@mail.com", "b")).getId();

            userService.addFriend(u1, u2);
            userService.removeFriend(u1, u2);

            assertThat(userService.getUserFriends(u1)).isEmpty();
        }

        @Test
        @DisplayName("getCommonFriends: возвращает общих")
        void getCommonFriends_Should_ReturnCommon_Test() {
            Long u1 = userService.create(newRequest("a@mail.com", "a")).getId();
            Long u2 = userService.create(newRequest("b@mail.com", "b")).getId();
            Long common = userService.create(newRequest("c@mail.com", "c")).getId();

            userService.addFriend(u1, common);
            userService.addFriend(u2, common);

            assertThat(userService.getCommonFriends(u1, u2))
                    .extracting(UserDto::getId)
                    .containsExactly(common);
        }
    }

    @Nested
    @DisplayName("Тесты findShortByIds()")
    class FindShortByIdsTests {

        @Test
        @DisplayName("Возвращает карту коротких DTO")
        void findShortByIds_Should_ReturnMap_Test() {
            Long u1 = userService.create(newRequest("a@mail.com", "a")).getId();
            Long u2 = userService.create(newRequest("b@mail.com", "b")).getId();

            Map<Long, UserShortDto> result = userService.findShortByIds(Set.of(u1, u2));

            assertThat(result).hasSize(2).containsKeys(u1, u2);
        }

        @Test
        @DisplayName("Пустой set → пустая карта")
        void findShortByIds_Should_ReturnEmpty_Test() {
            assertThat(userService.findShortByIds(Set.of())).isEmpty();
        }
    }
}