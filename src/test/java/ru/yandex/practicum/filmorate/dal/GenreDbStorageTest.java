package ru.yandex.practicum.filmorate.dal;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@DisplayName("GenreDbStorage Тесты")
class GenreDbStorageTest {

    @Autowired
    private GenreDbStorage storage;
    @Autowired
    private FilmDbStorage filmStorage;
    @Autowired
    private FilmGenresDbStorage filmGenresStorage;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanUp();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        // film_genres и films чистим напрямую — на них @CacheEvict не висит,
        // а жанры — через storage, чтобы сбросить кэш "genres".
        jdbcTemplate.update("DELETE FROM film_genres WHERE genre_id IN " +
                "(SELECT id FROM genres WHERE name LIKE 'TestGenre%')");
        jdbcTemplate.update("DELETE FROM films WHERE name LIKE 'TestFilm%'");

        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM genres WHERE name LIKE 'TestGenre%'", Long.class);
        ids.forEach(storage::delete);
    }

    private Genre newGenre(String name) {
        return new Genre(null, name);
    }

    private Long createFilm(String name) {
        return filmStorage.create(Film.builder()
                .name(name)
                .description("desc")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(120)
                .mpa(new RatingMpaaId(1L))
                .build()).getId();
    }

    @Nested
    @DisplayName("Тесты findAll()")
    class FindAllTests {
        @Test
        @DisplayName("Получаем все жанры, в порядке ID")
        void findAll_Should_Return_All_Genres_In_Id_Order_Test() {
            // given, when
            List<Genre> allGenres = storage.findAll();
            // then
            System.out.println(allGenres);
            assertThat(allGenres)
                    .hasSize(6);
        }
    }

    @Nested
    @DisplayName("Тесты findById()")
    class FindByIdTests {
        @Test
        @DisplayName("Получаем Optional с жанром по существующему ID")
        void findById_Should_Return_Optional_With_Genre_Test() {
            // given, when
            Optional<Genre> found = storage.findById(1L);
            // then
            System.out.println(found);
            assertThat(found)
                    .isPresent();
        }

        @Test
        @DisplayName("Получаем пустой Optional с жанром по не существующему ID")
        void findById_Should_Return_Empty_Optional_For_Non_Exists_Id_Test() {
            // given, when
            Optional<Genre> found = storage.findById(666L);
            // then
            System.out.println(found);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Получаем пустой Optional с жанром по NULL ID")
        void findById_Should_Return_Empty_Optional_For_Null_Id_Test() {
            // given, when
            Optional<Genre> found = storage.findById(null);
            // then
            System.out.println(found);
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты create()")
    class CreateTests {

        @Test
        @DisplayName("Создание: возвращает id, сохраняет имя")
        void create_Should_ReturnGenreWithId_Test() {
            Genre saved = storage.create(newGenre("TestGenreCreate"));

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("TestGenreCreate");
            assertThat(storage.findById(saved.getId()))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("name", "TestGenreCreate");
        }

        @Test
        @DisplayName("Созданный жанр виден в findAll()")
        void create_Should_BeVisibleInFindAll_Test() {
            storage.create(newGenre("TestGenreCreateVisible"));

            assertThat(storage.findAll())
                    .extracting(Genre::getName)
                    .contains("TestGenreCreateVisible");
        }
    }

    @Nested
    @DisplayName("Тесты update()")
    class UpdateTests {

        @Test
        @DisplayName("update: меняет имя")
        void update_Should_ChangeName_Test() {
            Genre saved = storage.create(newGenre("TestGenreUpdateOld"));
            saved.setName("TestGenreUpdateNew");
            storage.update(saved);

            assertThat(storage.findById(saved.getId()))
                    .get()
                    .hasFieldOrPropertyWithValue("name", "TestGenreUpdateNew");
        }
    }

    @Nested
    @DisplayName("Тесты delete()")
    class DeleteTests {

        @Test
        @DisplayName("delete: true → false при повторном вызове")
        void delete_Should_ReturnTrueThenFalse_Test() {
            Genre saved = storage.create(newGenre("TestGenreDelete"));

            assertThat(storage.delete(saved.getId())).isTrue();
            assertThat(storage.delete(saved.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты findByIds()")
    class FindByIdsTests {

        @Test
        @DisplayName("Возвращает только существующих, отсортированных по id")
        void findByIds_Should_ReturnOnlyRequestedSortedById_Test() {
            Genre a = storage.create(newGenre("TestGenreA"));
            Genre b = storage.create(newGenre("TestGenreB"));
            storage.create(newGenre("TestGenreC"));

            List<Genre> result = storage.findByIds(Set.of(b.getId(), a.getId()));

            assertThat(result)
                    .extracting(Genre::getName)
                    .containsExactly("TestGenreA", "TestGenreB");
        }

        @Test
        @DisplayName("Несуществующие id игнорируются")
        void findByIds_Should_IgnoreMissingIds_Test() {
            Genre a = storage.create(newGenre("TestGenreA"));

            assertThat(storage.findByIds(Set.of(a.getId(), 999L)))
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("Тесты isGenreExists() и isNameAlreadyUse()")
    class ChecksTests {

        @Test
        @DisplayName("isGenreExists: true для существующего, false для пропавшего")
        void isGenreExists_Should_ReturnTrueThenFalse_Test() {
            Genre saved = storage.create(newGenre("TestGenreExists"));

            assertThat(storage.isGenreExists(saved.getId())).isTrue();

            storage.delete(saved.getId());

            assertThat(storage.isGenreExists(saved.getId())).isFalse();
            assertThat(storage.isGenreExists(999L)).isFalse();
        }

        @Test
        @DisplayName("isNameAlreadyUse: true для занятого имени")
        void isNameAlreadyUse_Should_ReturnTrue_ForExisting_Test() {
            storage.create(newGenre("TestGenreName"));

            assertThat(storage.isNameAlreadyUse("TestGenreName")).isTrue();
            assertThat(storage.isNameAlreadyUse("TestGenreMissing")).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты isGenreInUse()")
    class IsGenreInUseTests {

        @Test
        @DisplayName("false, если жанр не привязан ни к одному фильму")
        void isGenreInUse_Should_ReturnFalse_ForUnused_Test() {
            Genre saved = storage.create(newGenre("TestGenreUnused"));

            assertThat(storage.isGenreInUse(saved.getId())).isFalse();
        }

        @Test
        @DisplayName("true, если жанр привязан к фильму")
        void isGenreInUse_Should_ReturnTrue_WhenLinked_Test() {
            Genre saved = storage.create(newGenre("TestGenreInUse"));
            Long filmId = createFilm("TestFilmInUse");

            filmGenresStorage.insert(filmId, Set.of(saved.getId()));

            assertThat(storage.isGenreInUse(saved.getId())).isTrue();
        }

        @Test
        @DisplayName("false после отвязки от фильма")
        void isGenreInUse_Should_ReturnFalse_AfterUnlink_Test() {
            Genre saved = storage.create(newGenre("TestGenreUnlink"));
            Long filmId = createFilm("TestFilmUnlink");

            filmGenresStorage.insert(filmId, Set.of(saved.getId()));
            filmGenresStorage.deleteAllGenresFromFilm(filmId);

            assertThat(storage.isGenreInUse(saved.getId())).isFalse();
        }
    }
}