package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDirectorRequest {
    @NotNull
    @Positive
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String name;
}