package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.GenreService;
import ru.yandex.practicum.filmorate.service.RatingMpaaService;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FilmMapper {

    public static Film mapToFilmWithGenres(NewFilmRequest newFilmRequest, Set<Long> newGenresIds,
                                           GenreService genreService, RatingMpaaService ratingMpaaService) {
        Film film = Film.builder()
                .name(newFilmRequest.getName())
                .description(newFilmRequest.getDescription())
                .releaseDate(newFilmRequest.getReleaseDate())
                .duration(newFilmRequest.getDuration())
                .build();

        Long ratingId = newFilmRequest.getMpa().getId();
        film.setRatingMpaa(ratingMpaaService.getRatingById(ratingId));

        film.setGenres(genreService.getAllGenres().stream()
                .filter(genre -> newGenresIds.contains(genre.getId()))
                .sorted(Comparator.comparing(Genre::getId))
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        return film;
    }

    public static Film mapToFilmWithoutGenres(NewFilmRequest newFilmRequest, RatingMpaaService ratingMpaaService) {
        Film film = Film.builder()
                .name(newFilmRequest.getName())
                .description(newFilmRequest.getDescription())
                .releaseDate(newFilmRequest.getReleaseDate())
                .duration(newFilmRequest.getDuration())
                .build();

        Long ratingId = newFilmRequest.getMpa().getId();
        film.setRatingMpaa(ratingMpaaService.getRatingById(ratingId));

        return film;
    }

    public static FilmDto mapToFilmDto(Film film) {
        FilmDto filmDto = FilmDto.builder()
                .id(film.getId())
                .name(film.getName())
                .description(film.getDescription())
                .releaseDate(film.getReleaseDate())
                .duration(film.getDuration())
                .mpa(film.getRatingMpaa())
                .genres(film.getGenres())
                .likesCount(film.getLikesCount())
                .build();
        return filmDto;
    }

    public static Film updateFilmFields(Film film, UpdateFilmRequest request, RatingMpaaService ratingMpaaService,
                                        GenreService genreService) {
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
            Long ratingId = request.getMpa().getId();
            film.setRatingMpaa(ratingMpaaService.getRatingById(ratingId));
        }

        if (request.hasGenres()) {
            Set<Long> newGenreIds = request.getGenres().stream()
                    .map(GenreDto::getId)
                    .collect(Collectors.toSet());
            film.setGenres(genreService.getAllGenres().stream()
                    .filter(genre -> newGenreIds.contains(genre.getId()))
                    .collect(Collectors.toSet()));
        }

        return film;
    }
}
