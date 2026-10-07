package ru.yandex.practicum.filmorate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.util.*;

@Builder
@Data
@EqualsAndHashCode(of = {"id"})
public class FilmDto {
    private Long id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private Integer duration;
    private RatingMpaaDto mpa;
    @Builder.Default
    private Set<GenreDto> genres = new TreeSet<>(Comparator.comparing(GenreDto::getId));
    private Integer likesCount;
    @Builder.Default
    private Set<DirectorDto> directors = new TreeSet<>(Comparator.comparing(DirectorDto::getId));
}
