package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewReviewRequest {
    @NotBlank
    @Size(max = 2000)
    private String content;

    @NotNull
    private Boolean isPositive;

    @NotNull
    @Positive
    private Long filmId;
}