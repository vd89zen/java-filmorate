package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.FilmDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmDirectorsDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmGenresDbStorage;
import ru.yandex.practicum.filmorate.dal.FilmLikesDbStorage;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.mapper.DirectorMapper;
import ru.yandex.practicum.filmorate.mapper.GenreMapper;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.ValidationError;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.FilmMapper;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;
import ru.yandex.practicum.filmorate.model.enums.OperationTypes;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate MOVIE_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private static final String FILM_NOT_FOUND = "Фильм с id = %d не найден.";
    private static final Set<String> VALID_SEARCH_BY = Set.of("title", "director", "description");
    private final FilmDbStorage filmStorage;
    private final FilmGenresDbStorage filmGenresDbStorage;
    private final FilmLikesDbStorage filmLikesDbStorage;
    private final FilmDirectorsDbStorage filmDirectorsDbStorage;
    private final UserService userService;
    private final GenreService genreService;
    private final RatingMpaaService ratingMpaaService;
    private final EventService eventService;
    private final DirectorService directorService;

    public void checkFilmExists(Long filmId) {
        if (filmStorage.isFilmExists(filmId) == false) {
            throw new NotFoundException(String.format(FILM_NOT_FOUND, filmId));
        }
    }

    @Transactional
    public FilmDto create(NewFilmRequest request) {
        log.info("Создание нового фильма: {}", request);
        checkDate(request.getReleaseDate());
        RatingMpaaDto ratingMpaaDto = ratingMpaaService.getRatingMpaaDtoById(request.getMpa().getId());

        Film newFilm = FilmMapper.mapToFilm(request);

        newFilm = filmStorage.create(newFilm);
        Long filmId = newFilm.getId();

        FilmDto filmDto = FilmMapper.mapToDto(newFilm);

        Set<GenreId> genres = request.getGenres();
        if (genres.isEmpty() == false) {
            Set<Long> genresIds = GenreMapper.mapGenreIdToIds(genres);
            List<GenreDto> genresDto = genreService.getGenresDto(genresIds);
            log.info("Связывание нового фильма {} с жанрами {}", filmId, genresIds);
            filmGenresDbStorage.insert(filmId, genresIds);
            genresDto.forEach(filmDto.getGenres()::add);
        }

        Set<DirectorId> directors = request.getDirectors();
        if (directors.isEmpty() == false) {
            Set<Long> directorsIds = DirectorMapper.mapDirectorIdToIds(directors);
            List<DirectorDto> directorsDto = directorService.getDirectorsDto(directorsIds);
            log.info("Связывание нового фильма {} с режиссёрами {}", filmId, directorsIds);
            filmDirectorsDbStorage.insert(filmId, directorsIds);
            directorsDto.forEach(filmDto.getDirectors()::add);
        }

        filmDto.setMpa(ratingMpaaDto);
        filmDto.setLikesCount(getLikesCountOfFilm(filmId));

        return filmDto;
    }

    @Transactional
    public FilmDto update(UpdateFilmRequest request) {
        log.info("Обновление фильма: {}", request);

        if (request.hasReleaseDate()) {
            checkDate(request.getReleaseDate());
        }

        RatingMpaaDto ratingMpaaDto = new RatingMpaaDto();
        if (request.hasMpa()) {
            ratingMpaaDto = ratingMpaaService.getRatingMpaaDtoById(request.getMpa().getId());
        }

        Long filmId = request.getId();
        Film film = getFilmOrThrow(filmId);
        FilmMapper.updateFilmFields(film, request);

        filmStorage.update(film);

        FilmDto filmDto = FilmMapper.mapToDto(film);

        if (request.hasGenres()) {
            Set<GenreId> genres = request.getGenres();
            Set<Long> genresIds = GenreMapper.mapGenreIdToIds(genres);
            List<GenreDto> genresDto = genreService.getGenresDto(genresIds);
            log.info("Обновление жанров у фильма {}", filmId);
            filmGenresDbStorage.deleteAllGenresFromFilm(filmId);
            filmGenresDbStorage.insert(filmId, genresIds);
            genresDto.forEach(filmDto.getGenres()::add);
        }

        if (request.hasDirectors()) {
            Set<DirectorId> directors = request.getDirectors();
            Set<Long> directorsIds = DirectorMapper.mapDirectorIdToIds(directors);
            List<DirectorDto> directorsDto = directorService.getDirectorsDto(directorsIds);
            log.info("Обновление режиссёров у фильма {}", filmId);
            filmDirectorsDbStorage.deleteAllDirectorsFromFilm(filmId);
            filmDirectorsDbStorage.insert(filmId, directorsIds);
            directorsDto.forEach(filmDto.getDirectors()::add);
        }

        if (request.hasMpa() == false) {
            ratingMpaaDto = ratingMpaaService.getRatingMpaaDtoById(film.getMpa().getId());
        }

        filmDto.setMpa(ratingMpaaDto);
        filmDto.setLikesCount(getLikesCountOfFilm(filmId));

        return filmDto;
    }

    @Transactional(readOnly = true)
    public FilmDto findById(Long filmId) {
        log.info("Поиск фильма ID {}.", filmId);
        Film film = getFilmOrThrow(filmId);
        FilmDto filmDto = FilmMapper.mapToDto(film);

        GenreMapper.toDtoList(
                        filmGenresDbStorage.getGenresOfFilm(filmId))
                .forEach(filmDto.getGenres()::add);

        DirectorMapper.toDtoList(
                        filmDirectorsDbStorage.getDirectorsOfFilm(filmId))
                .forEach(filmDto.getDirectors()::add);

        filmDto.setMpa(
                ratingMpaaService.getRatingMpaaDtoById(film.getMpa().getId()));

        filmDto.setLikesCount(getLikesCountOfFilm(filmId));

        return filmDto;
    }

    @Transactional(readOnly = true)
    public List<FilmDto> findAll(int from, int size) {
        log.info("Получение списка фильмов: from={}, size={}.", from, size);
        return enrichFilms(filmStorage.findAll(from, size));
    }

    @Transactional(readOnly = true)
    public Map<Long, FilmShortDto> findShortByIds(Set<Long> filmIds) {
        if (filmIds == null || filmIds.isEmpty()) {
            return Map.of();
        }
        return filmStorage.findBySeveralIds(new ArrayList<>(filmIds)).stream()
                .collect(Collectors.toMap(
                        Film::getId,
                        film -> FilmShortDto.builder()
                                .id(film.getId())
                                .name(film.getName())
                                .releaseDate(film.getReleaseDate())
                                .build()));
    }

    public void delete(Long filmId) {
        log.info("Удаление фильма ID {}.", filmId);
        if (filmStorage.delete(filmId) == false) {
            throw new NotFoundException(String.format(FILM_NOT_FOUND, filmId));
        }
        log.info("Фильм ID {} успешно удалён.", filmId);
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
        } else {
            eventService.addEvent(userId, EventTypes.LIKE, OperationTypes.ADD, filmId);
            log.info("FilmService: Добавлено событие (add like) в ленту пользователя");
        }
    }

    @Transactional
    public void unlikeFilm(Long filmId, Long userId) {
        log.info("Удаление лайка: фильм ID {}, пользователь ID {}.", filmId, userId);

        checkFilmExists(filmId);
        userService.checkUserExists(userId);

        if (filmLikesDbStorage.deleteLikeFromFilmIfExists(filmId, userId) == false) {
            throw new NotFoundException(String.format("У фильма ID %d нет лайка от пользователя ID %d.", filmId, userId));
        } else {
            eventService.addEvent(userId, EventTypes.LIKE, OperationTypes.REMOVE, filmId);
            log.info("FilmService: Добавлено событие (remove like) в ленту пользователя");
        }
    }

    @Transactional(readOnly = true)
    public List<FilmDto> getTopPopularFilms(Integer count, Long genreId, Integer year) {
        log.info("Получение списка из {} самых популярных фильмов (genreId={}, year={})",
                count, genreId, year);

        if (genreId != null) {
            genreService.findById(genreId);
        }

        LinkedHashMap<Long, Integer> filmsLikes =
                filmLikesDbStorage.getTopPopularFilmsIds(count, genreId, year);
        List<Long> filmIds = List.copyOf(filmsLikes.keySet());
        if (filmIds.isEmpty()) {
            return List.of();
        }

        // findBySeveralIds не гарантирует порядок — восстанавливаем его по filmIds.
        Map<Long, Film> filmMap = filmStorage.findBySeveralIds(filmIds).stream()
                .collect(Collectors.toMap(Film::getId, film -> film));
        List<Film> orderedFilms = filmIds.stream()
                .map(filmMap::get)
                .filter(Objects::nonNull)
                .toList();

        return enrichFilms(orderedFilms);
    }

    @Transactional(readOnly = true)
    public List<FilmDto> getCommonFilms(Long userId, Long otherUserId) {
        log.info("Получение общих фильмов пользователей ID {} и ID {}.", userId, otherUserId);

        userService.checkUserExists(userId);
        userService.checkUserExists(otherUserId);

        Set<Long> userLikes = filmLikesDbStorage.getFilmsIdsLikedByUser(userId);
        Set<Long> otherUserLikes = filmLikesDbStorage.getFilmsIdsLikedByUser(otherUserId);

        Set<Long> commonFilmIds = new HashSet<>(userLikes);
        commonFilmIds.retainAll(otherUserLikes);

        if (commonFilmIds.isEmpty()) {
            log.info("У пользователей ID {} и ID {} нет общих фильмов.", userId, otherUserId);
            return List.of();
        }

        List<FilmDto> result = new ArrayList<>(
                enrichFilms(filmStorage.findBySeveralIds(new ArrayList<>(commonFilmIds))));
        result.sort(Comparator.comparing(FilmDto::getLikesCount).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public List<FilmDto> getFilmsByDirector(Long directorId, String sortBy) {
        log.info("Получение фильмов режиссёра ID {} с сортировкой {}", directorId, sortBy);

        directorService.checkDirectorExists(directorId);

        List<Film> films = switch (sortBy) {
            case "year" -> filmStorage.findByDirectorSortedByYear(directorId);
            case "likes" -> filmStorage.findByDirectorSortedByLikes(directorId);
            default -> throw new ValidationException(ValidationError.builder()
                    .field("sortBy")
                    .message("Допустимые значения: year, likes.")
                    .rejectedValue(sortBy)
                    .build());
        };

        return enrichFilms(films);
    }

    @Transactional(readOnly = true)
    public List<FilmDto> search(SearchRequest request) {
        log.info("Поиск фильмов: {}", request);

        validateSearch(request);
        normalizeSearchBy(request);

        List<Film> films = filmStorage.search(request);
        return enrichFilms(films);
    }

    private void validateSearch(SearchRequest r) {
        Set<String> by = r.getBy() == null ? Set.of() : r.getBy();
        boolean hasQuery = r.getQuery() != null && !r.getQuery().isBlank();

        if (!VALID_SEARCH_BY.containsAll(by)) {
            throw new ValidationException(ValidationError.builder()
                    .field("by")
                    .message("Допустимые значения: title, director, description (через запятую).")
                    .rejectedValue(by)
                    .build());
        }
        if (!by.isEmpty() && !hasQuery) {
            throw new ValidationException(ValidationError.builder()
                    .field("query")
                    .message("Параметр by задан, но query отсутствует.")
                    .rejectedValue(r.getQuery())
                    .build());
        }
        if (r.getYear() != null && (r.getYearFrom() != null || r.getYearTo() != null)) {
            throw new ValidationException(ValidationError.builder()
                    .field("year")
                    .message("Нельзя комбинировать year с yearFrom/yearTo.")
                    .rejectedValue(r.getYear())
                    .build());
        }
        if (r.getYearFrom() != null && r.getYearTo() != null && r.getYearFrom() > r.getYearTo()) {
            throw new ValidationException(ValidationError.builder()
                    .field("yearFrom")
                    .message("yearFrom должен быть не больше yearTo.")
                    .rejectedValue(r.getYearFrom())
                    .build());
        }
        if (r.getDuration() != null && (r.getDurationFrom() != null || r.getDurationTo() != null)) {
            throw new ValidationException(ValidationError.builder()
                    .field("duration")
                    .message("Нельзя комбинировать duration с durationFrom/durationTo.")
                    .rejectedValue(r.getDuration())
                    .build());
        }
        if (r.getDurationFrom() != null && r.getDurationTo() != null
                && r.getDurationFrom() > r.getDurationTo()) {
            throw new ValidationException(ValidationError.builder()
                    .field("durationFrom")
                    .message("durationFrom должен быть не больше durationTo.")
                    .rejectedValue(r.getDurationFrom())
                    .build());
        }
    }

    /**
     * Если query задан, а by пустой — ищем по названию (дефолт).
     */
    private void normalizeSearchBy(SearchRequest r) {
        boolean hasQuery = r.getQuery() != null && !r.getQuery().isBlank();
        Set<String> by = r.getBy() == null ? Set.of() : r.getBy();
        if (hasQuery && by.isEmpty()) {
            r.setBy(Set.of("title"));
        }
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

    private Film getFilmOrThrow(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format(FILM_NOT_FOUND, id)));
    }

    private int getLikesCountOfFilm(Long filmId) {
        log.info("Получение количества лайков фильма ID: {}", filmId);
        return filmLikesDbStorage.getLikesCountOfFilm(filmId);
    }

    private List<FilmDto> enrichFilms(List<Film> films) {
        if (films.isEmpty()) {
            return List.of();
        }

        Set<Long> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toSet());

        Map<Long, RatingMpaaDto> mpaDto = ratingMpaaService.findAll().stream()
                .collect(Collectors.toMap(RatingMpaaDto::getId, r -> r));
        Map<Long, List<Genre>> genres = filmGenresDbStorage.getGenresByFilmsIds(filmIds);
        Map<Long, List<Director>> directors = filmDirectorsDbStorage.getDirectorsByFilmsIds(filmIds);
        Map<Long, Integer> likesCount = filmLikesDbStorage.getLikesCountByFilmsIds(filmIds);

        return films.stream()
                .map(FilmMapper::mapToDto)
                .map(filmDto -> {
                    filmDto.setMpa(mpaDto.get(filmDto.getMpa().getId()));
                    GenreMapper.toDtoList(
                            genres.getOrDefault(filmDto.getId(), List.of()))
                            .forEach(filmDto.getGenres()::add);
                    DirectorMapper.toDtoList(
                            directors.getOrDefault(filmDto.getId(), List.of()))
                            .forEach(filmDto.getDirectors()::add);
                    filmDto.setLikesCount(
                            likesCount.getOrDefault(filmDto.getId(), 0));
                    return filmDto;
                })
                .collect(Collectors.toList());
    }
}