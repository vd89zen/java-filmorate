package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.RatingMpaa;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmGenresDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private FilmDbStorage filmStorage;

    private FilmGenresDbStorage filmGenresStorage;
    private Long filmId;
    private Long filmIdTo;
    private Set<Long> genreIds;
    private Set<Long> genreIdsTo;

    @BeforeEach
    void setUp() {
        filmGenresStorage = new FilmGenresDbStorage(jdbcTemplate);
        filmId = createTestFilm();
        filmIdTo = createTestFilm();
        genreIds = Set.of(1L, 2L, 3L);
        genreIdsTo = Set.of(4L, 5L, 6L);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM films");
    }

    private Long createTestFilm() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Description")
                .releaseDate(LocalDate.now())
                .duration(90)
                .ratingMpaa(new RatingMpaa(1L, "G"))
                .build();
        return filmStorage.create(film).getId();
    }

    @Nested
    @DisplayName("Тесты метода insert()")
    class InsertTests {
        @Test
        @DisplayName("Добавление жанров к фильму: должен вставить записи в БД")
        void insert_Should_Insert_Genres_For_Film_Test() {
            // given, when
            filmGenresStorage.insert(filmId, genreIds);
            // then
            Set<Long> savedGenreIds = filmGenresStorage.getGenreIdsOfFilm(filmId);
            assertThat(savedGenreIds).containsExactlyInAnyOrderElementsOf(genreIds);
        }

        @Test
        @DisplayName("Добавление пустых жанров: не должен вставлять записи")
        void insert_Should_Not_Insert_Empty_Genres_Test() {
            // given
            Set<Long> emptyGenreIds = Collections.emptySet();
            // when
            filmGenresStorage.insert(filmId, emptyGenreIds);
            // then
            Set<Long> savedGenreIds = filmGenresStorage.getGenreIdsOfFilm(filmId);
            assertThat(savedGenreIds).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты метода getGenresOfFilm()")
    class GetGenresOfFilmTests {
        @Test
        @DisplayName("Получение жанров фильма: должен вернуть все жанры для указанного фильма")
        void getGenresOfFilm_Should_Return_All_Genres_Of_Film_Test() {
            // given
            filmGenresStorage.insert(filmId, genreIds);
            // when
            Set<Genre> genres = filmGenresStorage.getGenresOfFilm(filmId);
            // then
            assertThat(genres).hasSize(3);
            assertThat(genres)
                    .extracting("id")
                    .containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("Получение жанров несуществующего фильма: должен вернуть пустой набор")
        void getGenresOfFilm_Should_Return_Empty_Set_For_NonExisting_Film_Test() {
            setUp();
            // given
            Long nonExistingFilmId = 666L;
            // when
            Set<Genre> genres = filmGenresStorage.getGenresOfFilm(nonExistingFilmId);
            // then
            assertThat(genres).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты метода getGenreIdsOfFilm()")
    class GetGenreIdsOfFilmTests {
        @Test
        @DisplayName("Получение ID жанров фильма: должен вернуть ID всех жанров фильма")
        void getGenreIdsOfFilm_Should_Return_Ids_Of_All_Genres_Test() {
            // given
            filmGenresStorage.insert(filmId, genreIds);
            // when
            Set<Long> genreIds = filmGenresStorage.getGenreIdsOfFilm(filmId);
            // then
            assertThat(genreIds).containsExactlyInAnyOrderElementsOf(genreIds);
        }

        @Test
        @DisplayName("Получение ID жанров несуществующего фильма: должен вернуть пустой набор")
        void getGenreIdsOfFilm_Should_Return_Empty_Set_For_NonExisting_Film_Test() {
            setUp();
            // given
            Long nonExistingFilmId = 999L;
            // when
            Set<Long> genreIds = filmGenresStorage.getGenreIdsOfFilm(nonExistingFilmId);
            // then
            assertThat(genreIds).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты метода getGenresByFilmsIds()")
    class GetGenresByFilmsIdsTests {
        @Test
        @DisplayName("Получение жанров по списку ID фильмов: должен вернуть жанры для всех указанных фильмов")
        void getGenresByFilmsIds_Should_Return_Genres_For_All_Films_Test() {
            // given
            filmGenresStorage.insert(filmId, genreIds);
            filmGenresStorage.insert(filmIdTo, genreIdsTo);
            Set<Long> filmsIds = Set.of(filmId, filmIdTo);
            // when
            Map<Long, Set<Genre>> result = filmGenresStorage.getGenresByFilmsIds(filmsIds);
            // then
            assertThat(result).hasSize(2);

            assertThat(result.get(filmId))
                    .extracting("id")
                    .containsExactlyInAnyOrder(1L, 2L, 3L);

            assertThat(result.get(filmIdTo))
                    .extracting("id")
                    .containsExactlyInAnyOrder(4L, 5L, 6L);
        }

        @Test
        @DisplayName("Получение жанров для пустого списка ID фильмов: должен вернуть пустую карту")
        void getGenresByFilmsIds_Should_Return_Empty_Map_For_EmptyIds_Test() {
            // given
            Set<Long> emptyFilmIds = Collections.emptySet();
            // when
            Map<Long, Set<Genre>> result = filmGenresStorage.getGenresByFilmsIds(emptyFilmIds);
            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Получение жанров для несуществующих ID фильмов: должен вернуть карту без записей для этих ID")
        void getGenresByFilmsIds_Should_Handle_NonExistingIds_Test() {
            setUp();
            // given
            Set<Long> nonExistingIds = Set.of(222L, 555L);
            // when
            Map<Long, Set<Genre>> result = filmGenresStorage.getGenresByFilmsIds(nonExistingIds);
            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты метода deleteGenresFromFilm()")
    class DeleteGenresFromFilmTests {
        @Test
        @DisplayName("Удаление конкретных жанров из фильма: должен удалить только указанные жанры")
        void deleteGenresFromFilm_Should_Remove_Only_Specified_Genres_Test() {
            // given
            filmGenresStorage.insert(filmId, genreIds);
            Set<Long> toDelete = Set.of(2L, 3L);
            // when
            filmGenresStorage.deleteGenresFromFilm(filmId, toDelete);
            // then
            Set<Long> remainingIds = filmGenresStorage.getGenreIdsOfFilm(filmId);
            assertThat(remainingIds)
                    .containsExactly(1L)
                    .doesNotContain(2L, 3L);
        }

        @Test
        @DisplayName("Удаление жанров из несуществующего фильма: не должен вызывать ошибку")
        void deleteGenresFromFilm_Should_NotFail_For_NonExistingFilm_Test() {
            // given
            Long nonExistingFilmId = 666L;
            // when, then
            assertDoesNotThrow(() ->
                    filmGenresStorage.deleteGenresFromFilm(nonExistingFilmId, genreIds));

            Set<Long> ids = filmGenresStorage.getGenreIdsOfFilm(nonExistingFilmId);
            assertThat(ids).isEmpty();
        }

        @Test
        @DisplayName("Удаление пустых жанров: не должен изменять БД")
        void deleteGenresFromFilm_Should_NotExecute_For_EmptySet_Test() {
            // given
            filmGenresStorage.insert(filmId, genreIds);
            Set<Long> emptySet = Collections.emptySet();
            // when
            filmGenresStorage.deleteGenresFromFilm(filmId, emptySet);
            // then
            Set<Long> currentIds = filmGenresStorage.getGenreIdsOfFilm(filmId);
            assertThat(currentIds).containsExactlyInAnyOrderElementsOf(genreIds);
        }

        @Test
        @DisplayName("Удаление жанров, которых нет у фильма: не должен влиять на существующие")
        void deleteGenresFromFilm_Should_Ignore_NonAssignedGenres_Test() {
            // given
            filmGenresStorage.insert(filmId, Set.of(1L, 2L));
            Set<Long> nonAssignedGenres = Set.of(3L, 4L);
            // when
            filmGenresStorage.deleteGenresFromFilm(filmId, nonAssignedGenres);
            // then
            Set<Long> actualIds = filmGenresStorage.getGenreIdsOfFilm(filmId);
            assertThat(actualIds).containsExactlyInAnyOrder(1L, 2L);
        }
    }

    @Nested
    @DisplayName("Тесты метода deleteAllGenresFromFilm()")
    class DeleteAllGenresFromFilmTests {
        @Test
        @DisplayName("Удаление всех жанров фильма: должен очистить все записи для фильма")
        void deleteAllGenresFromFilm_Should_Remove_All_Genres_Of_Film_Test() {
            // given
            filmGenresStorage.insert(filmId, genreIds);
            // when
            filmGenresStorage.deleteAllGenresFromFilm(filmId);
            // then
            Set<Long> remainingIds = filmGenresStorage.getGenreIdsOfFilm(filmId);
            assertThat(remainingIds).isEmpty();
        }

        @Test
        @DisplayName("Удаление у несуществующего фильма: не должен вызывать ошибку")
        void deleteAllGenresFromFilm_Should_NotFail_For_NonExistingFilm_Test() {
            // given
            Long nonExistingId = 888L;
            // when, then
            assertDoesNotThrow(() -> filmGenresStorage.deleteAllGenresFromFilm(nonExistingId));
            // then
            Set<Long> ids = filmGenresStorage.getGenreIdsOfFilm(nonExistingId);
            assertThat(ids).isEmpty();
        }

        @Test
        @DisplayName("Повторный вызов deleteAllGenresFromFilm: должен быть без последствий")
        void deleteAllGenresFromFilm_Should_Be_Idempotent_Test() {
            // given
            filmGenresStorage.deleteAllGenresFromFilm(filmId);
            // when
            assertDoesNotThrow(() -> filmGenresStorage.deleteAllGenresFromFilm(filmId));
            // then
            Set<Long> ids = filmGenresStorage.getGenreIdsOfFilm(filmId);
            assertThat(ids).isEmpty();
        }

        @Test
        @DisplayName("Удаление после частичной очистки: должен удалить оставшиеся жанры")
        void deleteAllGenresFromFilm_Should_Remove_Remaining_After_Partial_Deletion_Test() {
            // given
            filmGenresStorage.insert(filmId, genreIds);
            filmGenresStorage.deleteGenresFromFilm(filmId, Set.of(1L));
            // when
            filmGenresStorage.deleteAllGenresFromFilm(filmId);
            // then
            Set<Long> ids = filmGenresStorage.getGenreIdsOfFilm(filmId);
            assertThat(ids).isEmpty();
        }
    }
}