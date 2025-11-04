package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @CacheEvict(value = "films", allEntries = true)
    public ResponseEntity<Film> create(@Valid @RequestBody Film newFilm) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(filmService.create(newFilm));
    }

    @PutMapping
    @CacheEvict(value = "films", key = "#film.id")
    public ResponseEntity<Film> update(@Valid @RequestBody Film film) {
        return ResponseEntity
                .ok(filmService.update(film));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Film> findById(@PathVariable long id) {
        return ResponseEntity
                .ok(filmService.findById(id));
    }

    @GetMapping
    @Cacheable("films")
    public ResponseEntity<Collection<Film>> findAll() {
        return ResponseEntity
                .ok(filmService.findAll());
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable long id, @PathVariable long userId) {
        filmService.likeFilm(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable long id, @PathVariable long userId) {
        filmService.unlikeFilm(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/popular")
    public ResponseEntity<List<Film>> getMostPopularFilms(@RequestParam(defaultValue = "10") int count) {
        return ResponseEntity
                .ok(filmService.getMostPopularFilms(count));
    }
}
