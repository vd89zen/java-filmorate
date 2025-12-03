package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.NewFilmRequest;
import ru.yandex.practicum.filmorate.dto.UpdateFilmRequest;
import ru.yandex.practicum.filmorate.service.FilmService;
import java.util.*;

@Validated
@RestController
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;

    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    public ResponseEntity<FilmDto> create(@Valid @RequestBody NewFilmRequest newFilmRequest) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(filmService.create(newFilmRequest));
    }

    @PutMapping
    public ResponseEntity<FilmDto> update(@Valid @RequestBody UpdateFilmRequest updateFilmRequest) {
        return ResponseEntity
                .ok(filmService.update(updateFilmRequest));
    }

    @GetMapping("/{filmId}")
    public ResponseEntity<FilmDto> findById(@PathVariable @NotNull @Positive Long filmId) {
        return ResponseEntity
                .ok(filmService.findById(filmId));
    }

    @GetMapping
    public ResponseEntity<Collection<FilmDto>> findAll() {
        return ResponseEntity
                .ok(filmService.findAll());
    }

    @PutMapping("/{filmId}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable @NotNull @Positive Long filmId,
                                        @PathVariable @NotNull @Positive Long userId) {
        filmService.likeFilm(filmId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{filmId}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable @NotNull @Positive Long filmId,
                                           @PathVariable @NotNull @Positive Long userId) {
        filmService.unlikeFilm(filmId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/popular")
    public ResponseEntity<List<FilmDto>> getMostPopularFilms(@RequestParam(defaultValue = "10")
                                                             @NotNull @Positive Integer count) {
        return ResponseEntity
                .ok(filmService.getTopPopularFilms(count));
    }
}
