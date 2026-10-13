package ru.yandex.practicum.filmorate.dal;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.model.RatingRars;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("RatingRarsDbStorage Тесты")
class RatingRarsDbStorageTest {

    @Autowired
    private RatingRarsDbStorage storage;

    @Nested
    @DisplayName("Тесты findAll()")
    class FindAllTests {
        @Test
        @DisplayName("Получаем все рейтинги RARS в порядке ID")
        void findAll_Should_Return_All_Ratings_In_Id_Order_Test() {
            List<RatingRars> allRatings = storage.findAll();

            assertThat(allRatings)
                    .hasSize(5)
                    .extracting(RatingRars::getName)
                    .containsExactly("0+", "6+", "12+", "16+", "18+");
        }
    }

    @Nested
    @DisplayName("Тесты findById()")
    class FindByIdTests {
        @Test
        @DisplayName("Получаем Optional с рейтингом RARS по существующему ID")
        void findById_Should_Return_Optional_With_Rating_Test() {
            Optional<RatingRars> found = storage.findById(1L);

            assertThat(found)
                    .isPresent()
                    .get()
                    .hasFieldOrPropertyWithValue("name", "0+");
        }

        @Test
        @DisplayName("Получаем пустой Optional по несуществующему ID")
        void findById_Should_Return_Empty_Optional_For_Non_Exists_Id_Test() {
            Optional<RatingRars> found = storage.findById(666L);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Получаем пустой Optional по NULL ID")
        void findById_Should_Return_Empty_Optional_For_Null_Id_Test() {
            Optional<RatingRars> found = storage.findById(null);

            assertThat(found).isEmpty();
        }
    }
}