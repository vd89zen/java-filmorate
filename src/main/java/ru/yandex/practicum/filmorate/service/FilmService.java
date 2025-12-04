package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.FilmGenresDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmLikesDbStorage;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.mapper.GenreMapper;
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
    private static final String FILM_NOT_FOUND = "Фильм с id = %d не найден.";
    private final FilmStorage filmStorage;
    private final FilmGenresDbStorage filmGenresDbStorage;
    private final FilmLikesDbStorage filmLikesDbStorage;
    private final UserService userService;
    private final GenreService genreService;
    private final RatingMpaaService ratingMpaaService;

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
    public FilmDto create(NewFilmRequest newFilmRequest) {
        log.info("Создание нового фильма: {}", newFilmRequest);
        checkDate(newFilmRequest.getReleaseDate());
        RatingMpaaDto ratingMpaaDto = ratingMpaaService.getRatingMpaaDtoById(newFilmRequest.getMpa().getId());

        Film newFilm = FilmMapper.mapToFilm(newFilmRequest);

        newFilm = filmStorage.create(newFilm);
        Long filmId = newFilm.getId();

        FilmDto filmDto = FilmMapper.mapToDto(newFilm);

//        Set<GenreId> genres = newFilmRequest.getGenres();
//        if (genres != null && genres.isEmpty() == false) {
//            Set<Long> genresIds = GenreMapper.mapGenreIdToIds(genres);
//            genreService.validateGenresByIds(genresIds);
//            log.info("Связывание нового фильма {} с жанрами {}", filmId, genresIds);
//            filmGenresDbStorage.insert(filmId, genresIds);
//            genres.forEach(filmDto.getGenres()::add);
//        }

        Set<GenreId> genres = newFilmRequest.getGenres();
        if (genres != null && genres.isEmpty() == false) {
            Set<Long> genresIds = GenreMapper.mapGenreIdToIds(genres);
            List<GenreDto> genresDto = genreService.getGenresDto(genresIds);
            log.info("Связывание нового фильма {} с жанрами {}", filmId, genresIds);
            filmGenresDbStorage.insert(filmId, genresIds);
            genresDto.forEach(filmDto.getGenres()::add);
        }

        filmDto.setMpa(ratingMpaaDto);
        filmDto.setLikesCount(getLikesCountOfFilm(filmId));

        return filmDto;
    }

    @Transactional
    public FilmDto update(UpdateFilmRequest updateFilmRequest) {
        log.info("Обновление фильма: {}", updateFilmRequest);

        if (updateFilmRequest.hasReleaseDate()) {
            checkDate(updateFilmRequest.getReleaseDate());
        }

        RatingMpaaDto ratingMpaaDto = new RatingMpaaDto();
        if (updateFilmRequest.hasMpa()) {
            ratingMpaaDto = ratingMpaaService.getRatingMpaaDtoById(updateFilmRequest.getMpa().getId());
        }

        Long filmId = updateFilmRequest.getId();
        Film updatingFilm = getFilmOrThrow(filmId);
        updatingFilm = FilmMapper.updateFilmFields(updatingFilm, updateFilmRequest);

        filmStorage.update(updatingFilm);

        FilmDto filmDto = FilmMapper.mapToDto(updatingFilm);

        if (updateFilmRequest.hasGenres()) {
            Set<GenreId> genres = updateFilmRequest.getGenres();
            Set<Long> genresIds = GenreMapper.mapGenreIdToIds(genres);
            List<GenreDto> genresDto = genreService.getGenresDto(genresIds);
            log.info("Обновление жанров у фильма {}", filmId);
            filmGenresDbStorage.deleteAllGenresFromFilm(filmId);
            filmGenresDbStorage.insert(filmId, genresIds);
            genresDto.forEach(filmDto.getGenres()::add);
        }

        if (updateFilmRequest.hasMpa() == false) {
            ratingMpaaDto = ratingMpaaService.getRatingMpaaDtoById(updatingFilm.getMpa().getId());
        }

        filmDto.setMpa(ratingMpaaDto);
        filmDto.setLikesCount(getLikesCountOfFilm(filmId));

        return filmDto;
    }

    @Transactional
    public FilmDto findById(Long filmId) {
        log.info("Поиск фильма ID {}.", filmId);
        Film film = getFilmOrThrow(filmId);
        FilmDto filmDto = FilmMapper.mapToDto(film);

        GenreMapper.toDtoSet(
                filmGenresDbStorage.getGenresOfFilm(filmId))
                .forEach(filmDto.getGenres()::add);

        filmDto.setMpa(
                ratingMpaaService.getRatingMpaaDtoById(film.getMpa().getId()));

        filmDto.setLikesCount(getLikesCountOfFilm(filmId));

        return filmDto;
    }

    @Transactional
    public List<FilmDto> findAll() {
        log.info("Получение списка всех фильмов.");
        List<Film> films = filmStorage.findAll();
        if (films.isEmpty()) {
            return List.of();
        }

        Set<Long> filmsIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toSet());

        Map<Long, RatingMpaaDto> mpaDto = ratingMpaaService.findAll().stream()
                .collect(Collectors.toMap(RatingMpaaDto::getId, ratingMpaaDto -> ratingMpaaDto));
        Map<Long, List<Genre>> genres = filmGenresDbStorage.getGenresByFilmsIds(filmsIds);
        Map<Long, Integer> likesCount = getLikesCountByFilmsIds(filmsIds);

        return films.stream()
                .map(FilmMapper::mapToDto)
                .map(filmDto -> {
                    long mpaId = filmDto.getMpa().getId();
                    filmDto.setMpa(mpaDto.get(mpaId));
                    GenreMapper.toDtoSet(
                            genres.getOrDefault(filmDto.getId(), List.of()))
                            .forEach(filmDto.getGenres()::add);
                    filmDto.setLikesCount(
                            likesCount.getOrDefault(filmDto.getId(), 0));
                    return filmDto;
                }).collect(Collectors.toList());
    }

    public void checkFilmExists(Long filmId) {
        if (filmStorage.isFilmExists(filmId) == false) {
            throw new NotFoundException(String.format(FILM_NOT_FOUND, filmId));
        }
    }

    public void delete(Long filmId) {
        log.info("Удаление фильма ID {}.", filmId);
        if (filmStorage.delete(filmId) == false) {
            throw new NotFoundException(String.format(FILM_NOT_FOUND, filmId));
        }
    }

    private Film getFilmOrThrow(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format(FILM_NOT_FOUND, id)));
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
        List<Film> films = filmStorage.findBySeveralIds(filmsIds);
        Map<Long, Film> filmMap = films.stream().collect(Collectors.toMap(Film::getId, film -> film));
        Map<Long, List<Genre>> genres = filmGenresDbStorage.getGenresByFilmsIds(Set.copyOf(filmsIds));

        List<FilmDto> topFilms = filmsIds.stream()
                .map(filmMap::get)
                .filter(Objects::nonNull)
                .map(FilmMapper::mapToDto)
                .map(filmDto -> {
                    GenreMapper.toDtoSet(
                                    genres.getOrDefault(filmDto.getId(), List.of()))
                            .forEach(filmDto.getGenres()::add);
                    filmDto.setLikesCount(
                            filmsLikes.get(filmDto.getId()));
                    return filmDto;
                }).toList();

        return topFilms;
    }

    private int getLikesCountOfFilm(Long filmId) {
        log.info("Получение количества лайков фильма ID: {}", filmId);
        return filmLikesDbStorage.getLikesCountOfFilm(filmId);
    }

    private Map<Long, Integer> getLikesCountByFilmsIds(Set<Long> filmsIds) {
        return filmLikesDbStorage.getLikesCountByFilmsIds(filmsIds);
    }

}
