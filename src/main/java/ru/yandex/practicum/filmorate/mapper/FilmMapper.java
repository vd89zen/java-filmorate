package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.model.Film;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilmMapper {

    public static Film mapToFilm(NewFilmRequest newFilmRequest) {
        Film film = Film.builder()
                .name(newFilmRequest.getName())
                .description(newFilmRequest.getDescription())
                .releaseDate(newFilmRequest.getReleaseDate())
                .duration(newFilmRequest.getDuration())
                .mpa(newFilmRequest.getMpa())
                .build();

        return film;
    }

    public static FilmDto mapToDto(Film film) {
        FilmDto filmDto = FilmDto.builder()
                .id(film.getId())
                .name(film.getName())
                .description(film.getDescription())
                .releaseDate(film.getReleaseDate())
                .duration(film.getDuration())
                .mpa(new RatingMpaaDto(film.getId(), null))
                .build();

        return filmDto;
    }

    public static Film updateFilmFields(Film film, UpdateFilmRequest request) {
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

        return film;
    }
}
