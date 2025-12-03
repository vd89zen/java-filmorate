package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.GenreService;
import java.util.List;

@Validated
@RestController
@RequestMapping("/genres")
public class GenreController {
    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping("/{genreId}")
    public ResponseEntity<Genre> findById(@PathVariable @NotNull @Positive Long genreId) {
        return ResponseEntity
                .ok(genreService.getGenreById(genreId));
    }

    @GetMapping
    public ResponseEntity<List<Genre>> findAll() {
        return ResponseEntity
                .ok(genreService.getAllGenres());
    }
}