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
import ru.yandex.practicum.filmorate.model.Film;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FilmorateApplication.class)
@ActiveProfiles("test")
@DisplayName("Тесты FilmController")
class FilmControllerTest {

    private FilmController filmController;

    @Autowired
    private Validator validator;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
    }

    @Nested
    @DisplayName("Тесты метода create")
    class testsForCreateFilm {
        @Test
        @DisplayName("Проверяем создание валидного фильма")
        void create_Valid_Film_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .build();
            //When
            Film createdFilm = filmController.create(film);
            //Then
            assertNotNull(createdFilm.getId());
            assertEquals(film.getName(), createdFilm.getName());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при попытке создать фильм с заданным id")
        void create_Film_With_Existing_Id_Test() {
            //Given
            Film film = Film.builder()
                    .id(666L)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .build();
            //When, Then
            assertThrows(ValidationException.class, () -> {
                filmController.create(film);
            });
        }

        @Test
        @DisplayName("Проверяем невозможность создать фильм с невалидным (менее 2 символов) описанием (description)")
        void create_Film_With_Small_Description_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("t")
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertTrue(!violations.isEmpty(), "Ожидались ошибки валидации.");
            assertEquals(1, violations.size(), "Ожидалась одна ошибка валидации.");
            ConstraintViolation<Film> violation = violations.iterator().next();
            assertEquals("description", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("Описание должно быть от 2 до 200 символов."));
        }

        @Test
        @DisplayName("Проверяем невозможность создать фильм с невалидным (более 200 символов) описанием (description)")
        void create_Film_With_Big_Description_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("t".repeat(404))
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertTrue(!violations.isEmpty(), "Ожидались ошибки валидации.");
            assertEquals(1, violations.size(), "Ожидалась одна ошибка валидации.");
            ConstraintViolation<Film> violation = violations.iterator().next();
            assertEquals("description", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("Описание должно быть от 2 до 200 символов."));
        }

        @Test
        @DisplayName("Проверяем невозможность создать фильм не указав название (name)")
        void create_Film_With_Empty_Name_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name(null)
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertTrue(!violations.isEmpty(), "Ожидались ошибки валидации.");
            assertEquals(1, violations.size(), "Ожидалась одна ошибка валидации.");
            ConstraintViolation<Film> violation = violations.iterator().next();
            assertEquals("name", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("Не указано название фильма."));
        }

        @Test
        @DisplayName("Проверяем выброс исключения при попытке создать фильм с датой релиза (releaseDate) ранее дня рождения кино (28.12.1895)")
        void create_Film_With_Release_Date_Before_Movie_Birthday_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.of(1894, 11, 27))
                    .duration(60)
                    .build();
            //When, Then
            assertThrows(ValidationException.class, () -> {
                filmController.create(film);
            });
        }

        @Test
        @DisplayName("Проверяем невозможность создать фильм не указав дату релиза (releaseDate)")
        void create_Film_With_Empty_Release_Date_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(null)
                    .duration(60)
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertTrue(!violations.isEmpty(), "Ожидались ошибки валидации.");
            assertEquals(1, violations.size(), "Ожидалась одна ошибка валидации.");
            ConstraintViolation<Film> violation = violations.iterator().next();
            assertEquals("releaseDate", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("Не указана дата релиза фильма."));
        }

        @Test
        @DisplayName("Проверяем невозможность создать фильм c отрицательной длительностью (duration)")
        void create_Film_With_Negative_Duration_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(5))
                    .duration(-80)
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertTrue(!violations.isEmpty(), "Ожидались ошибки валидации.");
            assertEquals(1, violations.size(), "Ожидалась одна ошибка валидации.");
            ConstraintViolation<Film> violation = violations.iterator().next();
            assertEquals("duration", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("Длительность фильма должна быть положительным числом."));
        }

        @Test
        @DisplayName("Проверяем невозможность создать фильм c нулевой длительностью (duration)")
        void create_Film_With_Zero_Duration_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(5))
                    .duration(0)
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertTrue(!violations.isEmpty(), "Ожидались ошибки валидации.");
            assertEquals(1, violations.size(), "Ожидалась одна ошибка валидации.");
            ConstraintViolation<Film> violation = violations.iterator().next();
            assertEquals("duration", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("Длительность фильма должна быть положительным числом."));
        }
    }

    @Nested
    @DisplayName("Тесты метода update")
    class testsForUpdateFilm {
        @Test
        @DisplayName("Проверяем обновление валидного фильма валидными данными")
        void update_Valid_Film_Valid_Data_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .build();

            Film createdFilm = filmController.create(film);
            String expectedName = "updatedName";
            //When
            createdFilm.setName(expectedName);
            Film actualFilm = filmController.update(createdFilm);
            //Then
            assertEquals(expectedName, actualFilm.getName());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при попытке обновить фильм не указав id")
        void update_Film_Without_Id_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .build();
            //When, Then
            assertThrows(ValidationException.class, () -> {
                filmController.update(film);
            });
        }

        @Test
        @DisplayName("Проверяем выброс исключения при попытке обновить несуществующий фильм")
        void update_Non_Existing_Film_Test() {
            //Given
            Film film = Film.builder()
                    .id(666L)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .build();
            //When, Then
            assertThrows(NotFoundException.class, () -> {
                filmController.update(film);
            });
        }
    }
}