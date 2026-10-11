package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.dal.GenreDbStorage;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.dto.NewGenreRequest;
import ru.yandex.practicum.filmorate.dto.UpdateGenreRequest;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("GenreService Тесты")
class GenreServiceTest {

    private final GenreService genreService;
    private final GenreDbStorage genreStorage;

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
        // Собираем id тестовых жанров напрямую из БД (чтобы не словить кэш),
        // но удаляем через storage — так сработает @CacheEvict и кэш genres очистится.
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM genres WHERE name LIKE 'TestGenre%'", Long.class);
        ids.forEach(genreStorage::delete);
    }

    private NewGenreRequest newRequest(String name) {
        NewGenreRequest r = new NewGenreRequest();
        r.setName(name);
        return r;
    }

    private UpdateGenreRequest updateRequest(Long id, String name) {
        UpdateGenreRequest r = new UpdateGenreRequest();
        r.setId(id);
        r.setName(name);
        return r;
    }

    @Nested
    @DisplayName("Тесты findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Существующий жанр")
        void findById_Should_ReturnGenre_Test() {
            assertThat(genreService.findById(1L).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Несуществующий → NotFoundException")
        void findById_Should_ThrowNotFound_Test() {
            assertThatThrownBy(() -> genreService.findById(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Тесты findAll()")
    class FindAllTests {

        @Test
        @DisplayName("Возвращает 6 предзаполненных жанров")
        void findAll_Should_ReturnAllPreloaded_Test() {
            assertThat(genreService.findAll()).hasSize(6);
        }
    }

    @Nested
    @DisplayName("Тесты getGenresDto()")
    class GetGenresDtoTests {

        @Test
        @DisplayName("null → IllegalArgumentException")
        void getGenresDto_Should_Throw_ForNull_Test() {
            assertThatThrownBy(() -> genreService.getGenresDto(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("пустое множество → пустой список")
        void getGenresDto_Should_ReturnEmpty_ForEmpty_Test() {
            assertThat(genreService.getGenresDto(Set.of())).isEmpty();
        }

        @Test
        @DisplayName("несуществующий id → NotFoundException")
        void getGenresDto_Should_ThrowNotFound_Test() {
            assertThatThrownBy(() -> genreService.getGenresDto(Set.of(1L, 999L)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("все существуют → список DTO")
        void getGenresDto_Should_ReturnAll_Test() {
            List<GenreDto> result = genreService.getGenresDto(Set.of(1L, 2L));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(GenreDto::getId)
                    .containsExactlyInAnyOrder(1L, 2L);
        }
    }

    @Nested
    @DisplayName("Тесты create()")
    class CreateTests {

        @Test
        @DisplayName("Возвращает DTO с id и именем")
        void create_Should_ReturnGenreWithId_Test() {
            GenreDto created = genreService.create(newRequest("TestGenreCreate"));

            assertThat(created.getId()).isNotNull();
            assertThat(created.getName()).isEqualTo("TestGenreCreate");
            assertThat(genreService.findById(created.getId()).getName()).isEqualTo("TestGenreCreate");
        }

        @Test
        @DisplayName("Дубль имени → ValidationException")
        void create_Should_ThrowValidationException_OnDuplicate_Test() {
            genreService.create(newRequest("TestGenreCreateDup"));

            assertThatThrownBy(() -> genreService.create(newRequest("TestGenreCreateDup")))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Новый жанр виден в findAll()")
        void create_Should_BeVisibleInFindAll_Test() {
            genreService.create(newRequest("TestGenreCreateVisible"));

            assertThat(genreService.findAll())
                    .extracting(GenreDto::getName)
                    .contains("TestGenreCreateVisible");
        }
    }

    @Nested
    @DisplayName("Тесты update()")
    class UpdateTests {

        @Test
        @DisplayName("Меняет имя")
        void update_Should_ChangeName_Test() {
            GenreDto created = genreService.create(newRequest("TestGenreUpdateOld"));

            GenreDto updated = genreService.update(updateRequest(created.getId(), "TestGenreUpdateNew"));

            assertThat(updated.getId()).isEqualTo(created.getId());
            assertThat(updated.getName()).isEqualTo("TestGenreUpdateNew");
            assertThat(genreService.findById(created.getId()).getName()).isEqualTo("TestGenreUpdateNew");
        }

        @Test
        @DisplayName("Несуществующий id → NotFoundException")
        void update_Should_ThrowNotFound_Test() {
            assertThatThrownBy(() -> genreService.update(updateRequest(999L, "TestGenreUpdateX")))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Имя занято другим → ValidationException")
        void update_Should_ThrowValidationException_WhenNameTaken_Test() {
            GenreDto first = genreService.create(newRequest("TestGenreUpdateFirst"));
            genreService.create(newRequest("TestGenreUpdateSecond"));

            assertThatThrownBy(() ->
                    genreService.update(updateRequest(first.getId(), "TestGenreUpdateSecond")))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Обновление без смены имени — не конфликтует с самим собой")
        void update_Should_AllowSameName_Test() {
            GenreDto created = genreService.create(newRequest("TestGenreUpdateSame"));

            GenreDto updated = genreService.update(updateRequest(created.getId(), "TestGenreUpdateSame"));

            assertThat(updated.getName()).isEqualTo("TestGenreUpdateSame");
        }
    }

    @Nested
    @DisplayName("Тесты delete()")
    class DeleteTests {

        @Test
        @DisplayName("Удаляет существующий")
        void delete_Should_Remove_Test() {
            GenreDto created = genreService.create(newRequest("TestGenreDelete"));

            genreService.delete(created.getId());

            assertThatThrownBy(() -> genreService.findById(created.getId()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Несуществующий → NotFoundException")
        void delete_Should_ThrowNotFound_Test() {
            assertThatThrownBy(() -> genreService.delete(999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Удалённый жанр исчезает из findAll()")
        void delete_Should_DisappearFromFindAll_Test() {
            GenreDto created = genreService.create(newRequest("TestGenreDeleteVisible"));

            genreService.delete(created.getId());

            assertThat(genreService.findAll())
                    .extracting(GenreDto::getName)
                    .doesNotContain("TestGenreDeleteVisible");
        }
    }
}