package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

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
    RatingMpaa ratingMpaa;
    @Builder.Default
    Set<Genre> genres = new LinkedHashSet<>();
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Builder.Default
    Integer likesCount = 0;
}
