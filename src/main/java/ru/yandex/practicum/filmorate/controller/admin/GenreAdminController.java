package ru.yandex.practicum.filmorate.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.dto.NewGenreRequest;
import ru.yandex.practicum.filmorate.dto.UpdateGenreRequest;
import ru.yandex.practicum.filmorate.service.GenreService;

@Validated
@RestController
@RequestMapping("/admin/genres")
public class GenreAdminController {
    private final GenreService genreService;

    public GenreAdminController(GenreService genreService) {
        this.genreService = genreService;
    }

    @PostMapping
    public ResponseEntity<GenreDto> create(@Valid @RequestBody NewGenreRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(genreService.create(request));
    }

    @PutMapping
    public ResponseEntity<GenreDto> update(@Valid @RequestBody UpdateGenreRequest request) {
        return ResponseEntity
                .ok(genreService.update(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NotNull @Positive Long id) {
        genreService.delete(id);
        return ResponseEntity
                .noContent().build();
    }
}