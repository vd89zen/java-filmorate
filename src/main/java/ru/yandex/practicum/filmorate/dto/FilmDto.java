package ru.yandex.practicum.filmorate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.RatingMpaa;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Builder
@Data
@EqualsAndHashCode(of = {"id"})
public class FilmDto {
    Long id;
    String name;
    String description;
    LocalDate releaseDate;
    Integer duration;
    RatingMpaa mpa;
    @Builder.Default
    Set<Genre> genres = new LinkedHashSet<>();
    Integer likesCount;
}
