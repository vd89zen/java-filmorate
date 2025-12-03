package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.RatingMpaa;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты RatingMpaaDbStorage")
class RatingMpaaDbStorageTest {

    @Autowired
    private RatingMpaaDbStorage storage;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("Тесты findAll()")
    class FindAllTests {
        @Test
        @DisplayName("Получаем все рейтинги MPAA в порядке ID")
        void findAll_Should_Return_All_Ratings_In_Id_Order_Test() {
            // given, when
            List<RatingMpaa> allRatings = storage.findAll();
            // then
            System.out.println(allRatings);
            assertThat(allRatings)
                    .hasSize(5);
        }
    }

    @Nested
    @DisplayName("Тесты findById()")
    class FindByIdTests {
        @Test
        @DisplayName("Получаем Optional с рейтингом MPAA по существующему ID")
        void findById_Should_Return_Optional_With_Rating_Test() {
            // given, when
            Optional<RatingMpaa> found = storage.findById(1L);
            // then
            System.out.println(found);
            assertThat(found)
                    .isPresent();
        }

        @Test
        @DisplayName("Получаем пустой Optional по несуществующему ID")
        void findById_Should_Return_Empty_Optional_For_Non_Exists_Id_Test() {
            // given, when
            Optional<RatingMpaa> found = storage.findById(666L);
            // then
            System.out.println(found);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Получаем пустой Optional по NULL ID")
        void findById_Should_Return_Empty_Optional_For_Null_Id_Test() {
            // given, when
            Optional<RatingMpaa> found = storage.findById(null);
            // then
            System.out.println(found);
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты evictCache()")
    class EvictCacheTests {
        @Test
        @DisplayName("Проверяем отсутствие исключений при очистке кэша")
        void evictCache_Should_Execute_Without_Exceptions_Test() {
            // given, when, then
            assertThatCode(() -> storage.evictCache())
                    .doesNotThrowAnyException();
        }
    }
}
