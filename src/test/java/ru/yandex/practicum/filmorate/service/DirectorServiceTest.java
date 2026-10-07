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

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты DirectorService")
class DirectorServiceTest {

    private final DirectorService directorService;

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
        jdbcTemplate.execute("DELETE FROM film_directors");
        jdbcTemplate.execute("DELETE FROM directors");
    }

    private NewDirectorRequest newRequest(String name) {
        NewDirectorRequest r = new NewDirectorRequest();
        r.setName(name);
        return r;
    }

    @Nested
    @DisplayName("Тесты create()")
    class CreateTests {

        @Test
        @DisplayName("Возвращает DTO с id")
        void create_Should_ReturnDirectorWithId_Test() {
            DirectorDto dto = directorService.create(newRequest("Nolan"));

            assertThat(dto.getId()).isNotNull();
            assertThat(dto.getName()).isEqualTo("Nolan");
        }

        @Test
        @DisplayName("Дубль имени → ValidationException")
        void create_Should_ThrowValidationException_OnDuplicate_Test() {
            directorService.create(newRequest("Nolan"));

            assertThatThrownBy(() -> directorService.create(newRequest("Nolan")))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("Тесты update()")
    class UpdateTests {

        @Test
        @DisplayName("Меняет имя")
        void update_Should_ChangeName_Test() {
            DirectorDto created = directorService.create(newRequest("Old"));

            UpdateDirectorRequest req = new UpdateDirectorRequest();
            req.setId(created.getId());
            req.setName("New");

            assertThat(directorService.update(req).getName()).isEqualTo("New");
        }

        @Test
        @DisplayName("Несуществующий id → NotFoundException")
        void update_Should_ThrowNotFound_Test() {
            UpdateDirectorRequest req = new UpdateDirectorRequest();
            req.setId(999L);
            req.setName("X");

            assertThatThrownBy(() -> directorService.update(req))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Имя занято другим → ValidationException")
        void update_Should_ThrowValidationException_WhenNameTaken_Test() {
            DirectorDto first = directorService.create(newRequest("First"));
            directorService.create(newRequest("Second"));

            UpdateDirectorRequest req = new UpdateDirectorRequest();
            req.setId(first.getId());
            req.setName("Second");

            assertThatThrownBy(() -> directorService.update(req))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("Тесты delete()")
    class DeleteTests {

        @Test
        @DisplayName("Удаляет существующего")
        void delete_Should_Remove_Test() {
            DirectorDto created = directorService.create(newRequest("X"));
            directorService.delete(created.getId());

            assertThatThrownBy(() -> directorService.findById(created.getId()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Несуществующий → NotFoundException")
        void delete_Should_ThrowNotFound_Test() {
            assertThatThrownBy(() -> directorService.delete(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Тесты findById() и findAll()")
    class FindTests {

        @Test
        @DisplayName("findById: возвращает DTO")
        void findById_Should_ReturnDto_Test() {
            DirectorDto created = directorService.create(newRequest("A"));
            assertThat(directorService.findById(created.getId()).getName()).isEqualTo("A");
        }

        @Test
        @DisplayName("findById: несуществующий → NotFoundException")
        void findById_Should_ThrowNotFound_Test() {
            assertThatThrownBy(() -> directorService.findById(999L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("findAll: пустой список")
        void findAll_Should_ReturnEmpty_Test() {
            assertThat(directorService.findAll()).isEmpty();
        }

        @Test
        @DisplayName("findAll: сортировка по id")
        void findAll_Should_ReturnSorted_Test() {
            directorService.create(newRequest("A"));
            directorService.create(newRequest("B"));

            assertThat(directorService.findAll())
                    .extracting(DirectorDto::getName)
                    .containsExactly("A", "B");
        }
    }

    @Nested
    @DisplayName("Тесты getDirectorsDto()")
    class GetDirectorsDtoTests {

        @Test
        @DisplayName("null → IllegalArgumentException")
        void getDirectorsDto_Should_ThrowIllegalArgument_ForNull_Test() {
            assertThatThrownBy(() -> directorService.getDirectorsDto(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("пустое множество → пустой список")
        void getDirectorsDto_Should_ReturnEmptyList_ForEmpty_Test() {
            assertThat(directorService.getDirectorsDto(Set.of())).isEmpty();
        }

        @Test
        @DisplayName("несуществующий id → NotFoundException")
        void getDirectorsDto_Should_ThrowNotFound_ForMissing_Test() {
            DirectorDto existing = directorService.create(newRequest("A"));

            assertThatThrownBy(() -> directorService.getDirectorsDto(
                    Set.of(existing.getId(), 999L)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("все существуют → список DTO")
        void getDirectorsDto_Should_ReturnAll_Test() {
            DirectorDto a = directorService.create(newRequest("A"));
            DirectorDto b = directorService.create(newRequest("B"));

            assertThat(directorService.getDirectorsDto(Set.of(a.getId(), b.getId())))
                    .extracting(DirectorDto::getName)
                    .containsExactlyInAnyOrder("A", "B");
        }
    }

    @Nested
    @DisplayName("Тесты checkDirectorExists()")
    class CheckDirectorExistsTests {

        @Test
        @DisplayName("существующий — не бросает")
        void checkDirectorExists_Should_NotThrow_ForExisting_Test() {
            DirectorDto dto = directorService.create(newRequest("A"));
            assertThatCode(() -> directorService.checkDirectorExists(dto.getId()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("несуществующий → NotFoundException")
        void checkDirectorExists_Should_Throw_ForMissing_Test() {
            assertThatThrownBy(() -> directorService.checkDirectorExists(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}