package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.error.ValidationError;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {
    private static final LocalDate MOVIE_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private Long nextId;

    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;

        log.info("Определение начального значения для генерации ID фильмов.");
        Collection<Film> allFilms = filmStorage.findAll();
        if (allFilms.isEmpty()) {
            this.nextId = 1L;
        } else {
            long maxId = allFilms.stream()
                    .mapToLong(Film::getId)
                    .max()
                    .orElse(0L);

            this.nextId = (maxId == Long.MAX_VALUE) ? 1 : maxId + 1;
        }
        log.info("Начальное значение для генерации ID фильмов: {}.", this.nextId);
    }

    public Film create(Film newFilm) {
        log.info("Создание нового фильма: {}", newFilm);

        if (newFilm.getId() != null) {
            throw new ValidationException(ValidationError.builder()
                    .field("id")
                    .message("При создании нового фильма id должен быть null.")
                    .rejectedValue(newFilm.getId())
                    .build());
        }

        if (newFilm.getReleaseDate().isBefore(MOVIE_BIRTHDAY)) {
            throw new ValidationException(ValidationError.builder()
                    .field("releaseDate")
                    .message("Дата релиза должна быть не раньше 28 декабря 1895 года.")
                    .rejectedValue(newFilm.getReleaseDate())
                    .build());
        }

        newFilm.setId(getNextId());
        Film createdFilm = filmStorage.create(newFilm.selfCopy());
        log.info("Успешно создан новый фильм: {}", createdFilm);
        return createdFilm;
    }

    public Film update(Film film) {
        log.info("Обновление фильма: {}", film);

        if (film.getId() == null) {
            throw new ValidationException(ValidationError.builder()
                    .field("id")
                    .message("Не указан Id.")
                    .rejectedValue("null")
                    .build());
        }

        if (film.getReleaseDate().isBefore(MOVIE_BIRTHDAY)) {
            throw new ValidationException(ValidationError.builder()
                    .field("releaseDate")
                    .message("Дата релиза должна быть не раньше 28 декабря 1895 года.")
                    .rejectedValue(film.getReleaseDate())
                    .build());
        }

        Film oldFilm = getFilmOrThrow(film.getId());
        Film updatedFilm = Film.builder()
                .id(film.getId())
                .name(film.getName())
                .description(film.getDescription())
                .releaseDate(film.getReleaseDate())
                .duration(film.getDuration())
                .likes(new HashSet<>(oldFilm.getLikes()))
                .build();
        log.info("Успешно обновлён фильм: {}", updatedFilm);
        return filmStorage.update(updatedFilm);
    }

    public void delete(long filmId) {
        log.info("Удаление фильма ID {}.", filmId);
        filmStorage.delete(filmId);
    }

    public Film findById(long filmId) {
        log.info("Поиск фильма ID {}.", filmId);
        return getFilmOrThrow(filmId);
    }

    public Collection<Film> findAll() {
        log.info("Получение списка всех фильмов.");
        return filmStorage.findAll();
    }

    public void likeFilm(long filmId, long userId) {
        log.info("Добавление лайка: фильм ID {}, пользователь ID {}.", filmId, userId);
        Film film = getFilmOrThrow(filmId);

        if (userStorage.findById(userId).isPresent()) {
            if (film.getLikes().add(userId)) {
                filmStorage.update(film);
                log.info("Успешное добавление лайка: фильм ID {}, пользователь ID {}.", filmId, userId);
            } else {
                throw new ValidationException(ValidationError.builder()
                        .field("likes")
                        .message("У фильма уже есть лайк от пользователя.")
                        .rejectedValue(String.format("Фильм ID %d, пользователь ID %d.", filmId, userId))
                        .build());
            }
        } else {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден.", userId));
        }
    }

    public void unlikeFilm(long filmId, long userId) {
        log.info("Удаление лайка: фильм ID {}, пользователь ID {}.", filmId, userId);
        Film film = getFilmOrThrow(filmId);

        if (film.getLikes().remove(userId)) {
            filmStorage.update(film);
            log.info("Успешное удаление лайка: фильм ID {}, пользователь ID {}.", filmId, userId);
        } else {
            throw new NotFoundException(String.format("У фильма ID %d нет лайка от пользователя ID %d.", filmId, userId));
        }
    }

    public List<Film> getMostPopularFilms(int count) {
        log.info("Получение списка из {} самых популярных фильмов", count);

        if (count < 1) {
            throw new ValidationException(ValidationError.builder()
                    .field("count")
                    .message("Количество фильмов должно быть больше 0.")
                    .rejectedValue(count)
                    .build());
        }

        return filmStorage.findAll().stream()
                .filter(film -> !film.getLikes().isEmpty())
                .sorted((f1, f2) -> Long.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
    }

    private Film getFilmOrThrow(long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Фильм с id = %d не найден.", id)));
    }

    private synchronized long getNextId() {
        return nextId++;
    }
}
