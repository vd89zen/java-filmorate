package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("RatingRarsService Тесты")
class RatingRarsServiceTest {

    private final RatingRarsService ratingRarsService;

    @Nested
    @DisplayName("Тесты findAll()")
    class FindAllTests {

        @Test
        @DisplayName("Возвращает 5 предзаполненных рейтингов")
        void findAll_Should_ReturnAllPreloaded_Test() {
            assertThat(ratingRarsService.findAll()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("Тесты getRatingRarsDtoById()")
    class GetByIdTests {

        @Test
        @DisplayName("Существующий рейтинг")
        void getById_Should_Return_Test() {
            assertThat(ratingRarsService.getRatingRarsDtoById(1L).getId()).isEqualTo(1L);
            assertThat(ratingRarsService.getRatingRarsDtoById(1L).getName()).isEqualTo("0+");
        }

        @Test
        @DisplayName("Несуществующий → NotFoundException")
        void getById_Should_ThrowNotFound_Test() {
            assertThatThrownBy(() -> ratingRarsService.getRatingRarsDtoById(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}