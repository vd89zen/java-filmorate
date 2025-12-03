package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import ru.yandex.practicum.filmorate.model.RatingMpaa;
import java.time.LocalDate;
import java.util.Set;

@Data
@EqualsAndHashCode(of = "id")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateFilmRequest {
    @NotNull(message = "При обновлении ID фильма не может быть null.")
    @Positive(message = "ID фильма не может быть меньше 1.")
    Long id;
    @Pattern(regexp = ".*\\S+.*",
            message = "(Если поле не null: название фильма должно содержать хотя бы один непробельный символ.")
    String name;
    @Size(min = 2, max = 200, message = "Если поле не null: описание должно быть от 2 до 200 символов.")
    String description;
    LocalDate releaseDate;
    @Positive(message = "Если поле не null: длительность фильма должна быть положительным числом.")
    Integer duration;
    RatingMpaa mpa;
    @Size(min = 1, message = "Если поле не null: должен быть указан хотя бы один жанр.")
    Set<GenreDto> genres;

    public boolean hasName() {
        return name != null;
    }

    public boolean hasDescription() {
        return description != null;
    }

    public boolean hasReleaseDate() {
        return releaseDate != null;
    }

    public boolean hasDuration() {
        return duration != null;
    }

    public boolean hasMpa() {
        return mpa != null;
    }

    public boolean hasGenres() {
        return genres != null;
    }
}
