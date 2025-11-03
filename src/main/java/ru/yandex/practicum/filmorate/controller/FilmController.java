package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import java.util.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;

    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @CacheEvict(value = "films", allEntries = true)
    public Film create(@Valid @RequestBody Film newFilm) {
        return filmService.create(newFilm);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    @CacheEvict(value = "films", key = "#film.id")
    public Film update(@Valid @RequestBody Film film) {
        return filmService.update(film);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Film findById(@PathVariable long id) {
        return filmService.findById(id);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Cacheable("films")
    public Collection<Film> findAll() {
        return filmService.findAll();
    }

    @PutMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addLike(@PathVariable long id, @PathVariable long userId) {
        filmService.likeFilm(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLike(@PathVariable long id, @PathVariable long userId) {
        filmService.unlikeFilm(id, userId);
    }

    @GetMapping("/popular")
    @ResponseStatus(HttpStatus.OK)
    public List<Film> getMostPopularFilms(@RequestParam(defaultValue = "10") int count) {
        return filmService.getMostPopularFilms(count);
    }
}
