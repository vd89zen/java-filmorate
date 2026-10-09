package ru.yandex.practicum.filmorate.controller.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.security.UserPrincipal;
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

    @PutMapping("/{filmId}/like")
    public ResponseEntity<Void> addLike(@PathVariable @NotNull @Positive Long filmId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        filmService.likeFilm(filmId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{filmId}/like")
    public ResponseEntity<Void> removeLike(@PathVariable @NotNull @Positive Long filmId,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        filmService.unlikeFilm(filmId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/common")
    public ResponseEntity<List<FilmDto>> getCommonFilms(
            @RequestParam @NotNull @Positive Long friendId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                filmService.getCommonFilms(principal.getId(), friendId));
    }
}
