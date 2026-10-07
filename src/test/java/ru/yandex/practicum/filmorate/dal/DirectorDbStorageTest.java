package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты DirectorDbStorage")
class DirectorDbStorageTest {

    private final DirectorDbStorage directorStorage;

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

    private Director newDirector(String name) {
        return Director.builder().name(name).build();
    }

    @Nested
    @DisplayName("Тесты create() и findById()")
    class CreateAndFindTests {

        @Test
        @DisplayName("Создание: возвращает id, сохраняет имя")
        void create_Should_ReturnDirectorWithId_Test() {
            Director saved = directorStorage.create(newDirector("Nolan"));

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Nolan");
            assertThat(directorStorage.findById(saved.getId()))
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("name", "Nolan");
        }

        @Test
        @DisplayName("findById: пустой Optional для несуществующего")
        void findById_Should_ReturnEmptyOptional_ForNonExisting_Test() {
            assertThat(directorStorage.findById(999L)).isEmpty();
        }

        @Test
        @DisplayName("findAll: возвращает всех, отсортированных по id")
        void findAll_Should_ReturnAllSortedById_Test() {
            directorStorage.create(newDirector("A"));
            directorStorage.create(newDirector("B"));

            assertThat(directorStorage.findAll())
                    .hasSize(2)
                    .extracting(Director::getName)
                    .containsExactly("A", "B");
        }
    }

    @Nested
    @DisplayName("Тесты update() и delete()")
    class UpdateDeleteTests {

        @Test
        @DisplayName("update: меняет имя")
        void update_Should_ChangeName_Test() {
            Director saved = directorStorage.create(newDirector("Old"));
            saved.setName("New");
            directorStorage.update(saved);

            assertThat(directorStorage.findById(saved.getId()))
                    .get()
                    .hasFieldOrPropertyWithValue("name", "New");
        }

        @Test
        @DisplayName("delete: true → false при повторном вызове")
        void delete_Should_ReturnTrueThenFalse_Test() {
            Director saved = directorStorage.create(newDirector("X"));
            assertThat(directorStorage.delete(saved.getId())).isTrue();
            assertThat(directorStorage.delete(saved.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты findByIds()")
    class FindByIdsTests {

        @Test
        @DisplayName("Возвращает только существующих, отсортированных по id")
        void findByIds_Should_ReturnOnlyRequestedSortedById_Test() {
            Director a = directorStorage.create(newDirector("A"));
            Director b = directorStorage.create(newDirector("B"));
            directorStorage.create(newDirector("C"));

            List<Director> result = directorStorage.findByIds(Set.of(b.getId(), a.getId()));

            assertThat(result)
                    .extracting(Director::getName)
                    .containsExactly("A", "B");
        }

        @Test
        @DisplayName("Несуществующие id игнорируются")
        void findByIds_Should_IgnoreMissingIds_Test() {
            Director a = directorStorage.create(newDirector("A"));

            assertThat(directorStorage.findByIds(Set.of(a.getId(), 999L)))
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("Тесты isDirectorExists() и isNameAlreadyUse()")
    class ChecksTests {

        @Test
        @DisplayName("isDirectorExists: true для существующего, false для пропавшего")
        void isDirectorExists_Should_ReturnTrueThenFalse_Test() {
            Director saved = directorStorage.create(newDirector("Y"));
            assertThat(directorStorage.isDirectorExists(saved.getId())).isTrue();
            assertThat(directorStorage.isDirectorExists(999L)).isFalse();
        }

        @Test
        @DisplayName("isNameAlreadyUse: true для занятого имени")
        void isNameAlreadyUse_Should_ReturnTrue_ForExisting_Test() {
            directorStorage.create(newDirector("Z"));
            assertThat(directorStorage.isNameAlreadyUse("Z")).isTrue();
            assertThat(directorStorage.isNameAlreadyUse("Q")).isFalse();
        }
    }
}