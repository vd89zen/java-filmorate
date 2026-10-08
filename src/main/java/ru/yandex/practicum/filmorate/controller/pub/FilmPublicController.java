package ru.yandex.practicum.filmorate.controller.pub;

import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.FilmDto;
import ru.yandex.practicum.filmorate.dto.SearchRequest;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.*;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/films")
public class FilmPublicController {
    private final FilmService filmService;

    public FilmPublicController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping("/{filmId}")
    public ResponseEntity<FilmDto> findById(@PathVariable @NotNull @Positive Long filmId) {
        return ResponseEntity
                .ok(filmService.findById(filmId));
    }

    @GetMapping
    public ResponseEntity<Collection<FilmDto>> findAll(
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive @Max(100) int size) {
        return ResponseEntity
                .ok(filmService.findAll(from, size));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<FilmDto>> getMostPopularFilms(
            @RequestParam(defaultValue = "10") @NotNull @Positive Integer count,
            @RequestParam(required = false) @Positive Long genreId,
            @RequestParam(required = false) @Positive Integer year) {
        return ResponseEntity
                .ok(filmService.getTopPopularFilms(count, genreId, year));
    }

    @GetMapping("/director/{directorId}")
    public ResponseEntity<List<FilmDto>> getFilmsByDirector(
            @PathVariable @NotNull @Positive Long directorId,
            @RequestParam(defaultValue = "year")
            @Pattern(regexp = "year|likes", message = "Допустимые значения: year, likes.") String sortBy) {
        return ResponseEntity
                .ok(filmService.getFilmsByDirector(directorId, sortBy));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FilmDto>> search(@RequestParam(required = false) String query,
                                                @RequestParam(required = false) String by,
                                                @RequestParam(required = false) @Positive Integer year,
                                                @RequestParam(required = false) @Positive Integer yearFrom,
                                                @RequestParam(required = false) @Positive Integer yearTo,
                                                @RequestParam(required = false) @Positive Integer duration,
                                                @RequestParam(required = false) @Positive Integer durationFrom,
                                                @RequestParam(required = false) @Positive Integer durationTo,
                                                @RequestParam(required = false) List<Long> mpaIds,
                                                @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                @RequestParam(defaultValue = "10") @Positive @Max(100) int size) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .by(parseBy(by))
                .year(year)
                .yearFrom(yearFrom)
                .yearTo(yearTo)
                .duration(duration)
                .durationFrom(durationFrom)
                .durationTo(durationTo)
                .mpaIds(mpaIds == null ? Set.of() : new HashSet<>(mpaIds))
                .from(from)
                .size(size)
                .build();

        return ResponseEntity.ok(filmService.search(request));
    }

    private static Set<String> parseBy(String by) {
        if (by == null || by.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(by.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
