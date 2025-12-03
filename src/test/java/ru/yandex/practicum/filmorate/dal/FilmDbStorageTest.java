package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.RatingMpaa;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты FilmDbStorage")
class FilmDbStorageTest {
    private final FilmDbStorage filmStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM films");
    }

    @Nested
    @DisplayName("Тесты метода create()")
    class CreateTests {
        @Test
        @DisplayName("Создание фильма: должен вставить запись и вернуть объект с ID")
        void create_Should_Insert_Film_And_Return_With_Id_Test() {
            // given
            Film newFilm = Film.builder()
                    .name("Inception")
                    .description("A mind-bending thriller.")
                    .releaseDate(LocalDate.of(2010, 7, 16))
                    .duration(148)
                    .ratingMpaa(new RatingMpaa(3L, "PG-13"))
                    .build();
            // when
            Film createdFilm = filmStorage.create(newFilm);
            // then
            assertThat(createdFilm).isNotNull();
            assertThat(createdFilm.getId()).isNotNull();
            assertThat(createdFilm)
                    .hasFieldOrPropertyWithValue("name", "Inception")
                    .hasFieldOrPropertyWithValue("description", "A mind-bending thriller.")
                    .hasFieldOrPropertyWithValue("releaseDate", LocalDate.of(2010, 7, 16))
                    .hasFieldOrPropertyWithValue("duration", 148);
            assertThat(filmStorage.isFilmExists(createdFilm.getId())).isTrue();
        }
    }

    @Nested
    @DisplayName("Тесты метода update()")
    class UpdateTests {
        @Test
        @DisplayName("Обновление фильма: должен изменить существующие данные")
        void update_Should_Update_Existing_Film_Test() {
            // given
            Film originalFilm = Film.builder()
                    .name("The Matrix")
                    .description("Sci-fi classic.")
                    .releaseDate(LocalDate.of(1999, 3, 31))
                    .duration(136)
                    .ratingMpaa(new RatingMpaa(2L, "R"))
                    .build();
            Film savedFilm = filmStorage.create(originalFilm);
            Film updatedFilm = Film.builder()
                    .id(savedFilm.getId())
                    .name(savedFilm.getName())
                    .description(savedFilm.getDescription())
                    .releaseDate(savedFilm.getReleaseDate())
                    .duration(savedFilm.getDuration())
                    .ratingMpaa(savedFilm.getRatingMpaa())
                    .build();

            updatedFilm.setName("The Matrix: Reloaded");
            updatedFilm.setDescription("Sequel to the original.");
            updatedFilm.setDuration(138);
            // when
            filmStorage.update(updatedFilm);
            // then
            Optional<Film> retrievedFilm = filmStorage.findById(savedFilm.getId());
            assertThat(retrievedFilm)
                    .isPresent()
                    .hasValueSatisfying(film -> {
                        assertThat(film)
                                .hasFieldOrPropertyWithValue("name", "The Matrix: Reloaded")
                                .hasFieldOrPropertyWithValue("description", "Sequel to the original.")
                                .hasFieldOrPropertyWithValue("duration", 138);
                    });
        }
    }

    @Nested
    @DisplayName("Тесты метода findById()")
    class FindByIdTests {
        @Test
        @DisplayName("Поиск по ID: должен вернуть фильм, если он существует")
        void findById_Should_Return_Film_When_Exists_Test() {
            // given
            Film film = Film.builder()
                    .name("Interstellar")
                    .description("Space adventure.")
                    .releaseDate(LocalDate.of(2014, 11, 7))
                    .duration(169)
                    .ratingMpaa(new RatingMpaa(3L, "PG-13"))
                    .build();
            Film savedFilm = filmStorage.create(film);
            Long filmId = savedFilm.getId();
            // when
            Optional<Film> result = filmStorage.findById(filmId);
            // then
            assertThat(result)
                    .isPresent()
                    .hasValueSatisfying(foundFilm -> {
                        assertThat(foundFilm)
                                .hasFieldOrPropertyWithValue("id", filmId)
                                .hasFieldOrPropertyWithValue("name", "Interstellar")
                                .hasFieldOrPropertyWithValue("description", "Space adventure.")
                                .hasFieldOrPropertyWithValue("releaseDate", LocalDate.of(2014, 11, 7))
                                .hasFieldOrPropertyWithValue("duration", 169);
                    });
        }

        @Test
        @DisplayName("Поиск по ID: должен вернуть пустой Optional, если фильма нет")
        void findById_Should_Return_Empty_When_Not_Exists_Test() {
            // given
            Long nonExistentId = 999L;
            // when
            Optional<Film> result = filmStorage.findById(nonExistentId);
            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты метода findAll()")
    class FindAllTests {
        @Test
        @DisplayName("Получение всех фильмов: должен вернуть список всех записей")
        void findAll_Should_Return_All_Films_Test() {
            // given
            Film film1 = Film.builder()
                    .name("Dune")
                    .description("Epic sci-fi.")
                    .releaseDate(LocalDate.of(2021, 9, 3))
                    .duration(155)
                    .ratingMpaa(new RatingMpaa(3L,"PG-13"))
                    .build();
            Film film2 = Film.builder()
                    .name("Eternal Sunshine")
                    .description("Romantic drama.")
                    .releaseDate(LocalDate.of(2004, 3, 19))
                    .duration(108)
                    .ratingMpaa(new RatingMpaa(2L, "R"))
                    .build();
            filmStorage.create(film1);
            filmStorage.create(film2);
            // when
            List<Film> films = filmStorage.findAll();
            // then
            assertThat(films)
                    .hasSize(2)
                    .anySatisfy(film ->
                            assertThat(film).hasFieldOrPropertyWithValue("name", "Dune"))
                    .anySatisfy(film ->
                            assertThat(film).hasFieldOrPropertyWithValue("name", "Eternal Sunshine"));
        }
    }

    @Nested
    @DisplayName("Тесты метода delete()")
    class DeleteTests {
        @Test
        @DisplayName("Удаление фильма: должен удалить запись из БД")
        void delete_Should_Remove_Film_From_Db_Test() {
            // given
            Film film = Film.builder()
                    .name("Blade Runner 2049")
                    .description("Neo-noir sci-fi.")
                    .releaseDate(LocalDate.of(2017, 10, 6))
                    .duration(164)
                    .ratingMpaa(new RatingMpaa(3L, "R"))
                    .build();
            Film savedFilm = filmStorage.create(film);
            Long filmId = savedFilm.getId();
            // when
            boolean deleteResult = filmStorage.delete(filmId);
            // then
            assertThat(deleteResult).isTrue();
            boolean existsInDb = filmStorage.isFilmExists(filmId);
            assertThat(existsInDb).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты метода findBySeveralIds()")
    class FindBySeveralIdsTests {
        @Test
        @DisplayName("Поиск по списку ID: должен вернуть фильмы")
        void findBySeveralIds_Should_Return_Films_Test() {
            // given
            Film film1 = Film.builder()
                    .name("Dune")
                    .description("Epic sci-fi.")
                    .releaseDate(LocalDate.of(2021, 9, 3))
                    .duration(155)
                    .ratingMpaa(new RatingMpaa(3L, "PG-13"))
                    .build();
            Film savedFilm1 = filmStorage.create(film1);

            Film film2 = Film.builder()
                    .name("Eternal Sunshine")
                    .description("Romantic drama.")
                    .releaseDate(LocalDate.of(2004, 3, 19))
                    .duration(108)
                    .ratingMpaa(new RatingMpaa(2L, "R"))
                    .build();
            Film savedFilm2 = filmStorage.create(film2);

            List<Long> filmIds = List.of(savedFilm1.getId(), savedFilm2.getId());
            // when
            List<Film> foundFilms = filmStorage.findBySeveralIds(filmIds);
            // then
            assertThat(foundFilms)
                    .hasSize(2)
                    .extracting("id")
                    .containsExactlyInAnyOrder(savedFilm1.getId(), savedFilm2.getId());
        }

        @Test
        @DisplayName("Поиск по списку ID: должен вернуть пустой список, если ID не найдены")
        void findBySeveralIds_Should_Return_Empty_List_When_Ids_Not_Found_Test() {
            // given
            List<Long> nonExistentIds = List.of(666L, 999L);
            // when
            List<Film> foundFilms = filmStorage.findBySeveralIds(nonExistentIds);
            // then
            assertThat(foundFilms).isEmpty();
        }

        @Test
        @DisplayName("Поиск по списку ID: должен корректно обработать пустой список ID")
        void findBySeveralIds_Should_Handle_Empty_Id_List_Test() {
            // given
            List<Long> emptyIds = List.of();
            // when
            List<Film> foundFilms = filmStorage.findBySeveralIds(emptyIds);
            // then
            assertThat(foundFilms).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты метода isFilmExists()")
    class IsFilmExistsTests {
        @Test
        @DisplayName("Проверка существования: должен вернуть true для существующего фильма")
        void isFilmExists_Should_Return_True_For_Existing_Film_Test() {
            // given
            Film film = Film.builder()
                    .name("Interstellar")
                    .description("Space adventure.")
                    .releaseDate(LocalDate.of(2014, 11, 7))
                    .duration(169)
                    .ratingMpaa(new RatingMpaa(3L, "PG-13"))
                    .build();
            Film savedFilm = filmStorage.create(film);
            Long filmId = savedFilm.getId();
            // when
            boolean exists = filmStorage.isFilmExists(filmId);
            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Проверка существования: должен вернуть false для несуществующего фильма")
        void isFilmExists_Should_Return_False_For_NonExisting_Film_Test() {
            // given
            Long nonExistentId = 666L;
            // when
            boolean exists = filmStorage.isFilmExists(nonExistentId);
            // then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Проверка существования: должен корректно обработать null ID")
        void isFilmExists_Should_Handle_Null_Id_Test() {
            // given
            Long nullId = null;
            // when, then
            boolean exists = filmStorage.isFilmExists(nullId);
            assertThat(exists).isFalse();
        }
    }
}
