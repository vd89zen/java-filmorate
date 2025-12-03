package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.FilmGenresDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmLikesDbStorage;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.dto.NewFilmRequest;
import ru.yandex.practicum.filmorate.dto.UpdateFilmRequest;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.ValidationError;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmStorage;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate MOVIE_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private final FilmStorage filmStorage;
    private final FilmGenresDbStorage filmGenresDbStorage;
    private final FilmLikesDbStorage filmLikesDbStorage;
    private final UserService userService;
    private final GenreService genreService;
    private final RatingMpaaService ratingMpaaService;

    @Transactional
    public FilmDto create(NewFilmRequest newFilmRequest) {
        log.info("Создание нового фильма: {}", newFilmRequest);

        checkDate(newFilmRequest.getReleaseDate());

        Film newFilm;
        if (newFilmRequest.getGenres() != null && newFilmRequest.getGenres().isEmpty() == false) {
            Set<Long> newGenresIds = newFilmRequest.getGenres().stream()
                    .map(GenreDto::getId)
                    .collect(Collectors.toSet());
            genreService.validateGenreIds(newGenresIds);
            newFilm = FilmMapper.mapToFilmWithGenres(newFilmRequest, newGenresIds, genreService, ratingMpaaService);
            newFilm = filmStorage.create(newFilm);

            log.info("Связывание фильма {} с жанрами {}", newFilm.getId(), newGenresIds);
            filmGenresDbStorage.insert(newFilm.getId(), newGenresIds);
        } else {
            newFilm = FilmMapper.mapToFilmWithoutGenres(newFilmRequest, ratingMpaaService);
            newFilm = filmStorage.create(newFilm);
        }

        return FilmMapper.mapToFilmDto(newFilm);
    }

    @Transactional
    public FilmDto update(UpdateFilmRequest updateFilmRequest) {
        log.info("Обновление фильма: {}", updateFilmRequest);

        if (updateFilmRequest.hasReleaseDate()) {
            checkDate(updateFilmRequest.getReleaseDate());
        }

        if (updateFilmRequest.hasGenres()) {
            genreService.validateGenreIds(
                    updateFilmRequest.getGenres().stream()
                            .map(GenreDto::getId)
                            .collect(Collectors.toSet()));
        }

        Film updatingFilm = getFilmOrThrow(updateFilmRequest.getId());
        updatingFilm = FilmMapper.updateFilmFields(updatingFilm, updateFilmRequest, ratingMpaaService, genreService);
        filmStorage.update(updatingFilm);

        Long updatingFilmId = updatingFilm.getId();
        if (updateFilmRequest.hasGenres()) {
            log.info("Обновление жанров у фильма {}", updatingFilmId);
            filmGenresDbStorage.deleteAllGenresFromFilm(updatingFilmId);
            filmGenresDbStorage.insert(
                    updatingFilmId,
                    updateFilmRequest.getGenres().stream()
                            .map(GenreDto::getId)
                            .collect(Collectors.toSet()));
        }

        updatingFilm.setLikesCount(getLikesCountOfFilm(updatingFilmId));

        return FilmMapper.mapToFilmDto(updatingFilm);
    }

    @Transactional
    public FilmDto findById(Long filmId) {
        log.info("Поиск фильма ID {}.", filmId);
        Film film = getFilmOrThrow(filmId);
        Set<Genre> genres = filmGenresDbStorage.getGenresOfFilm(filmId);
        int likesCount = getLikesCountOfFilm(filmId);
        film.setGenres(genres);
        film.setLikesCount(likesCount);
        return FilmMapper.mapToFilmDto(film);
    }

    @Transactional
    public List<FilmDto> findAll() {
        log.info("Получение списка всех фильмов.");
        Collection<Film> films = filmStorage.findAll();

        Map<Long, Set<Genre>> genres = filmGenresDbStorage.getGenresByFilmsIds(
                films.stream()
                        .map(Film::getId)
                        .collect(Collectors.toSet()));

        Map<Long, Integer> likesCount = getLikesCountByFilmsIds(
                films.stream()
                        .map(Film::getId)
                        .collect(Collectors.toSet()));

        films.forEach(film -> {
            film.setGenres(genres.getOrDefault(film.getId(), Set.of()));
            film.setLikesCount(likesCount.getOrDefault(film.getId(), 0));
        });

        return films.stream()
                .map(FilmMapper::mapToFilmDto)
                .collect(Collectors.toList());
    }

    public List<Film> findBySeveralIds(List<Long> filmsIds) {
        return filmStorage.findBySeveralIds(filmsIds);
    }

    public void checkFilmExists(Long filmId) {
        if (filmStorage.isFilmExists(filmId) == false) {
            throw new NotFoundException(String.format("Фильм с id = %d не найден.", filmId));
        }
    }

    public void delete(Long filmId) {
        log.info("Удаление фильма ID {}.", filmId);
        if (filmStorage.delete(filmId) == false) {
            throw new NotFoundException(String.format("Фильм с id = %d не найден.", filmId));
        }
    }

    private Film getFilmOrThrow(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Фильм с id = %d не найден.", id)));
    }

    private void checkDate(LocalDate date) {
        if (date.isBefore(MOVIE_BIRTHDAY)) {
            throw new ValidationException(ValidationError.builder()
                    .field("releaseDate")
                    .message("Дата релиза должна быть не раньше 28 декабря 1895 года.")
                    .rejectedValue(date)
                    .build());
        }
    }

    @Transactional
    public void likeFilm(Long filmId, Long userId) {
        log.info("Добавление лайка: фильм ID {}, пользователь ID {}.", filmId, userId);

        checkFilmExists(filmId);
        userService.checkUserExists(userId);

        if (filmLikesDbStorage.addLikeIfNotExists(filmId, userId) == false) {
            throw new ValidationException(ValidationError.builder()
                    .field("likes")
                    .message("У фильма уже есть лайк от пользователя.")
                    .rejectedValue(String.format("Фильм ID %d, пользователь ID %d.", filmId, userId))
                    .build());
        }
    }

    @Transactional
    public void unlikeFilm(Long filmId, Long userId) {
        log.info("Удаление лайка: фильм ID {}, пользователь ID {}.", filmId, userId);

        checkFilmExists(filmId);
        userService.checkUserExists(userId);

        if (filmLikesDbStorage.deleteLikeFromFilmIfExists(filmId, userId) == false) {
            throw new NotFoundException(String.format("У фильма ID %d нет лайка от пользователя ID %d.", filmId, userId));
        }
    }

    @Transactional
    public List<FilmDto> getTopPopularFilms(Integer count) {
        log.info("Получение списка из {} самых популярных фильмов", count);
        LinkedHashMap<Long, Integer> filmsLikes = filmLikesDbStorage.getTopPopularFilmsIds(count);
        List<Long> filmsIds = List.copyOf(filmsLikes.keySet());
        List<Film> topFilms = findBySeveralIds(filmsIds);
        topFilms.forEach(film -> film.setLikesCount(filmsLikes.get(film.getId())));

        return topFilms.stream()
                .sorted(Comparator.comparing(Film::getLikesCount).reversed())
                .map(FilmMapper::mapToFilmDto)
                .collect(Collectors.toList());
    }

    public int getLikesCountOfFilm(Long filmId) {
        return filmLikesDbStorage.getLikesCountOfFilm(filmId);
    }

    public Map<Long, Integer> getLikesCountByFilmsIds(Set<Long> filmsIds) {
        return filmLikesDbStorage.getLikesCountByFilmsIds(filmsIds);
    }

    public boolean hasUserLikedFilm(Long filmId, Long userId) {
        return filmLikesDbStorage.hasUserLikedFilm(filmId, userId);
    }
}
