package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Genre;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты GenreDbStorage")
class GenreDbStorageTest {

    @Autowired
    private GenreDbStorage storage;
    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @DisplayName("Тесты evictCache()")
    class EvictCacheTests {
        @Test
        @DisplayName("Проверяем отсутствие исключений при вызове метода очистки кэша")
        void evictCache_Should_Execute_Without_Exceptions_Test() {
            // given, when, then
            assertThatCode(() -> storage.evictCache())
                    .doesNotThrowAnyException();
        }
    }
}
