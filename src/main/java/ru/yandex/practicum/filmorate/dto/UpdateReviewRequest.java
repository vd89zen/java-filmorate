package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateReviewRequest {
    @NotNull
    @Positive
    private Long reviewId;

    @Size(max = 2000)
    private String content;

    private Boolean isPositive;

    public boolean hasContent() {
        return content != null;
    }

    public boolean hasIsPositive() {
        return isPositive != null;
    }
}