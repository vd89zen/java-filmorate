package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты UserDbStorage")
class UserDbStorageTest {
    private static final String TEST_EMAIL = "test@test.com";

    @Autowired
    private UserDbStorage storage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM users");
    }

    private User createTestUser(String email) {
        return User.builder()
                .email(email)
                .login("testLogin")
                .name("testName")
                .birthday(LocalDate.now().minusYears(17))
                .build();
    }

    @Nested
    @DisplayName("Тесты create()")
    class CreateTests {
        @Test
        @DisplayName("Создаем пользователя - получаем ID")
        void create_Should_Return_User_With_Assigned_Id_Test() {
            // given
            User user = createTestUser(TEST_EMAIL);
            // when
            User createdUser = storage.create(user);
            // then
            assertThat(createdUser).isNotNull();
            assertThat(createdUser.getId()).isNotNull();
            assertThat(createdUser)
                    .hasFieldOrPropertyWithValue("email", "test@test.com")
                    .hasFieldOrPropertyWithValue("login", "testLogin")
                    .hasFieldOrPropertyWithValue("name", "testName");

            assertThat(storage.findById(createdUser.getId()))
                    .isPresent()
                    .contains(createdUser);
        }

        @Test
        @DisplayName("Проверяем выброс исключения при создании пользователя с дублирующим email")
        void create_Should_Throw_Exception_On_Duplicate_Email_Test() {
            // given
            User user1 = createTestUser(TEST_EMAIL);
            User user2 = createTestUser(TEST_EMAIL);
            storage.create(user1);
            // when, then
            assertThatThrownBy(() -> storage.create(user2))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Тесты update()")
    class UpdateTests {
        @Test
        @DisplayName("Обновляем пользователя")
        void update_Should_Change_User_Fields_Test() {
            // given
            User user = storage.create(createTestUser(TEST_EMAIL));
            user.setName("updatedName");
            user.setLogin("updatedLogin");
            user.setEmail("updated@test.com");
            // when
            storage.update(user);
            // then
            Optional<User> updated = storage.findById(user.getId());
            assertThat(updated)
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("name", "updatedName")
                    .hasFieldOrPropertyWithValue("login", "updatedLogin")
                    .hasFieldOrPropertyWithValue("email", "updated@test.com");
        }

        @Test
        @DisplayName("Проверяем выброс исключения при обновлении несуществующего пользователя")
        void update_Should_Throw_Exception_For_Non_Exists_User_Test() {
            // given
            User nonExists = User.builder()
                    .id(666L)
                    .email("non@exist.com")
                    .login("login")
                    .name("name")
                    .birthday(LocalDate.now().minusYears(9))
                    .build();
            // when, then
            assertThatThrownBy(() -> storage.update(nonExists))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Не удалось обновить данные в БД");
        }
    }

    @Nested
    @DisplayName("Тесты findAll()")
    class FindAllTests {
        @Test
        @DisplayName("Получаем всех пользователей")
        void findAll_Should_Return_All_Users_Test() {
            // given
            User user1 = storage.create(createTestUser(TEST_EMAIL));
            User user2 = storage.create(createTestUser("other@test.com"));
            // when
            List<User> allUsers = storage.findAll();
            // then
            assertThat(allUsers)
                    .hasSize(2)
                    .containsExactlyInAnyOrder(user1, user2);
        }

        @Test
        @DisplayName("Проверяем получение пустого списка когда нет пользователей в базе")
        void findAll_Should_Return_Empty_List_When_No_Users() {
            // given, when
            List<User> allUsers = storage.findAll();
            // then
            assertThat(allUsers).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты findById()")
    class FindByIdTests {
        @Test
        @DisplayName("Получаем Optional с пользователем")
        void findById_Should_Return_Optional_With_User_Test() {
            // given
            User user = storage.create(createTestUser(TEST_EMAIL));
            // when
            Optional<User> found = storage.findById(user.getId());
            // then
            assertThat(found)
                    .isPresent()
                    .contains(user);
        }

        @Test
        @DisplayName("Проверяем получение пустого Optional по несуществующему ID")
        void findById_Should_Return_Empty_Optional_For_Non_Exists_Id_Test() {
            // given, when
            Optional<User> found = storage.findById(666L);
            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты findByEmail()")
    class FindByEmailTests {
        @Test
        @DisplayName("Получаем Optional с пользователем по существующему email")
        void findByEmail_Should_Return_Optional_With_User_Test() {
            // given
            User user = storage.create(createTestUser(TEST_EMAIL));
            // when
            Optional<User> found = storage.findByEmail(user.getEmail());
            // then
            assertThat(found)
                    .isPresent()
                    .contains(user);
        }

        @Test
        @DisplayName("Проверяем получение пустого Optional по несуществующему email")
        void findByEmail_Should_Return_Empty_Optional_For_Non_Exists_Email_Test() {
            // given, when
            Optional<User> found = storage.findByEmail("non@exist.com");
            // then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты isEmailAlreadyUse()")
    class IsEmailAlreadyUseTests {
        @Test
        @DisplayName("Проверяем что Email уже занят - получаем true")
        void isEmailAlreadyUse_Should_Return_True_For_Exists_Email_Test() {
            // given
            User user = storage.create(createTestUser(TEST_EMAIL));
            // when
            boolean isUsed = storage.isEmailAlreadyUse(user.getEmail());
            // then
            assertThat(isUsed).isTrue();
        }

        @Test
        @DisplayName("Проверяем что Email свободен - получаем false")
        void isEmailAlreadyUse_Should_Return_False_For_Free_Email_Test() {
            // when
            boolean isUsed = storage.isEmailAlreadyUse("free@test.com");
            // then
            assertThat(isUsed).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты isUserExists()")
    class IsUserExistsTests {
        @Test
        @DisplayName("Проверяем что пользователь существует — получаем true")
        void isUserExists_Should_Return_True_For_Exists_User_Test() {
            // given
            User user = storage.create(createTestUser(TEST_EMAIL));
            // when
            boolean exists = storage.isUserExists(user.getId());
            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Проверяем что пользователь не существует — получаем false")
        void isUserExists_Should_Return_False_For_Non_Exists_User_Test() {
            // given, when
            boolean exists = storage.isUserExists(666L);
            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты delete()")
    class DeleteTests {
        @Test
        @DisplayName("Удаляем существующего пользователя — получаем true")
        void delete_Should_Return_True_And_Remove_User_Test() {
            // given
            User user = storage.create(createTestUser(TEST_EMAIL));
            Long userId = user.getId();
            // when
            boolean result = storage.delete(userId);
            // then
            assertThat(result).isTrue();
            assertThat(storage.findById(userId)).isEmpty();
            assertThat(storage.isUserExists(userId)).isFalse();
        }

        @Test
        @DisplayName("Удаляем несуществующего пользователя — получаем false")
        void delete_Should_Return_False_For_Non_Exists_User_Test() {
            // given
            Long nonExistingId = 666L;
            // when
            boolean result = storage.delete(nonExistingId);
            // then
            assertThat(result).isFalse();
            assertThat(storage.findById(nonExistingId)).isEmpty();
        }

        @Test
        @DisplayName("Повторно удаляем того же пользователя — получаем false")
        void delete_Should_Return_False_On_Second_Attempt_Test() {
            // given
            User user = storage.create(createTestUser(TEST_EMAIL));
            Long userId = user.getId();
            assertThat(storage.delete(userId)).isTrue();
            // when
            boolean secondResult = storage.delete(userId);
            // then
            assertThat(secondResult).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты findBySeveralIds()")
    class FindBySeveralIdsTests {
        @Test
        @DisplayName("Получаем список пользователей по списку существующих ID")
        void findBySeveralIds_Should_Return_Users_Test() {
            // given
            User user1 = storage.create(createTestUser("user1@test.com"));
            User user2 = storage.create(createTestUser("user2@test.com"));
            User user3 = storage.create(createTestUser("user3@test.com"));
            List<Long> ids = List.of(user3.getId(), user1.getId(), user2.getId());
            // when
            List<User> result = storage.findBySeveralIds(ids);
            // then
            assertThat(result)
                    .hasSize(3)
                    .containsExactly(user1, user2, user3);
        }

        @Test
        @DisplayName("Получаем пустой список по пустым ID")
        void findBySeveralIds_Should_Return_Empty_List_For_Empty_Ids_Test() {
            // given
            List<Long> emptyIds = List.of();
            // when
            List<User> result = storage.findBySeveralIds(emptyIds);
            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Проверяем поиск с существующим и несуществующим ID — получаем только существующего")
        void findBySeveralIds_Should_Return_Only_Exists_Users_Test() {
            // given
            User existsUser = storage.create(createTestUser(TEST_EMAIL));
            Long existsId = existsUser.getId();
            Long nonExistsId = 666L;
            List<Long> mixedIds = List.of(existsId, nonExistsId);
            // when
            List<User> result = storage.findBySeveralIds(mixedIds);
            // then
            assertThat(result)
                    .hasSize(1)
                    .containsExactly(existsUser);
        }

        @Test
        @DisplayName("Получаем пустой список по несуществующим ID")
        void findBySeveralIds_Should_Return_Empty_List_For_All_Non_Exists_Ids_Test() {
            // given
            List<Long> nonExistingIds = List.of(666L, 999L, 888L);
            // when
            List<User> result = storage.findBySeveralIds(nonExistingIds);
            // then
            assertThat(result).isEmpty();
        }
    }
}
