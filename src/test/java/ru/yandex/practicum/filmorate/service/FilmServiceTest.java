package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты FilmService")
class FilmServiceTest {

    private final FilmService filmService;
    private final UserService userService;
    private final DirectorService directorService;

    @Autowired private JdbcTemplate jdbcTemplate;

    private Long userId;

    @BeforeEach
    void setUp() {
        cleanUp();
        userId = userService.create(newUserRequest("user@mail.com", "user")).getId();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM review_opinions");
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM film_directors");
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM directors");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private NewUserRequest newUserRequest(String email, String login) {
        NewUserRequest r = new NewUserRequest();
        r.setEmail(email);
        r.setLogin(login);
        r.setName(login);
        r.setBirthday(LocalDate.of(1990, 1, 1));
        return r;
    }

    private NewFilmRequest newFilmRequest(String name) {
        NewFilmRequest r = new NewFilmRequest();
        r.setName(name);
        r.setDescription("desc");
        r.setReleaseDate(LocalDate.of(2000, 1, 1));
        r.setDuration(120);
        r.setMpa(new RatingMpaaId(1L));
        return r;
    }

    @Nested
    @DisplayName("Тесты create()")
    class CreateTests {

        @Test
        @DisplayName("Создание без жанров и режиссёров")
        void create_Should_ReturnFilmWithoutRelations_Test() {
            FilmDto created = filmService.create(newFilmRequest("Film"));

            assertThat(created.getId()).isNotNull();
            assertThat(created.getGenres()).isEmpty();
            assertThat(created.getDirectors()).isEmpty();
            assertThat(created.getLikesCount()).isZero();
        }

        @Test
        @DisplayName("Создание с жанрами и режиссёрами")
        void create_Should_LinkGenresAndDirectors_Test() {
            Long directorId = directorService.create(newDirectorRequest("Nolan")).getId();

            NewFilmRequest req = newFilmRequest("Film");
            req.setGenres(Set.of(new GenreId(1L)));
            req.setDirectors(Set.of(new DirectorId(directorId)));

            FilmDto created = filmService.create(req);

            assertThat(created.getGenres()).extracting(GenreDto::getId).containsExactly(1L);
            assertThat(created.getDirectors()).extracting(DirectorDto::getId).containsExactly(directorId);
        }

        @Test
        @DisplayName("Несуществующий режиссёр → NotFoundException")
        void create_Should_ThrowNotFound_ForMissingDirector_Test() {
            NewFilmRequest req = newFilmRequest("Film");
            req.setDirectors(Set.of(new DirectorId(999L)));

            assertThatThrownBy(() -> filmService.create(req))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Дата релиза раньше 28.12.1895 → ValidationException")
        void create_Should_Throw_ForTooOldDate_Test() {
            NewFilmRequest req = newFilmRequest("Film");
            req.setReleaseDate(LocalDate.of(1800, 1, 1));

            assertThatThrownBy(() -> filmService.create(req))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("Тесты update()")
    class UpdateTests {

        @Test
        @DisplayName("Обновление только имени")
        void update_Should_ChangeOnlyName_Test() {
            FilmDto created = filmService.create(newFilmRequest("Old"));

            UpdateFilmRequest req = new UpdateFilmRequest();
            req.setId(created.getId());
            req.setName("New");

            FilmDto updated = filmService.update(req);

            assertThat(updated.getName()).isEqualTo("New");
            assertThat(updated.getDescription()).isEqualTo("desc");
        }

        @Test
        @DisplayName("Обновление directors = [] очищает связи")
        void update_Should_ClearDirectors_OnEmptySet_Test() {
            Long directorId = directorService.create(newDirectorRequest("Nolan")).getId();
            NewFilmRequest create = newFilmRequest("Film");
            create.setDirectors(Set.of(new DirectorId(directorId)));
            FilmDto created = filmService.create(create);

            UpdateFilmRequest req = new UpdateFilmRequest();
            req.setId(created.getId());
            req.setDirectors(Set.of());

            FilmDto updated = filmService.update(req);

            assertThat(updated.getDirectors()).isEmpty();
        }

        @Test
        @DisplayName("Несуществующий id → NotFoundException")
        void update_Should_ThrowNotFound_Test() {
            UpdateFilmRequest req = new UpdateFilmRequest();
            req.setId(999L);
            req.setName("X");

            assertThatThrownBy(() -> filmService.update(req))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Тесты findById() и findAll()")
    class FindTests {

        @Test
        @DisplayName("findById: возвращает полный DTO")
        void findById_Should_ReturnFilm_Test() {
            FilmDto created = filmService.create(newFilmRequest("Film"));
            FilmDto found = filmService.findById(created.getId());

            assertThat(found.getId()).isEqualTo(created.getId());
            assertThat(found.getMpa()).isNotNull();
        }

        @Test
        @DisplayName("findById: несуществующий → NotFoundException")
        void findById_Should_ThrowNotFound_Test() {
            assertThatThrownBy(() -> filmService.findById(999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("findAll: пагинация")
        void findAll_Should_Paginate_Test() {
            filmService.create(newFilmRequest("F1"));
            filmService.create(newFilmRequest("F2"));
            filmService.create(newFilmRequest("F3"));

            assertThat(filmService.findAll(0, 2)).hasSize(2);
            assertThat(filmService.findAll(2, 2)).hasSize(1);
        }

        @Test
        @DisplayName("findAll: пустая страница")
        void findAll_Should_ReturnEmpty_WhenNoFilms_Test() {
            assertThat(filmService.findAll(0, 10)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты лайков")
    class LikeTests {

        @Test
        @DisplayName("likeFilm: увеличивает likesCount")
        void likeFilm_Should_IncrementLikes_Test() {
            FilmDto created = filmService.create(newFilmRequest("Film"));
            filmService.likeFilm(created.getId(), userId);

            assertThat(filmService.findById(created.getId()).getLikesCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("likeFilm: повторный лайк → ValidationException")
        void likeFilm_Should_Throw_OnDuplicate_Test() {
            FilmDto created = filmService.create(newFilmRequest("Film"));
            filmService.likeFilm(created.getId(), userId);

            assertThatThrownBy(() -> filmService.likeFilm(created.getId(), userId))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("unlikeFilm: снимает лайк")
        void unlikeFilm_Should_DecrementLikes_Test() {
            FilmDto created = filmService.create(newFilmRequest("Film"));
            filmService.likeFilm(created.getId(), userId);
            filmService.unlikeFilm(created.getId(), userId);

            assertThat(filmService.findById(created.getId()).getLikesCount()).isZero();
        }

        @Test
        @DisplayName("unlikeFilm: нет лайка → NotFoundException")
        void unlikeFilm_Should_Throw_WhenNoLike_Test() {
            FilmDto created = filmService.create(newFilmRequest("Film"));

            assertThatThrownBy(() -> filmService.unlikeFilm(created.getId(), userId))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Тесты getTopPopularFilms()")
    class TopPopularTests {

        @Test
        @DisplayName("Сортировка по убыванию лайков")
        void getTopPopularFilms_Should_SortByLikesDesc_Test() {
            FilmDto f1 = filmService.create(newFilmRequest("F1"));
            FilmDto f2 = filmService.create(newFilmRequest("F2"));

            Long userId2 = userService.create(newUserRequest("user2@mail.com", "user2")).getId();
            filmService.likeFilm(f2.getId(), userId);
            filmService.likeFilm(f2.getId(), userId2);
            filmService.likeFilm(f1.getId(), userId);

            List<FilmDto> top = filmService.getTopPopularFilms(10, null, null);

            assertThat(top).hasSize(2);
            assertThat(top.get(0).getId()).isEqualTo(f2.getId());   // 2 лайка
            assertThat(top.get(1).getId()).isEqualTo(f1.getId());   // 1 лайк
        }

        @Test
        @DisplayName("Фильтр по жанру")
        void getTopPopularFilms_Should_FilterByGenre_Test() {
            NewFilmRequest req1 = newFilmRequest("F1");
            req1.setGenres(Set.of(new GenreId(1L)));
            FilmDto f1 = filmService.create(req1);

            NewFilmRequest req2 = newFilmRequest("F2");
            req2.setGenres(Set.of(new GenreId(2L)));
            FilmDto f2 = filmService.create(req2);

            filmService.likeFilm(f1.getId(), userId);
            filmService.likeFilm(f2.getId(), userId);

            List<FilmDto> top = filmService.getTopPopularFilms(10, 1L, null);

            assertThat(top).hasSize(1);
            assertThat(top.get(0).getId()).isEqualTo(f1.getId());
        }

        @Test
        @DisplayName("Фильтр по году")
        void getTopPopularFilms_Should_FilterByYear_Test() {
            NewFilmRequest req1 = newFilmRequest("F1");
            req1.setReleaseDate(LocalDate.of(2010, 1, 1));
            FilmDto f1 = filmService.create(req1);

            NewFilmRequest req2 = newFilmRequest("F2");
            req2.setReleaseDate(LocalDate.of(2020, 1, 1));
            FilmDto f2 = filmService.create(req2);

            filmService.likeFilm(f1.getId(), userId);
            filmService.likeFilm(f2.getId(), userId);

            List<FilmDto> top = filmService.getTopPopularFilms(10, null, 2010);

            assertThat(top).hasSize(1);
            assertThat(top.get(0).getId()).isEqualTo(f1.getId());
        }

        @Test
        @DisplayName("Фильтр по несуществующему жанру → NotFoundException")
        void getTopPopularFilms_Should_ThrowNotFound_ForMissingGenre_Test() {
            assertThatThrownBy(() -> filmService.getTopPopularFilms(10, 999L, null))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Пустой результат при отсутствии совпадений")
        void getTopPopularFilms_Should_ReturnEmpty_WhenNoMatches_Test() {
            assertThat(filmService.getTopPopularFilms(10, 1L, 1900)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты getFilmsByDirector()")
    class FilmsByDirectorTests {

        private Long directorId;

        @BeforeEach
        void initDirector() {
            directorId = directorService.create(newDirectorRequest("Nolan")).getId();
        }

        private Long createWithDirector(String name, LocalDate date) {
            NewFilmRequest req = newFilmRequest(name);
            req.setReleaseDate(date);
            req.setDirectors(Set.of(new DirectorId(directorId)));
            return filmService.create(req).getId();
        }

        @Test
        @DisplayName("sortBy=year: сортировка по возрастанию даты")
        void getByDirector_Should_SortByYear_Test() {
            Long f2020 = createWithDirector("F2020", LocalDate.of(2020, 1, 1));
            Long f2010 = createWithDirector("F2010", LocalDate.of(2010, 1, 1));

            List<FilmDto> result = filmService.getFilmsByDirector(directorId, "year");

            assertThat(result).extracting(FilmDto::getId).containsExactly(f2010, f2020);
        }

        @Test
        @DisplayName("sortBy=likes: сортировка по убыванию лайков")
        void getByDirector_Should_SortByLikesDesc_Test() {
            Long f1 = createWithDirector("F1", LocalDate.of(2010, 1, 1));
            Long f2 = createWithDirector("F2", LocalDate.of(2011, 1, 1));

            filmService.likeFilm(f2, userId);

            List<FilmDto> result = filmService.getFilmsByDirector(directorId, "likes");
            assertThat(result.get(0).getId()).isEqualTo(f2);
        }

        @Test
        @DisplayName("sortBy=bad → ValidationException")
        void getByDirector_Should_Throw_ForBadSortBy_Test() {
            assertThatThrownBy(() -> filmService.getFilmsByDirector(directorId, "bad"))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Несуществующий режиссёр → NotFoundException")
        void getByDirector_Should_ThrowNotFound_Test() {
            assertThatThrownBy(() -> filmService.getFilmsByDirector(999L, "year"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    private NewDirectorRequest newDirectorRequest(String name) {
        NewDirectorRequest r = new NewDirectorRequest();
        r.setName(name);
        return r;
    }
}