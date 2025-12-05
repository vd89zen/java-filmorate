package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.dto.GenreId;
import ru.yandex.practicum.filmorate.model.Genre;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GenreMapper {

    public static List<GenreDto> toDtoSet(List<Genre> genres) {
        if (genres.isEmpty()) {
            return List.of();
        }

        return genres.stream()
                .map(genre -> new GenreDto(
                        genre.getId(),
                        genre.getName()))
                .collect(Collectors.toUnmodifiableList());
    }

    public static Set<Long> mapGenreIdToIds(Set<GenreId> genres) {
        return genres.stream()
                .map(GenreId::getId)
                .collect(Collectors.toSet());
    }

    public static GenreDto toDto(Genre genre) {
        return new GenreDto(
                genre.getId(),
                genre.getName()
        );
    }
}
