package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.filmorate.FilmorateApplication;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FilmorateApplication.class)
@ActiveProfiles("test")
@DisplayName("Тесты UserController")
class UserControllerTest {

    @Autowired
    private Validator validator;
    private UserStorage userStorage;
    private UserService userService;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userStorage = new InMemoryUserStorage();
        userService = new UserService(userStorage);
        userController = new UserController(userService);
    }

    @Nested
    @DisplayName("Тесты метода create")
    class TestsForCreateUser {
        @Test
        @DisplayName("Проверяем создание валидного пользователя")
        void create_Valid_User_Test() {
            //Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            //When
            User createdUser = userController.create(user);
            //Then
            assertNotNull(createdUser.getId());
            assertEquals(user.getEmail(), createdUser.getEmail());
        }

        @Test
        @DisplayName("Проверяем выброс ошибки при попытке создать пользователя с заданным id")
        void create_User_With_Existing_Id_Test() {
            //Given
            User user = User.builder()
                    .id(666L)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            //When, Then
            assertThrows(ValidationException.class, () -> {
                userController.create(user);
            });
        }

        @Test
        @DisplayName("Проверяем невозможность создать пользователя с невалидным email")
        void create_User_With_Invalid_Email_Test() {
            //Given
            User user = User.builder()
                    .id(null)
                    .email("invalid-email")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            //When
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            //Then
            assertFalse(violations.isEmpty(), "Ожидались ошибки валидации.");
            assertEquals(1, violations.size(), "Ожидалась одна ошибка валидации.");
            ConstraintViolation<User> violation = violations.iterator().next();
            assertEquals("email", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("Неверный формат адреса электронной почты."));
        }

        @Test
        @DisplayName("Проверяем невозможность создать пользователя не указав login")
        void create_User_With_Empty_Login_Test() {
            //Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login(null)
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            //When
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            //Then
            assertFalse(violations.isEmpty(), "Ожидались ошибки валидации.");
            assertEquals(1, violations.size(), "Ожидалась одна ошибка валидации.");
            ConstraintViolation<User> violation = violations.iterator().next();
            assertEquals("login", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("Не указан логин (login)."));
        }

        @Test
        @DisplayName("Проверяем невозможность создать пользователя с датой рождения позже текущей даты")
        void create_User_With_Future_Birthday_Test() {
            //Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().plusYears(8))
                    .friends(new HashSet<>())
                    .build();
            //When
            Set<ConstraintViolation<User>> violations = validator.validate(user);
            //Then
            assertFalse(violations.isEmpty(), "Ожидались ошибки валидации.");
            assertEquals(1, violations.size(), "Ожидалась одна ошибка валидации.");
            ConstraintViolation<User> violation = violations.iterator().next();
            assertEquals("birthday", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("Дата рождения не может быть в будущем."));
        }

        @Test
        @DisplayName("Проверяем создание пользователя: если имя (name) не указано, должно использоваться значение login")
        void create_Valid_User_With_Empty_Name_Test() {
            //Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name(null)
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            //When
            User createdUser = userController.create(user);
            //Then
            assertNotNull(createdUser.getName());
            assertEquals(createdUser.getName(), createdUser.getLogin());
        }
    }

    @Nested
    @DisplayName("Тесты метода update")
    class TestsForUpdateUser {
        @Test
        @DisplayName("Проверяем обновление валидного пользователя валидными данными")
        void update_Valid_User_Valid_Data_Test() {
            //Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();

            User createdUser = userController.create(user);
            String expectedName = "updatedName";
            //When
            createdUser.setName(expectedName);
            User actualUser = userController.update(createdUser);
            //Then
            assertEquals(expectedName, actualUser.getName());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при попытке обновить пользователя не указав id")
        void update_User_Without_Id_Test() {
            //Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            //When, Then
            assertThrows(ValidationException.class, () -> {
                userController.update(user);
            });
        }

        @Test
        @DisplayName("Проверяем выброс исключения при попытке обновить несуществующего пользователя")
        void update_Non_Existing_User_Test() {
            //Given
            User user = User.builder()
                    .id(666L)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            //When, Then
            assertThrows(NotFoundException.class, () -> {
                userController.update(user);
            });
        }
    }

    @Nested
    @DisplayName("Тесты метода findById")
    class TestsForFindById {
        @Test
        @DisplayName("Проверяем получение существующего пользователя по ID")
        void findById_Existing_User_Test() {
            // Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            User createdUser = userController.create(user);
            long userId = createdUser.getId();
            // When
            User foundUser = userController.findById(userId);
            // Then
            assertNotNull(foundUser);
            assertEquals(userId, foundUser.getId());
            assertEquals(user.getEmail(), foundUser.getEmail());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при поиске несуществующего пользователя")
        void findById_Non_Existing_User_Test() {
            // Given
            long nonExistingId = 666L;
            // When, Then
            assertThrows(NotFoundException.class, () -> {
                userController.findById(nonExistingId);
            });
        }
    }

    @Nested
    @DisplayName("Тесты метода findAll")
    class TestsForFindAll {
        @Test
        @DisplayName("Проверяем получение списка всех пользователей")
        void findAll_Users_Test() {
            // Given
            User user1 = User.builder()
                    .id(null)
                    .email("user1@email.com")
                    .login("user1Login")
                    .name("User1Name")
                    .birthday(LocalDate.now().minusYears(20))
                    .friends(new HashSet<>())
                    .build();
            User user2 = User.builder()
                    .id(null)
                    .email("user2@email.com")
                    .login("user2Login")
                    .name("User2Name")
                    .birthday(LocalDate.now().minusYears(25))
                    .friends(new HashSet<>())
                    .build();

            userController.create(user1);
            userController.create(user2);
            // When
            Collection<User> allUsers = userController.findAll();
            // Then
            assertNotNull(allUsers);
            assertEquals(2, allUsers.size());
            assertTrue(allUsers.stream().anyMatch(u -> u.getEmail().equals("user1@email.com")));
            assertTrue(allUsers.stream().anyMatch(u -> u.getEmail().equals("user2@email.com")));
        }
    }

    @Nested
    @DisplayName("Тесты метода addFriend")
    class TestsForAddFriend {
        @Test
        @DisplayName("Проверяем добавление друга для существующего пользователя")
        void addFriend_Valid_Users_Test() {
            // Given
            User user1 = User.builder()
                    .id(null)
                    .email("user1@email.com")
                    .login("user1Login")
                    .name("User1Name")
                    .birthday(LocalDate.now().minusYears(20))
                    .friends(new HashSet<>())
                    .build();
            User user2 = User.builder()
                    .id(null)
                    .email("user2@email.com")
                    .login("user2Login")
                    .name("User2Name")
                    .birthday(LocalDate.now().minusYears(25))
                    .friends(new HashSet<>())
                    .build();

            User createdUser1 = userController.create(user1);
            User createdUser2 = userController.create(user2);
            long userId1 = createdUser1.getId();
            long userId2 = createdUser2.getId();
            // When
            userController.addFriend(userId1, userId2);
            // Then
            List<User> friendsOfUser1 = userController.getFriends(userId1);
            List<User> friendsOfUser2 = userController.getFriends(userId2);

            assertEquals(1, friendsOfUser1.size());
            assertEquals(userId2, friendsOfUser1.get(0).getId());
            assertEquals(1, friendsOfUser2.size());
            assertEquals(userId1, friendsOfUser2.get(0).getId());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при попытке добавить себя в друзья")
        void addFriend_Same_User_Test() {
            // Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            User createdUser = userController.create(user);
            long userId = createdUser.getId();
            // When, Then
            assertThrows(ValidationException.class, () -> {
                userController.addFriend(userId, userId);
            });
        }

        @Test
        @DisplayName("Проверяем выброс исключения при добавлении друга для несуществующего пользователя")
        void addFriend_Non_Existing_User_Test() {
            // Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            User createdUser = userController.create(user);
            long existingUserId = createdUser.getId();
            long nonExistingUserId = 666L;
            // When, Then
            assertThrows(NotFoundException.class, () -> {
                userController.addFriend(nonExistingUserId, existingUserId);
            });
        }
    }

    @Nested
    @DisplayName("Тесты метода removeFriend")
    class TestsForRemoveFriend {
        @Test
        @DisplayName("Проверяем удаление друга у существующего пользователя")
        void removeFriend_Valid_Users_Test() {
            // Given
            User user1 = User.builder()
                    .id(null)
                    .email("user1@email.com")
                    .login("user1Login")
                    .name("User1Name")
                    .birthday(LocalDate.now().minusYears(20))
                    .friends(new HashSet<>())
                    .build();
            User user2 = User.builder()
                    .id(null)
                    .email("user2@email.com")
                    .login("user2Login")
                    .name("User2Name")
                    .birthday(LocalDate.now().minusYears(25))
                    .friends(new HashSet<>())
                    .build();

            User createdUser1 = userController.create(user1);
            User createdUser2 = userController.create(user2);
            long userId1 = createdUser1.getId();
            long userId2 = createdUser2.getId();
            userController.addFriend(userId1, userId2);
            // When
            userController.removeFriend(userId1, userId2);
            // Then
            List<User> friendsOfUser1 = userController.getFriends(userId1);
            List<User> friendsOfUser2 = userController.getFriends(userId2);
            assertTrue(friendsOfUser1.isEmpty());
            assertTrue(friendsOfUser2.isEmpty());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при попытке удалить себя из своих друзей")
        void removeFriend_Same_User_Test() {
            // Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            User createdUser = userController.create(user);
            long userId = createdUser.getId();
            // When, Then
            assertThrows(ValidationException.class, () -> {
                userController.removeFriend(userId, userId);
            });
        }

        @Test
        @DisplayName("Проверяем выброс исключения при удалении друга у несуществующего пользователя")
        void removeFriend_Non_Existing_User_Test() {
            // Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            User createdUser = userController.create(user);
            long existingUserId = createdUser.getId();
            long nonExistingUserId = 666L;
            // When, Then
            assertThrows(NotFoundException.class, () -> {
                userController.removeFriend(nonExistingUserId, existingUserId);
            });
        }
    }

    @Nested
    @DisplayName("Тесты метода getFriends")
    class TestsForGetFriends {
        @Test
        @DisplayName("Проверяем получение списка друзей для пользователя с друзьями")
        void getFriends_User_With_Friends_Test() {
            // Given
            User user1 = User.builder()
                    .id(null)
                    .email("user1@email.com")
                    .login("user1Login")
                    .name("User1Name")
                    .birthday(LocalDate.now().minusYears(20))
                    .friends(new HashSet<>())
                    .build();
            User user2 = User.builder()
                    .id(null)
                    .email("user2@email.com")
                    .login("user2Login")
                    .name("User2Name")
                    .birthday(LocalDate.now().minusYears(25))
                    .friends(new HashSet<>())
                    .build();
            User user3 = User.builder()
                    .id(null)
                    .email("user3@email.com")
                    .login("user3Login")
                    .name("User3Name")
                    .birthday(LocalDate.now().minusYears(30))
                    .friends(new HashSet<>())
                    .build();

            User createdUser1 = userController.create(user1);
            User createdUser2 = userController.create(user2);
            User createdUser3 = userController.create(user3);
            long userId1 = createdUser1.getId();
            long userId2 = createdUser2.getId();
            long userId3 = createdUser3.getId();
            userController.addFriend(userId1, userId2);
            userController.addFriend(userId1, userId3);
            // When
            List<User> friends = userController.getFriends(userId1);
            // Then
            assertNotNull(friends);
            assertEquals(2, friends.size());
            assertTrue(friends.stream().anyMatch(u -> u.getId().equals(userId2)));
            assertTrue(friends.stream().anyMatch(u -> u.getId().equals(userId3)));
        }

        @Test
        @DisplayName("Проверяем получение пустого списка друзей для пользователя без друзей")
        void getFriends_User_Without_Friends_Test() {
            // Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            User createdUser = userController.create(user);
            long userId = createdUser.getId();
            // When
            List<User> friends = userController.getFriends(userId);
            // Then
            assertNotNull(friends);
            assertTrue(friends.isEmpty());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при получении друзей для несуществующего пользователя")
        void getFriends_Non_Existing_User_Test() {
            // Given
            long nonExistingUserId = 666L;
            // When, Then
            assertThrows(NotFoundException.class, () -> {
                userController.getFriends(nonExistingUserId);
            });
        }
    }

    @Nested
    @DisplayName("Тесты метода getCommonFriends")
    class TestsForGetCommonFriends {
        @Test
        @DisplayName("Проверяем получение общих друзей двух пользователей")
        void getCommonFriends_Users_With_Common_Friends_Test() {
            // Given
            User user1 = User.builder()
                    .id(null)
                    .email("user1@email.com")
                    .login("user1Login")
                    .name("User1Name")
                    .birthday(LocalDate.now().minusYears(20))
                    .friends(new HashSet<>())
                    .build();
            User user2 = User.builder()
                    .id(null)
                    .email("user2@email.com")
                    .login("user2Login")
                    .name("User2Name")
                    .birthday(LocalDate.now().minusYears(25))
                    .friends(new HashSet<>())
                    .build();
            User commonFriend = User.builder()
                    .id(null)
                    .email("common@email.com")
                    .login("commonLogin")
                    .name("CommonName")
                    .birthday(LocalDate.now().minusYears(22))
                    .friends(new HashSet<>())
                    .build();

            User createdUser1 = userController.create(user1);
            User createdUser2 = userController.create(user2);
            User createdCommonFriend = userController.create(commonFriend);
            long userId1 = createdUser1.getId();
            long userId2 = createdUser2.getId();
            long commonFriendId = createdCommonFriend.getId();
            userController.addFriend(userId1, commonFriendId);
            userController.addFriend(userId2, commonFriendId);
            // When
            List<User> commonFriends = userController.getCommonFriends(userId1, userId2);
            // Then
            assertNotNull(commonFriends);
            assertEquals(1, commonFriends.size());
            assertEquals(commonFriendId, commonFriends.get(0).getId());
        }

        @Test
        @DisplayName("Проверяем получение пустого списка общих друзей, если общих друзей нет")
        void getCommonFriends_No_Common_Friends_Test() {
            // Given
            User user1 = User.builder()
                    .id(null)
                    .email("user1@email.com")
                    .login("user1Login")
                    .name("User1Name")
                    .birthday(LocalDate.now().minusYears(20))
                    .friends(new HashSet<>())
                    .build();
            User user2 = User.builder()
                    .id(null)
                    .email("user2@email.com")
                    .login("user2Login")
                    .name("User2Name")
                    .birthday(LocalDate.now().minusYears(25))
                    .friends(new HashSet<>())
                    .build();
            User friend1 = User.builder()
                    .id(null)
                    .email("friend1@email.com")
                    .login("friend1Login")
                    .name("Friend1Name")
                    .birthday(LocalDate.now().minusYears(23))
                    .friends(new HashSet<>())
                    .build();
            User friend2 = User.builder()
                    .id(null)
                    .email("friend2@email.com")
                    .login("friend2Login")
                    .name("Friend2Name")
                    .birthday(LocalDate.now().minusYears(24))
                    .friends(new HashSet<>())
                    .build();

            User createdUser1 = userController.create(user1);
            User createdUser2 = userController.create(user2);
            User createdFriend1 = userController.create(friend1);
            User createdFriend2 = userController.create(friend2);
            long userId1 = createdUser1.getId();
            long userId2 = createdUser2.getId();
            long friend1Id = createdFriend1.getId();
            long friend2Id = createdFriend2.getId();
            userController.addFriend(userId1, friend1Id);
            userController.addFriend(userId2, friend2Id);
            // When
            List<User> commonFriends = userController.getCommonFriends(userId1, userId2);
            // Then
            assertNotNull(commonFriends);
            assertTrue(commonFriends.isEmpty());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при поиске общих друзей для несуществующего пользователя")
        void getCommonFriends_First_User_Non_Existing_Test() {
            // Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            User createdUser = userController.create(user);
            long existingUserId = createdUser.getId();
            long nonExistingUserId = 666L;
            // When, Then
            assertThrows(NotFoundException.class, () -> {
                userController.getCommonFriends(nonExistingUserId, existingUserId);
            });
        }
    }
}