package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.model.Film;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilmMapper {

    public static Film mapToFilm(NewFilmRequest newFilmRequest) {
        return Film.builder()
                .name(newFilmRequest.getName())
                .description(newFilmRequest.getDescription())
                .releaseDate(newFilmRequest.getReleaseDate())
                .duration(newFilmRequest.getDuration())
                .mpa(newFilmRequest.getMpa())
                .rars(newFilmRequest.getRars())
                .build();
    }

    public static FilmDto mapToDto(Film film) {
        return FilmDto.builder()
                .id(film.getId())
                .name(film.getName())
                .description(film.getDescription())
                .releaseDate(film.getReleaseDate())
                .duration(film.getDuration())
                .mpa(film.getMpa() == null
                        ? null
                        : new RatingMpaaDto(film.getMpa().getId(), null))
                .rars(film.getRars() == null
                        ? null
                        : new RatingRarsDto(film.getRars().getId(), null))
                .build();
    }

    public static void updateFilmFields(Film film, UpdateFilmRequest request) {
        if (request.hasName()) {
            film.setName(request.getName());
        }

        if (request.hasDescription()) {
            film.setDescription(request.getDescription());
        }

        if (request.hasReleaseDate()) {
            film.setReleaseDate(request.getReleaseDate());
        }

        if (request.hasDuration()) {
            film.setDuration(request.getDuration());
        }

        if (request.hasMpa()) {
            film.setMpa(request.getMpa());
        }

        if (request.hasRars()) {
            film.setRars(request.getRars());
        }
    }
}