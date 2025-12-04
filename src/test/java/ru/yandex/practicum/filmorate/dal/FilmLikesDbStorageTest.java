package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmLikesDbStorageTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private FilmDbStorage filmStorage;
    @Autowired
    private UserDbStorage userStorage;

    private FilmLikesDbStorage storage;
    private Long filmId1;
    private Long filmId2;
    private Long filmId3;
    private Long userId1;
    private Long userId2;
    private Long userId3;
    private int prefixEmail = 1;

    @BeforeEach
    void setUp() {
        storage = new FilmLikesDbStorage(jdbcTemplate);
        filmId1 = createTestFilm();
        filmId2 = createTestFilm();
        filmId3 = createTestFilm();
        userId1 = createTestUser();
        userId2 = createTestUser();
        userId3 = createTestUser();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM users");
    }

    private Long createTestFilm() {
        Film film = Film.builder()
                .name("Test Film")
                .description("Description")
                .releaseDate(LocalDate.now())
                .duration(90)
                .mpa(new RatingMpaaId(1L))
                .build();
        return filmStorage.create(film).getId();
    }

    private Long createTestUser() {
        User user = User.builder()
                .email(String.format("%dtest@test.com", prefixEmail++))
                .login("testLogin")
                .name("testName")
                .birthday(LocalDate.now().minusYears(17))
                .build();
        return userStorage.create(user).getId();
    }

    @Nested
    @DisplayName("Тесты hasUserLikedFilm()")
    class HasUserLikedFilmTests {

        @Test
        @DisplayName("Если пользователь поставил лайк — должно вернуть true")
        void hasUserLikedFilm_Should_Return_True_When_User_Liked_Film_Test() {
            // given
            storage.addLikeIfNotExists(filmId1, userId1);
            // when, then
            assertThat(storage.hasUserLikedFilm(filmId1, userId1)).isTrue();
        }

        @Test
        @DisplayName("Если пользователь не ставил лайк — должно вернуть false")
        void hasUserLikedFilm_Should_Return_False_When_User_Not_Liked_Film_Test() {
            // given, when, then
            assertThat(storage.hasUserLikedFilm(filmId1, userId1)).isFalse();
        }

        @Test
        @DisplayName("При вызове метода для существующих сущностей - должен вернуть false")
        void hasUserLikedFilm_Should_Return_False_For_Non_Exists_Users_Test() {
            // given, when, then
            assertThat(storage.hasUserLikedFilm(666L, 999L)).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты addLikeIfNotExists")
    class AddLikeIfNotExistsTests {

        @Test
        @DisplayName("Проверяем добавление лайка — должен вернуть true")
        void addLikeIfNotExists_Should_Add_Like_And_Return_True_Test() {
            // given, when
            boolean added = storage.addLikeIfNotExists(filmId1, userId1);
            // when, then
            assertThat(added).isTrue();
            assertThat(storage.hasUserLikedFilm(filmId1, userId1)).isTrue();
        }

        @Test
        @DisplayName("Проверяем добавление повторного лайка тем же юзером — должен вернуть false")
        void addLikeIfNotExists_Should_Not_Duplicate_Like_And_Return_False_Test() {
            // given
            storage.addLikeIfNotExists(filmId1, userId1);
            // when
            boolean addedAgain = storage.addLikeIfNotExists(filmId1, userId1);
            // then
            assertThat(addedAgain).isFalse();
            assertThat(storage.getLikesCountOfFilm(filmId1)).isEqualTo(1);
        }

        @Test
        @DisplayName("Проверяем добавление лайков от разных пользователей - должны все сохраниться")
        void addLikeIfNotExists_Should_Allow_Multiple_Users_To_Like_Test() {
            // given, when
            storage.addLikeIfNotExists(filmId1, userId1);
            storage.addLikeIfNotExists(filmId1, userId2);
            // then
            assertThat(storage.getLikesCountOfFilm(filmId1)).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Тесты getLikesCountOfFilm")
    class GetLikesCountOfFilmTests {

        @Test
        @DisplayName("Проверяем получение лайков у фильма")
        void getLikesCountOfFilm_Should_Return_Correct_Likes_Count_For_Film_With_Likes_Test() {
            // given
            storage.addLikeIfNotExists(filmId1, userId1);
            storage.addLikeIfNotExists(filmId1, userId2);
            // when, then
            assertThat(storage.getLikesCountOfFilm(filmId1)).isEqualTo(2);
        }

        @Test
        @DisplayName("Проверяем получение нуля для фильма без лайков")
        void getLikesCountOfFilm_Should_Return_Zero_For_Film_Without_Likes_Test() {
            // given, when, then
            assertThat(storage.getLikesCountOfFilm(filmId2)).isZero();
        }

        @Test
        @DisplayName("Проверяем получение нуля для несуществующего фильма")
        void getLikesCountOfFilm_Should_Return_Zero_For_Non_Existing_Film_Test() {
            // given, when, then
            assertThat(storage.getLikesCountOfFilm(666L)).isZero();
        }
    }

    @Nested
    @DisplayName("Тесты getLikesCountByFilmsIds")
    class GetLikesCountByFilmsIdsTests {

        @Test
        @DisplayName("Проверяем получение количества лайков для нескольких фильмов")
        void getLikesCountByFilmsIds_Should_Return_Correct_Map_For_Multiple_Films_Test() {
            // given
            storage.addLikeIfNotExists(filmId1, userId1);
            storage.addLikeIfNotExists(filmId1, userId2);
            storage.addLikeIfNotExists(filmId2, userId1);
            Set<Long> filmIds = Set.of(filmId1, filmId2, filmId3);
            // when
            Map<Long, Integer> result = storage.getLikesCountByFilmsIds(filmIds);
            // then
            assertThat(result)
                    .containsEntry(filmId1, 2)
                    .containsEntry(filmId2, 1)
                    .containsEntry(filmId3, 0);
        }

        @Test
        @DisplayName("Проверяем получение пустого набора лайков для пустого набора ID")
        void getLikesCountByFilmsIds_Should_Return_Empty_Map_For_Empty_FilmIds_Test() {
            // given, when
            Map<Long, Integer> result = storage.getLikesCountByFilmsIds(Set.of());
            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты getFilmsIdsLikedByUser")
    class GetFilmsIdsLikedByUserTests {
        @Test
        @DisplayName("Проверяем получение списка фильмов, которым пользователь поставил лайк")
        void getFilmsIdsLikedByUser_Should_Return_Correct_When_User_Has_Likes_Test() {
            // given
            storage.addLikeIfNotExists(filmId1, userId1);
            storage.addLikeIfNotExists(filmId2, userId1);
            storage.addLikeIfNotExists(filmId3, userId2);
            // when
            Set<Long> result = storage.getFilmsIdsLikedByUser(userId1);
            // then
            assertThat(result)
                    .hasSize(2)
                    .containsExactlyInAnyOrder(filmId1, filmId2)
                    .doesNotContain(filmId3);
        }

        @Test
        @DisplayName("Проверяем получение пустого набора для пользователя без лайков")
        void getFilmsIdsLikedByUser_Should_Return_Empty_Set_For_User_Without_Likes_Test() {
            // given, when
            Set<Long> result = storage.getFilmsIdsLikedByUser(userId3);
            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Проверяем получение пустого набора для несуществующего пользователя")
        void getFilmsIdsLikedByUser_Should_Return_Empty_Set_For_Non_Existing_User_Test() {
            // given, when
            Set<Long> result = storage.getFilmsIdsLikedByUser(666L);
            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты getTopPopularFilmsIds")
    class GetTopPopularFilmsIdsTests {
        @Test
        @DisplayName("Проверяем получение топ‑фильмов по лайкам — должен вернуть корректную карту с сортировкой")
        void getTopPopularFilmsIds_Should_Return_Correct_Sorted_Map_Test() {
            // given
            storage.addLikeIfNotExists(filmId1, userId1);
            storage.addLikeIfNotExists(filmId1, userId2);
            storage.addLikeIfNotExists(filmId1, userId3);
            storage.addLikeIfNotExists(filmId2, userId1);
            storage.addLikeIfNotExists(filmId3, userId3);
            storage.addLikeIfNotExists(filmId3, userId1);
            // when
            LinkedHashMap<Long, Integer> result = storage.getTopPopularFilmsIds(3);
            // then
            assertThat(result)
                    .hasSize(3)
                    .containsKey(filmId1)
                    .containsKey(filmId3)
                    .containsKey(filmId2);
            List<Long> orderedKeys = new ArrayList<>(result.keySet());
            assertThat(orderedKeys).containsExactly(filmId1, filmId3, filmId2);
        }

        @Test
        @DisplayName("Проверяем получение пустого результата при пустом запросе (count/limit = 0)")
        void getTopPopularFilmsIds_Should_Return_Empty_Map_For_Limit_Zero_Test() {
            // given, when
            LinkedHashMap<Long, Integer> result = storage.getTopPopularFilmsIds(0);
            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Проверяем получение всех фильмов при count/limit больше чем количество записей")
        void getTopPopularFilmsIds_Should_Return_All_Films_When_Limit_Is_Large_Test() {
            // given
            storage.addLikeIfNotExists(filmId1, userId1);
            storage.addLikeIfNotExists(filmId2, userId1);
            storage.addLikeIfNotExists(filmId2, userId2);
            // when
            LinkedHashMap<Long, Integer> result = storage.getTopPopularFilmsIds(100);
            // then
            assertThat(result)
                    .hasSize(2)
                    .containsEntry(filmId2, 2)
                    .containsEntry(filmId1, 1);
            List<Long> orderedKeys = new ArrayList<>(result.keySet());
            assertThat(orderedKeys).containsExactly(filmId2, filmId1);
        }
    }

    @Nested
    @DisplayName("Тесты deleteLikeFromFilmIfExists")
    class DeleteLikeFromFilmIfExistsTests {

        @Test
        @DisplayName("Проверяем удаление существующего лайка — должен вернуть true")
        void deleteLikeFromFilmIfExists_Should_Delete_Existing_Like_And_Return_True_Test() {
            // given
            storage.addLikeIfNotExists(filmId1, userId1);
            // when
            boolean deleted = storage.deleteLikeFromFilmIfExists(filmId1, userId1);
            // then
            assertThat(deleted).isTrue();
            assertThat(storage.getLikesCountOfFilm(filmId1)).isZero();
        }

        @Test
        @DisplayName("Проверяем попытку удаления несуществующего лайка — должен вернуть false")
        void deleteLikeFromFilmIfExists_Should_Return_False_For_Non_Existing_Like_Test() {
            // given, when
            boolean deleted = storage.deleteLikeFromFilmIfExists(filmId1, userId1);
            // then
            assertThat(deleted).isFalse();
        }

        @Test
        @DisplayName("Проверяем повторное удаление — должен вернуть false после первого удаления")
        void deleteLikeFromFilmIfExists_Should_Return_False_On_Second_Delete_Attempt_Test() {
            // given
            storage.addLikeIfNotExists(filmId1, userId1);
            storage.deleteLikeFromFilmIfExists(filmId1, userId1);
            // when
            boolean deletedAgain = storage.deleteLikeFromFilmIfExists(filmId1, userId1);
            //then
            assertThat(deletedAgain).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты deleteAllLikesFromFilmIfExists")
    class DeleteAllLikesFromFilmIfExistsTests {

        @Test
        @DisplayName("Проверяем удаление всех лайков у фильма — должен вернуть true")
        void deleteAllLikesFromFilmIfExists_Should_Delete_All_Likes_And_Return_True_Test() {
            // given
            storage.addLikeIfNotExists(filmId1, userId1);
            storage.addLikeIfNotExists(filmId1, userId2);
            storage.addLikeIfNotExists(filmId1, userId3);
            // when
            boolean deletedAll = storage.deleteAllLikesFromFilmIfExists(filmId1);
            // then
            assertThat(deletedAll).isTrue();
            assertThat(storage.getLikesCountOfFilm(filmId1)).isZero();
        }

        @Test
        @DisplayName("Проверяем удаление лайков у фильма без лайков — должен вернуть false")
        void deleteAllLikesFromFilmIfExists_Should_Return_False_For_Film_Without_Likes_Test() {
            // given, when
            boolean deletedAll = storage.deleteAllLikesFromFilmIfExists(filmId2);
            // then
            assertThat(deletedAll).isFalse();
        }

        @Test
        @DisplayName("Проверяем удаление лайков у несуществующего фильма — должен вернуть false")
        void deleteAllLikesFromFilmIfExists_Should_Return_False_For_Non_Existing_Film_Test() {
            // given, when
            boolean deletedAll = storage.deleteAllLikesFromFilmIfExists(999L);
            // then
            assertThat(deletedAll).isFalse();
        }
    }
}