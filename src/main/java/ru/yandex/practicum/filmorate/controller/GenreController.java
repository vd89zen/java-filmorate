package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.dto.GenreDto;
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
    public ResponseEntity<GenreDto> findById(@PathVariable @NotNull @Positive Long genreId) {
        return ResponseEntity
                .ok(genreService.findById(genreId));
    }

    @GetMapping
    public ResponseEntity<List<GenreDto>> findAll() {
        return ResponseEntity
                .ok(genreService.findAll());
    }
}