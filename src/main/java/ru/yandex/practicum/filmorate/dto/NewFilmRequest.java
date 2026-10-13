package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewFilmRequest {
    @NotBlank(message = "Не указано название фильма.")
    private String name;

    @NotNull(message = "Описание фильма не может быть null.")
    @Size(min = 2, max = 200, message = "Описание должно быть от 2 до 200 символов.")
    private String description;

    @NotNull(message = "Дата релиза фильма не может быть null.")
    private LocalDate releaseDate;

    @NotNull(message = "Длительность фильма не может быть null.")
    @Positive(message = "Длительность фильма должна быть положительным числом.")
    private Integer duration;

    private RatingMpaaId mpa;
    private RatingRarsId rars;
    private Set<GenreId> genres = new HashSet<>();
    private Set<DirectorId> directors = new HashSet<>();

    @AssertTrue(message = "Должен быть указан хотя бы один рейтинг (MPAA или RARS).")
    private boolean isAtLeastOneRatingPresent() {
        return mpa != null || rars != null;
    }
}
