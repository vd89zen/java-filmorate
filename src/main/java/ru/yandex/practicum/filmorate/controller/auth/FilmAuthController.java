package ru.yandex.practicum.filmorate.controller.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/me/films")
public class FilmAuthController {
    private final FilmService filmService;

    public FilmAuthController(FilmService filmService) {
        this.filmService = filmService;
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @PutMapping("/{filmId}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable @NotNull @Positive Long filmId,
                                        @PathVariable @NotNull @Positive Long userId) {
        filmService.likeFilm(filmId, userId);
        return ResponseEntity
                .noContent().build();
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @DeleteMapping("/{filmId}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable @NotNull @Positive Long filmId,
                                           @PathVariable @NotNull @Positive Long userId) {
        filmService.unlikeFilm(filmId, userId);
        return ResponseEntity
                .noContent().build();
    }

    // TODO(auth): убрать userId из query — брать из SecurityContext
    @GetMapping("/common")
    public ResponseEntity<List<FilmDto>> getCommonFilms(
            @RequestParam @NotNull @Positive Long userId,
            @RequestParam @NotNull @Positive Long friendId) {
        return ResponseEntity
                .ok(filmService.getCommonFilms(userId, friendId));
    }
}
