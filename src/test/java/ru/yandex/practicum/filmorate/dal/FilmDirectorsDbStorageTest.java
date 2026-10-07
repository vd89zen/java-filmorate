package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты FilmDirectorsDbStorage")
class FilmDirectorsDbStorageTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private FilmDbStorage filmStorage;
    @Autowired private DirectorDbStorage directorStorage;

    private FilmDirectorsDbStorage storage;

    private Long filmId1;
    private Long filmId2;
    private Long directorId1;
    private Long directorId2;

    @BeforeEach
    void setUp() {
        cleanUp();
        storage = new FilmDirectorsDbStorage(jdbcTemplate);
        filmId1 = createFilm("Film1");
        filmId2 = createFilm("Film2");
        directorId1 = createDirector("D1");
        directorId2 = createDirector("D2");
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM film_directors");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM directors");
    }

    private Long createFilm(String name) {
        return filmStorage.create(Film.builder()
                .name(name).description("desc")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120).mpa(new RatingMpaaId(1L))
                .build()).getId();
    }

    private Long createDirector(String name) {
        return directorStorage.create(Director.builder().name(name).build()).getId();
    }

    @Nested
    @DisplayName("Тесты insert()")
    class InsertTests {

        @Test
        @DisplayName("Связывает фильм и режиссёров")
        void insert_Should_LinkFilmAndDirectors_Test() {
            storage.insert(filmId1, Set.of(directorId1, directorId2));

            assertThat(storage.getDirectorIdsOfFilm(filmId1))
                    .containsExactlyInAnyOrder(directorId1, directorId2);
        }

        @Test
        @DisplayName("insert(null) → IllegalArgumentException")
        void insert_Should_Throw_ForNull_Test() {
            assertThatThrownBy(() -> storage.insert(filmId1, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("insert(emptySet) — no-op")
        void insert_Should_DoNothing_ForEmptySet_Test() {
            assertThatCode(() -> storage.insert(filmId1, Set.of()))
                    .doesNotThrowAnyException();

            assertThat(storage.getDirectorIdsOfFilm(filmId1)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты getDirectorsOfFilm() и getDirectorIdsOfFilm()")
    class GettersTests {

        @Test
        @DisplayName("getDirectorsOfFilm: возвращает отсортированных по id")
        void getDirectorsOfFilm_Should_ReturnSorted_Test() {
            storage.insert(filmId1, Set.of(directorId2, directorId1));

            assertThat(storage.getDirectorsOfFilm(filmId1))
                    .extracting(Director::getId)
                    .containsExactly(directorId1, directorId2);
        }

        @Test
        @DisplayName("getDirectorsOfFilm: пустой для фильма без режиссёров")
        void getDirectorsOfFilm_Should_ReturnEmpty_ForEmptyFilm_Test() {
            assertThat(storage.getDirectorsOfFilm(filmId1)).isEmpty();
        }

        @Test
        @DisplayName("getDirectorIdsOfFilm: возвращает набор id")
        void getDirectorIdsOfFilm_Should_ReturnSet_Test() {
            storage.insert(filmId1, Set.of(directorId1, directorId2));

            assertThat(storage.getDirectorIdsOfFilm(filmId1))
                    .containsExactlyInAnyOrder(directorId1, directorId2);
        }
    }

    @Nested
    @DisplayName("Тесты getDirectorsByFilmsIds()")
    class GetDirectorsByFilmsIdsTests {

        @Test
        @DisplayName("Возвращает карту filmId → режиссёры")
        void getDirectorsByFilmsIds_Should_ReturnMap_Test() {
            storage.insert(filmId1, Set.of(directorId1));
            storage.insert(filmId2, Set.of(directorId1, directorId2));

            Map<Long, List<Director>> result = storage.getDirectorsByFilmsIds(Set.of(filmId1, filmId2));

            assertThat(result).containsOnlyKeys(filmId1, filmId2);
            assertThat(result.get(filmId1)).hasSize(1);
            assertThat(result.get(filmId2)).hasSize(2);
        }

        @Test
        @DisplayName("Пустой set → пустая карта")
        void getDirectorsByFilmsIds_Should_ReturnEmptyMap_ForEmptySet_Test() {
            assertThat(storage.getDirectorsByFilmsIds(Set.of())).isEmpty();
        }

        @Test
        @DisplayName("Несуществующие filmId игнорируются")
        void getDirectorsByFilmsIds_Should_IgnoreMissingIds_Test() {
            storage.insert(filmId1, Set.of(directorId1));

            Map<Long, List<Director>> result = storage.getDirectorsByFilmsIds(Set.of(filmId1, 999L));

            assertThat(result).containsOnlyKeys(filmId1);
        }
    }

    @Nested
    @DisplayName("Тесты deleteAllDirectorsFromFilm()")
    class DeleteAllTests {

        @Test
        @DisplayName("Удаляет все связи фильма")
        void deleteAllDirectorsFromFilm_Should_RemoveAll_Test() {
            storage.insert(filmId1, Set.of(directorId1, directorId2));
            storage.deleteAllDirectorsFromFilm(filmId1);

            assertThat(storage.getDirectorIdsOfFilm(filmId1)).isEmpty();
        }

        @Test
        @DisplayName("Не влияет на связи других фильмов")
        void deleteAllDirectorsFromFilm_Should_NotAffectOtherFilms_Test() {
            storage.insert(filmId1, Set.of(directorId1));
            storage.insert(filmId2, Set.of(directorId2));

            storage.deleteAllDirectorsFromFilm(filmId1);

            assertThat(storage.getDirectorIdsOfFilm(filmId1)).isEmpty();
            assertThat(storage.getDirectorIdsOfFilm(filmId2)).containsExactly(directorId2);
        }

        @Test
        @DisplayName("Идемпотентен при повторном вызове")
        void deleteAllDirectorsFromFilm_Should_BeIdempotent_Test() {
            storage.insert(filmId1, Set.of(directorId1));
            storage.deleteAllDirectorsFromFilm(filmId1);

            assertThatCode(() -> storage.deleteAllDirectorsFromFilm(filmId1))
                    .doesNotThrowAnyException();
        }
    }
}