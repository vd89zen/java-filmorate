package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RatingMpaaId {
    @NotNull(message = "ID рейтинга не может быть null.")
    @Positive(message = "ID рейтинга не может быть меньше 1.")
    Long id;

    @JsonCreator
    public RatingMpaaId(@JsonProperty("id") Long id) {
        this.id = id;
    }
}
