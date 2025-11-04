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
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = FilmorateApplication.class)
@ActiveProfiles("test")
@DisplayName("Тесты FilmController")
class FilmControllerTest {

    @Autowired
    private Validator validator;
    private FilmStorage filmStorage;
    private UserStorage userStorage;
    private FilmService filmService;
    private UserService userService;
    private FilmController filmController;
    private UserController userController;

    @BeforeEach
    void setUp() {
        filmStorage = new InMemoryFilmStorage();
        userStorage = new InMemoryUserStorage();
        filmService = new FilmService(filmStorage, userStorage);
        userService = new UserService(userStorage);
        filmController = new FilmController(filmService);
        userController = new UserController(userService);
    }

    @Nested
    @DisplayName("Тесты метода create")
    class TestsForCreateFilm {
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
                    .likes(new HashSet<>())
                    .build();
            //When
            Film createdFilm = filmController.create(film).getBody();
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
                    .likes(new HashSet<>())
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
                    .likes(new HashSet<>())
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertFalse(violations.isEmpty(), "Ожидались ошибки валидации.");
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
                    .likes(new HashSet<>())
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertFalse(violations.isEmpty(), "Ожидались ошибки валидации.");
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
                    .likes(new HashSet<>())
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertFalse(violations.isEmpty(), "Ожидались ошибки валидации.");
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
                    .likes(new HashSet<>())
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
                    .likes(new HashSet<>())
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertFalse(violations.isEmpty(), "Ожидались ошибки валидации.");
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
                    .likes(new HashSet<>())
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertFalse(violations.isEmpty(), "Ожидались ошибки валидации.");
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
                    .likes(new HashSet<>())
                    .build();
            //When
            Set<ConstraintViolation<Film>> violations = validator.validate(film);
            //Then
            assertFalse(violations.isEmpty(), "Ожидались ошибки валидации.");
            assertEquals(1, violations.size(), "Ожидалась одна ошибка валидации.");
            ConstraintViolation<Film> violation = violations.iterator().next();
            assertEquals("duration", violation.getPropertyPath().toString());
            assertTrue(violation.getMessage().contains("Длительность фильма должна быть положительным числом."));
        }
    }

    @Nested
    @DisplayName("Тесты метода update")
    class TestsForUpdateFilm {
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
                    .likes(new HashSet<>())
                    .build();

            Film createdFilm = filmController.create(film).getBody();
            String expectedName = "updatedName";
            //When
            createdFilm.setName(expectedName);
            Film actualFilm = filmController.update(createdFilm).getBody();
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
                    .likes(new HashSet<>())
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
                    .likes(new HashSet<>())
                    .build();
            //When, Then
            assertThrows(NotFoundException.class, () -> {
                filmController.update(film);
            });
        }
    }

    @Nested
    @DisplayName("Тесты метода findById")
    class TestsForFindById {
        @Test
        @DisplayName("Проверяем получение существующего фильма по ID")
        void findById_Existing_Film_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .likes(new HashSet<>())
                    .build();
            Film createdFilm = filmController.create(film).getBody();
            long filmId = createdFilm.getId();
            //When
            Film foundFilm = filmController.findById(filmId).getBody();
            //Then
            assertNotNull(foundFilm);
            assertEquals(filmId, foundFilm.getId());
            assertEquals(createdFilm.getName(), foundFilm.getName());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при поиске несуществующего фильма")
        void findById_Non_Existing_Film_Test() {
            //Given
            long nonExistingId = 666L;
            //When, Then
            assertThrows(NotFoundException.class, () -> {
                filmController.findById(nonExistingId);
            });
        }
    }

    @Nested
    @DisplayName("Тесты метода findAll")
    class TestsForFindAll {
        @Test
        @DisplayName("Проверяем получение списка всех фильмов (не пустой)")
        void findAll_Not_Empty_Test() {
            //Given
            Film film1 = Film.builder()
                    .id(null)
                    .name("Film1")
                    .description("Desc1")
                    .releaseDate(LocalDate.now().minusYears(10))
                    .duration(90)
                    .likes(new HashSet<>())
                    .build();
            Film film2 = Film.builder()
                    .id(null)
                    .name("Film2")
                    .description("Desc2")
                    .releaseDate(LocalDate.now().minusYears(5))
                    .duration(120)
                    .likes(new HashSet<>())
                    .build();
            filmController.create(film1);
            filmController.create(film2);
            //When
            Collection<Film> films = filmController.findAll().getBody();
            //Then
            assertNotNull(films);
            assertEquals(2, films.size());
            assertTrue(films.stream().anyMatch(f -> f.getName().equals("Film1")));
            assertTrue(films.stream().anyMatch(f -> f.getName().equals("Film2")));
        }

        @Test
        @DisplayName("Проверяем получение пустого списка при отсутствии фильмов")
        void findAll_Empty_Test() {
            //Given, When
            Collection<Film> films = filmController.findAll().getBody();
            //Then
            assertNotNull(films);
            assertTrue(films.isEmpty());
        }
    }

    @Nested
    @DisplayName("Тесты метода addLike")
    class TestsForAddLike {
        @Test
        @DisplayName("Проверяем добавление лайка к существующему фильму")
        void addLike_To_Existing_Film_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .likes(new HashSet<>())
                    .build();
            Film createdFilm = filmController.create(film).getBody();
            long filmId = createdFilm.getId();

            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            User createdUser = userController.create(user).getBody();
            long userId = createdUser.getId();
            //When
            filmController.addLike(filmId, userId);
            //Then
            Film updatedFilm = filmController.findById(filmId).getBody();
            assertTrue(updatedFilm.getLikes().contains(userId));
            assertEquals(1, updatedFilm.getLikes().size());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при добавлении лайка к несуществующему фильму")
        void addLike_To_Non_Existing_Film_Test() {
            //Given
            User user = User.builder()
                    .id(null)
                    .email("test@email.com")
                    .login("testLogin")
                    .name("TestName")
                    .birthday(LocalDate.now().minusYears(26))
                    .friends(new HashSet<>())
                    .build();
            User createdUser = userController.create(user).getBody();
            long userId = createdUser.getId();
            long nonExistingFilmId = 666L;
            //When, Then
            assertThrows(NotFoundException.class, () -> {
                filmController.addLike(nonExistingFilmId, userId);
            });
        }
    }

    @Nested
    @DisplayName("Тесты метода removeLike")
    class TestsForRemoveLike {
        @Test
        @DisplayName("Проверяем удаление лайка у существующего фильма")
        void removeLike_From_Existing_Film_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .likes(Set.of(101L, 102L))
                    .build();
            Film createdFilm = filmController.create(film).getBody();
            long filmId = createdFilm.getId();
            long userId = 101L;
            //When
            filmController.removeLike(filmId, userId);
            //Then
            Film updatedFilm = filmController.findById(filmId).getBody();
            assertFalse(updatedFilm.getLikes().contains(userId));
            assertEquals(1, updatedFilm.getLikes().size());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при удалении лайка из несуществующего фильма")
        void removeLike_From_Non_Existing_Film_Test() {
            //Given
            long nonExistingFilmId = 666L;
            long userId = 101L;
            //When, Then
            assertThrows(NotFoundException.class, () -> {
                filmController.removeLike(nonExistingFilmId, userId);
            });
        }

        @Test
        @DisplayName("Проверяем выброс исключения при удалении несуществующего лайка у существующего фильма")
        void removeLike_Non_Existing_Like_Test() {
            //Given
            Film film = Film.builder()
                    .id(null)
                    .name("testName")
                    .description("testDescription")
                    .releaseDate(LocalDate.now().minusYears(26))
                    .duration(60)
                    .likes(Set.of(101L, 102L))
                    .build();
            Film createdFilm = filmController.create(film).getBody();
            long filmId = createdFilm.getId();
            long nonExistingUserId = 999L;
            //When, Then
            assertThrows(NotFoundException.class, () -> {
                filmController.removeLike(filmId, nonExistingUserId);
            });
        }
    }

    @Nested
    @DisplayName("Тесты метода getMostPopularFilms")
    class TestsForGetMostPopularFilms {
        @Test
        @DisplayName("Проверяем получение популярных фильмов с корректным параметром count")
        void getMostPopularFilms_With_Valid_Count_Test() {
            // Given
            Film film1 = Film.builder()
                    .name("Popular Film 1")
                    .description("Description 1")
                    .releaseDate(LocalDate.now().minusYears(5))
                    .duration(120)
                    .likes(Set.of(1L, 2L, 3L))
                    .build();
            Film film2 = Film.builder()
                    .name("Popular Film 2")
                    .description("Description 2")
                    .releaseDate(LocalDate.now().minusYears(3))
                    .duration(90)
                    .likes(Set.of(1L, 2L))
                    .build();
            Film film3 = Film.builder()
                    .name("Popular Film 3")
                    .description("Description 3")
                    .releaseDate(LocalDate.now().minusYears(1))
                    .duration(100)
                    .likes(Set.of(1L))
                    .build();
            filmController.create(film1);
            filmController.create(film2);
            filmController.create(film3);
            int count = 2;
            // When
            List<Film> popularFilms = filmController.getMostPopularFilms(count).getBody();
            // Then
            assertEquals(count, popularFilms.size());
            // Проверяем, что фильмы отсортированы по количеству лайков (от большего к меньшему)
            assertTrue(popularFilms.get(0).getLikes().size() >= popularFilms.get(1).getLikes().size());
        }

        @Test
        @DisplayName("Проверяем выброс исключения при попытке получения популярных фильмов с count = 0")
        void getMostPopularFilms_With_Zero_Count_Test() {
            // Given
            int count = 0;
            //When, Then
            assertThrows(ValidationException.class, () -> {
                filmController.getMostPopularFilms(count);
            });
        }

        @Test
        @DisplayName("Проверяем выброс исключения при попытке получения популярных фильмов с отрицательным count")
        void getMostPopularFilms_WithNegativeCount_Test() {
            // Given
            int count = -5;
            // When, Then
            assertThrows(ValidationException.class, () -> {
                filmController.getMostPopularFilms(count);
            });
        }

        @Test
        @DisplayName("Проверяем получение популярных фильмов когда нет фильмов в системе")
        void getMostPopularFilms_WhenNoFilmsExist_Test() {
            // Given
            int count = 10;

            // When
            List<Film> popularFilms = filmController.getMostPopularFilms(count).getBody();

            // Then
            assertTrue(popularFilms.isEmpty());
        }

        @Test
        @DisplayName("Проверяем получение популярных фильмов когда все фильмы имеют одинаковое количество лайков")
        void getMostPopularFilms_WithEqualLikes_Test() {
            // Given
            int count = 3;
            Film film1 = Film.builder()
                    .name("Film 1")
                    .description("Description 1")
                    .releaseDate(LocalDate.now().minusYears(5))
                    .duration(120)
                    .likes(Set.of(1L))
                    .build();
            Film film2 = Film.builder()
                    .name("Film 2")
                    .description("Description 2")
                    .releaseDate(LocalDate.now().minusYears(3))
                    .duration(90)
                    .likes(Set.of(2L))
                    .build();
            Film film3 = Film.builder()
                    .name("Film 3")
                    .description("Description 3")
                    .releaseDate(LocalDate.now().minusYears(1))
                    .duration(100)
                    .likes(Set.of(3L))
                    .build();
            filmController.create(film1);
            filmController.create(film2);
            filmController.create(film3);
            // When
            List<Film> popularFilms = filmController.getMostPopularFilms(count).getBody();
            // Then
            assertEquals(3, popularFilms.size());
            Set<String> filmNames = popularFilms.stream()
                    .map(Film::getName)
                    .collect(Collectors.toSet());
            assertTrue(filmNames.contains("Film 1"));
            assertTrue(filmNames.contains("Film 2"));
            assertTrue(filmNames.contains("Film 3"));
        }
    }

}