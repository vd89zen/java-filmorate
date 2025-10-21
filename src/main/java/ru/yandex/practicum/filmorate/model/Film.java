package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Builder
@Data
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor
public class Film {
    Long id;
    @NotBlank(message = "Не указано название фильма.")
    String name;
    @Size(min = 2, max = 200, message = "Описание должно быть от 2 до 200 символов.")
    String description;
    @NotNull(message = "Не указана дата релиза фильма.")
    LocalDate releaseDate;
    @Positive(message = "Длительность фильма должна быть положительным числом.")
    int duration;
}
