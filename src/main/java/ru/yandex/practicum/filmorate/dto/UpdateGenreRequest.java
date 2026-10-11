package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateGenreRequest {
    @NotNull
    @Positive
    private Long id;

    @NotBlank(message = "Не указано название жанра.")
    @Size(max = 50, message = "Название жанра не должно превышать 50 символов.")
    private String name;
}