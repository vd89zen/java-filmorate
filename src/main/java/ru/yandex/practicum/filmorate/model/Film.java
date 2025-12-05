package ru.yandex.practicum.filmorate.model;

import lombok.*;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import java.time.LocalDate;

@Builder
@Data
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor
@NoArgsConstructor
public class Film {
    Long id;
    String name;
    String description;
    LocalDate releaseDate;
    int duration;
    RatingMpaaId mpa;
}
