package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.model.RatingMpaa;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewFilmRequest {
    @NotBlank(message = "Не указано название фильма.")
    String name;
    @NotNull(message = "Описание фильма не может быть null.")
    @Size(min = 2, max = 200, message = "Описание должно быть от 2 до 200 символов.")
    String description;
    @NotNull(message = "Дата релиза фильма не может быть null.")
    LocalDate releaseDate;
    @NotNull(message = "Длительность фильма не может быть null.")
    @Positive(message = "Длительность фильма должна быть положительным числом.")
    Integer duration;
    @NotNull(message = "Рейтинг фильма не может быть null.")
    RatingMpaa mpa;
    Set<GenreDto> genres = new LinkedHashSet<>();
}
