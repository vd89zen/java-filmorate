package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RatingMpaa {
    @NotNull(message = "ID рейтинга не может быть null.")
    @Positive(message = "ID рейтинга не может быть меньше 1.")
    Long id;
    @NotNull(message = "Имя рейтинга фильма не может быть null.")
    String name;

    @JsonCreator
    public RatingMpaa(@JsonProperty("id") Long id, @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }
}
