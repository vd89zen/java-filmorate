package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.error.ValidationError;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/films")
@Validated
@Slf4j
public class FilmController {
    private final ConcurrentHashMap<Long, Film> films = new ConcurrentHashMap<>();
    private static final LocalDate MOVIE_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private synchronized long getNextId() {
        return films.isEmpty()
                ? 1
                : Collections.max(films.keySet()) + 1;
    }

    @GetMapping
    @Cacheable("films")
    public Collection<Film> findAll() {
        return films.values();
    }

    @PostMapping
    @CacheEvict(value = "films", allEntries = true)
    public Film create(@Valid @RequestBody Film newFilm) {
        log.info("Попытка создания нового фильма: {}", newFilm);

        if (newFilm.getId() != null) {
            throw new ValidationException(ValidationError.builder()
                    .field("id")
                    .message("При создании нового фильма, id должен быть null.")
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
        films.put(newFilm.getId(), newFilm);
        log.info("Успешно создан новый фильм: {}", newFilm);
        return newFilm;
    }

    @PutMapping
    @CacheEvict(value = "films", key = "#film.id")
    public Film update(@Valid @RequestBody Film film) {
        log.info("Попытка обновления фильма: {}", film);

        if (film.getId() == null) {
            throw new ValidationException(ValidationError.builder()
                    .field("id")
                    .message("Не указан Id.")
                    .rejectedValue("null")
                    .build());
        }

        if (films.containsKey(film.getId())) {
            films.put(film.getId(), film);
            log.info("Успешно обновлён фильм: {}", film);
            return film;
        } else {
            throw new NotFoundException(String.format("Фильм с id = %d не найден.", film.getId()));
        }
    }
}
