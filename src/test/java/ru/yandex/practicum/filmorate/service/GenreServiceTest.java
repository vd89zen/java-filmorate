package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты GenreService")
class GenreServiceTest {

    private final GenreService genreService;

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
}