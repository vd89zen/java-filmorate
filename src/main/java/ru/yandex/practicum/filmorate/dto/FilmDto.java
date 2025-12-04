package ru.yandex.practicum.filmorate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

@Builder
@Data
@EqualsAndHashCode(of = {"id"})
public class FilmDto {
    Long id;
    String name;
    String description;
    LocalDate releaseDate;
    Integer duration;
    RatingMpaaDto mpa;
    @Builder.Default
    Set<GenreDto> genres = new TreeSet<>(Comparator.comparing(GenreDto::getId));
    Integer likesCount;
}
