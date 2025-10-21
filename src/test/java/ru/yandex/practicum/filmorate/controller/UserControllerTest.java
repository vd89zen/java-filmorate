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
import java.time.LocalDate;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FilmorateApplication.class)
@ActiveProfiles("test")
@DisplayName("Тесты UserController")
class UserControllerTest {

    @Autowired
    private Validator validator;

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Nested
    @DisplayName("Тесты метода create")
    class testsForCreateUser {
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
    class testsForUpdateUser {
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
                    .build();
            //When, Then
            assertThrows(NotFoundException.class, () -> {
                userController.update(user);
            });
        }
    }
}