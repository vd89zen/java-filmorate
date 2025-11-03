package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Builder
@Data
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor
@NoArgsConstructor
public class Film {
    Long id;
    @NotBlank(message = "Не указано название фильма.")
    String name;
    @Size(min = 2, max = 200, message = "Описание должно быть от 2 до 200 символов.")
    String description;
    @NotNull(message = "Не указана дата релиза фильма.")
    LocalDate releaseDate;
    @Positive(message = "Длительность фильма должна быть положительным числом.")
    int duration;
    @Builder.Default
    @Setter(AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Set<Long> likes = new HashSet<>();

    public Film selfCopy() {
        return Film.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .releaseDate(this.releaseDate)
                .duration(this.duration)
                .likes(new HashSet<>(this.likes))
                .build();
    }
}
