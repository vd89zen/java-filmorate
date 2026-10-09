package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.dto.SearchRequest;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.Role;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("FilmDbStorage Тесты")
class FilmDbStorageTest {
    private final FilmDbStorage filmStorage;
    private final FilmLikesDbStorage filmLikesStorage;
    private final DirectorDbStorage directorStorage;
    private final UserDbStorage userStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId1;
    private Long userId2;

    @BeforeEach
    void setUp() {
        cleanUp();

        userId1 = userStorage.create(User.builder()
                .email("search1@mail.com")
                .login("search1")
                .name("Search1")
                .birthday(LocalDate.of(1990, 1, 1))
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .role(Role.USER)
                .build()).getId();
        userId2 = userStorage.create(User.builder()
                .email("search2@mail.com")
                .login("search2")
                .name("Search2")
                .birthday(LocalDate.of(1990, 1, 1))
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .role(Role.USER)
                .build()).getId();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM film_directors");
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM directors");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Nested
    @DisplayName("Тесты метода create()")
    class CreateTests {
        @Test
        @DisplayName("Создание фильма: должен вставить запись и вернуть объект с ID")
        void create_Should_Insert_Film_And_Return_With_Id_Test() {
            Film newFilm = Film.builder()
                    .name("Inception")
                    .description("A mind-bending thriller.")
                    .releaseDate(LocalDate.of(2010, 7, 16))
                    .duration(148)
                    .mpa(new RatingMpaaId(3L))
                    .build();

            Film createdFilm = filmStorage.create(newFilm);

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
            Film originalFilm = Film.builder()
                    .name("The Matrix")
                    .description("Sci-fi classic.")
                    .releaseDate(LocalDate.of(1999, 3, 31))
                    .duration(136)
                    .mpa(new RatingMpaaId(2L))
                    .build();
            Film savedFilm = filmStorage.create(originalFilm);
            Film updatedFilm = Film.builder()
                    .id(savedFilm.getId())
                    .name(savedFilm.getName())
                    .description(savedFilm.getDescription())
                    .releaseDate(savedFilm.getReleaseDate())
                    .duration(savedFilm.getDuration())
                    .mpa(savedFilm.getMpa())
                    .build();

            updatedFilm.setName("The Matrix: Reloaded");
            updatedFilm.setDescription("Sequel to the original.");
            updatedFilm.setDuration(138);

            filmStorage.update(updatedFilm);

            Optional<Film> retrievedFilm = filmStorage.findById(savedFilm.getId());
            assertThat(retrievedFilm)
                    .isPresent()
                    .hasValueSatisfying(film -> assertThat(film)
                            .hasFieldOrPropertyWithValue("name", "The Matrix: Reloaded")
                            .hasFieldOrPropertyWithValue("description", "Sequel to the original.")
                            .hasFieldOrPropertyWithValue("duration", 138));
        }
    }

    @Nested
    @DisplayName("Тесты метода findById()")
    class FindByIdTests {
        @Test
        @DisplayName("Поиск по ID: должен вернуть фильм, если он существует")
        void findById_Should_Return_Film_When_Exists_Test() {
            Film film = Film.builder()
                    .name("Interstellar")
                    .description("Space adventure.")
                    .releaseDate(LocalDate.of(2014, 11, 7))
                    .duration(169)
                    .mpa(new RatingMpaaId(3L))
                    .build();
            Film savedFilm = filmStorage.create(film);
            Long filmId = savedFilm.getId();

            Optional<Film> result = filmStorage.findById(filmId);

            assertThat(result)
                    .isPresent()
                    .hasValueSatisfying(foundFilm -> assertThat(foundFilm)
                            .hasFieldOrPropertyWithValue("id", filmId)
                            .hasFieldOrPropertyWithValue("name", "Interstellar")
                            .hasFieldOrPropertyWithValue("description", "Space adventure.")
                            .hasFieldOrPropertyWithValue("releaseDate", LocalDate.of(2014, 11, 7))
                            .hasFieldOrPropertyWithValue("duration", 169));
        }

        @Test
        @DisplayName("Поиск по ID: должен вернуть пустой Optional, если фильма нет")
        void findById_Should_Return_Empty_When_Not_Exists_Test() {
            Optional<Film> result = filmStorage.findById(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты метода findAll()")
    class FindAllTests {
        @Test
        @DisplayName("Получение всех фильмов: должен вернуть список всех записей")
        void findAll_Should_Return_All_Films_Test() {
            Film film1 = Film.builder()
                    .name("Dune")
                    .description("Epic sci-fi.")
                    .releaseDate(LocalDate.of(2021, 9, 3))
                    .duration(155)
                    .mpa(new RatingMpaaId(3L))
                    .build();
            Film film2 = Film.builder()
                    .name("Eternal Sunshine")
                    .description("Romantic drama.")
                    .releaseDate(LocalDate.of(2004, 3, 19))
                    .duration(108)
                    .mpa(new RatingMpaaId(2L))
                    .build();
            filmStorage.create(film1);
            filmStorage.create(film2);

            List<Film> films = filmStorage.findAll(0, 2);

            assertThat(films)
                    .hasSize(2)
                    .anySatisfy(film -> assertThat(film).hasFieldOrPropertyWithValue("name", "Dune"))
                    .anySatisfy(film -> assertThat(film).hasFieldOrPropertyWithValue("name", "Eternal Sunshine"));
        }
    }

    @Nested
    @DisplayName("Тесты метода delete()")
    class DeleteTests {
        @Test
        @DisplayName("Удаление фильма: должен удалить запись из БД")
        void delete_Should_Remove_Film_From_Db_Test() {
            Film film = Film.builder()
                    .name("Blade Runner 2049")
                    .description("Neo-noir sci-fi.")
                    .releaseDate(LocalDate.of(2017, 10, 6))
                    .duration(164)
                    .mpa(new RatingMpaaId(3L))
                    .build();
            Film savedFilm = filmStorage.create(film);
            Long filmId = savedFilm.getId();

            boolean deleteResult = filmStorage.delete(filmId);

            assertThat(deleteResult).isTrue();
            assertThat(filmStorage.isFilmExists(filmId)).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты метода findBySeveralIds()")
    class FindBySeveralIdsTests {
        @Test
        @DisplayName("Поиск по списку ID: должен вернуть фильмы")
        void findBySeveralIds_Should_Return_Films_Test() {
            Film savedFilm1 = filmStorage.create(Film.builder()
                    .name("Dune").description("Epic sci-fi.")
                    .releaseDate(LocalDate.of(2021, 9, 3)).duration(155)
                    .mpa(new RatingMpaaId(3L)).build());
            Film savedFilm2 = filmStorage.create(Film.builder()
                    .name("Eternal Sunshine").description("Romantic drama.")
                    .releaseDate(LocalDate.of(2004, 3, 19)).duration(108)
                    .mpa(new RatingMpaaId(2L)).build());

            List<Long> filmIds = List.of(savedFilm1.getId(), savedFilm2.getId());
            List<Film> foundFilms = filmStorage.findBySeveralIds(filmIds);

            assertThat(foundFilms)
                    .hasSize(2)
                    .extracting("id")
                    .containsExactlyInAnyOrder(savedFilm1.getId(), savedFilm2.getId());
        }

        @Test
        @DisplayName("Поиск по списку ID: должен вернуть пустой список, если ID не найдены")
        void findBySeveralIds_Should_Return_Empty_List_When_Ids_Not_Found_Test() {
            List<Film> foundFilms = filmStorage.findBySeveralIds(List.of(666L, 999L));

            assertThat(foundFilms).isEmpty();
        }

        @Test
        @DisplayName("Поиск по списку ID: должен корректно обработать пустой список ID")
        void findBySeveralIds_Should_Handle_Empty_Id_List_Test() {
            List<Film> foundFilms = filmStorage.findBySeveralIds(List.of());

            assertThat(foundFilms).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты метода isFilmExists()")
    class IsFilmExistsTests {
        @Test
        @DisplayName("Проверка существования: должен вернуть true для существующего фильма")
        void isFilmExists_Should_Return_True_For_Existing_Film_Test() {
            Film savedFilm = filmStorage.create(Film.builder()
                    .name("Interstellar").description("Space adventure.")
                    .releaseDate(LocalDate.of(2014, 11, 7)).duration(169)
                    .mpa(new RatingMpaaId(3L)).build());

            assertThat(filmStorage.isFilmExists(savedFilm.getId())).isTrue();
        }

        @Test
        @DisplayName("Проверка существования: должен вернуть false для несуществующего фильма")
        void isFilmExists_Should_Return_False_For_NonExisting_Film_Test() {
            assertThat(filmStorage.isFilmExists(666L)).isFalse();
        }

        @Test
        @DisplayName("Проверка существования: должен корректно обработать null ID")
        void isFilmExists_Should_Handle_Null_Id_Test() {
            assertThat(filmStorage.isFilmExists(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты search()")
    class SearchTests {

        private Long directorNolan;
        private Long film1;
        private Long film2;
        private Long film3;

        @BeforeEach
        void initSearchData() {
            directorNolan = directorStorage.create(
                    Director.builder().name("Christopher Nolan").build()).getId();

            film1 = filmStorage.create(Film.builder()
                    .name("Inception").description("A mind heist")
                    .releaseDate(LocalDate.of(2010, 7, 16))
                    .duration(148).mpa(new RatingMpaaId(1L)).build()).getId();
            film2 = filmStorage.create(Film.builder()
                    .name("Interstellar").description("Space adventure")
                    .releaseDate(LocalDate.of(2014, 11, 7))
                    .duration(169).mpa(new RatingMpaaId(2L)).build()).getId();
            film3 = filmStorage.create(Film.builder()
                    .name("Dune").description("Desert planet")
                    .releaseDate(LocalDate.of(2021, 9, 3))
                    .duration(155).mpa(new RatingMpaaId(2L)).build()).getId();

            jdbcTemplate.update("INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)",
                    film1, directorNolan);
            jdbcTemplate.update("INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)",
                    film2, directorNolan);

            filmLikesStorage.addLikeIfNotExists(film1, userId1);
            filmLikesStorage.addLikeIfNotExists(film1, userId2);
            filmLikesStorage.addLikeIfNotExists(film2, userId1);
        }

        private SearchRequest newRequest() {
            return SearchRequest.builder()
                    .query(null).by(Set.of())
                    .from(0).size(10)
                    .build();
        }

        @Test
        @DisplayName("Поиск по title: находит подстроку, регистронезависимо")
        void search_Should_FindByTitle_Test() {
            SearchRequest r = newRequest();
            r.setQuery("cept");
            r.setBy(Set.of("title"));

            assertThat(filmStorage.search(r)).extracting(Film::getId)
                    .containsExactly(film1);
        }

        @Test
        @DisplayName("Поиск по director: находит все фильмы режиссёра")
        void search_Should_FindByDirector_Test() {
            SearchRequest r = newRequest();
            r.setQuery("nolan");
            r.setBy(Set.of("director"));

            assertThat(filmStorage.search(r)).extracting(Film::getId)
                    .containsExactlyInAnyOrder(film1, film2);
        }

        @Test
        @DisplayName("Поиск по description: находит по описанию")
        void search_Should_FindByDescription_Test() {
            SearchRequest r = newRequest();
            r.setQuery("desert");
            r.setBy(Set.of("description"));

            assertThat(filmStorage.search(r)).extracting(Film::getId)
                    .containsExactly(film3);
        }

        @Test
        @DisplayName("Поиск по нескольким полям: OR между ними")
        void search_Should_FindInAnyOfFields_Test() {
            SearchRequest r = newRequest();
            r.setQuery("space");
            r.setBy(Set.of("title", "director", "description"));

            assertThat(filmStorage.search(r)).extracting(Film::getId)
                    .containsExactly(film2);
        }

        @Test
        @DisplayName("Фильтр по точному году")
        void search_Should_FilterByExactYear_Test() {
            SearchRequest r = newRequest();
            r.setYear(2014);

            assertThat(filmStorage.search(r)).extracting(Film::getId).containsExactly(film2);
        }

        @Test
        @DisplayName("Фильтр по диапазону годов")
        void search_Should_FilterByYearRange_Test() {
            SearchRequest r = newRequest();
            r.setYearFrom(2010);
            r.setYearTo(2015);

            assertThat(filmStorage.search(r)).extracting(Film::getId)
                    .containsExactlyInAnyOrder(film1, film2);
        }

        @Test
        @DisplayName("Фильтр по точной длительности")
        void search_Should_FilterByExactDuration_Test() {
            SearchRequest r = newRequest();
            r.setDuration(169);

            assertThat(filmStorage.search(r)).extracting(Film::getId).containsExactly(film2);
        }

        @Test
        @DisplayName("Фильтр по диапазону длительностей")
        void search_Should_FilterByDurationRange_Test() {
            SearchRequest r = newRequest();
            r.setDurationFrom(150);
            r.setDurationTo(160);

            // film1=148 — не попадает, film2=169 — не попадает, film3=155 — попадает
            assertThat(filmStorage.search(r)).extracting(Film::getId)
                    .containsExactly(film3);
        }

        @Test
        @DisplayName("Фильтр по mpaIds: один рейтинг")
        void search_Should_FilterBySingleMpa_Test() {
            SearchRequest r = newRequest();
            r.setMpaIds(Set.of(2L));

            assertThat(filmStorage.search(r)).extracting(Film::getId)
                    .containsExactlyInAnyOrder(film2, film3);
        }

        @Test
        @DisplayName("Фильтр по mpaIds: несколько рейтингов")
        void search_Should_FilterByMultipleMpa_Test() {
            SearchRequest r = newRequest();
            r.setMpaIds(Set.of(1L, 2L));

            assertThat(filmStorage.search(r)).extracting(Film::getId)
                    .containsExactlyInAnyOrder(film1, film2, film3);
        }

        @Test
        @DisplayName("Комбинация: query + year + duration + mpa")
        void search_Should_CombineAllFilters_Test() {
            SearchRequest r = newRequest();
            r.setQuery("nolan");
            r.setBy(Set.of("director"));
            r.setYearFrom(2010);
            r.setYearTo(2020);
            r.setDurationTo(160);
            r.setMpaIds(Set.of(1L));

            assertThat(filmStorage.search(r)).extracting(Film::getId).containsExactly(film1);
        }

        @Test
        @DisplayName("Сортировка по лайкам, при равенстве — по id")
        void search_Should_SortByLikesDesc_Test() {
            SearchRequest r = newRequest();

            List<Film> result = filmStorage.search(r);

            assertThat(result).extracting(Film::getId)
                    .containsExactly(film1, film2, film3);
        }

        @Test
        @DisplayName("Пагинация")
        void search_Should_Paginate_Test() {
            SearchRequest r = newRequest();
            r.setSize(2);
            r.setFrom(0);

            assertThat(filmStorage.search(r)).hasSize(2);

            r.setFrom(2);
            assertThat(filmStorage.search(r)).hasSize(1);
        }

        @Test
        @DisplayName("Пустой результат")
        void search_Should_ReturnEmpty_WhenNoMatches_Test() {
            SearchRequest r = newRequest();
            r.setQuery("zzz");
            r.setBy(Set.of("title"));

            assertThat(filmStorage.search(r)).isEmpty();
        }

        @Test
        @DisplayName("Спецсимволы LIKE трактуются буквально")
        void search_Should_EscapeLikeWildcards_Test() {
            SearchRequest r = newRequest();
            r.setQuery("100%");
            r.setBy(Set.of("title"));

            // Ни один фильм не содержит "100%" буквально — ожидаем пусто, не «всё подряд»
            assertThat(filmStorage.search(r)).isEmpty();
        }

        @Test
        @DisplayName("Без query и без фильтров — вернуть все")
        void search_Should_ReturnAllWhenNoFilters_Test() {
            SearchRequest r = newRequest();

            assertThat(filmStorage.search(r)).hasSize(3);
        }
    }
}