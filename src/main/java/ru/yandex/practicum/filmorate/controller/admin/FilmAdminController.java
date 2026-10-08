package ru.yandex.practicum.filmorate.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.NewFilmRequest;
import ru.yandex.practicum.filmorate.dto.UpdateFilmRequest;
import ru.yandex.practicum.filmorate.service.FilmService;

@Validated
@RestController
@RequestMapping("/admin/films")
public class FilmAdminController {
    private final FilmService filmService;

    public FilmAdminController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    public ResponseEntity<FilmDto> create(@Valid @RequestBody NewFilmRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(filmService.create(request));
    }

    @PutMapping
    public ResponseEntity<FilmDto> update(@Valid @RequestBody UpdateFilmRequest request) {
        return ResponseEntity
                .ok(filmService.update(request));
    }

    @DeleteMapping("/{filmId}")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long filmId) {
        filmService.delete(filmId);
        return ResponseEntity
                .noContent().build();
    }
}