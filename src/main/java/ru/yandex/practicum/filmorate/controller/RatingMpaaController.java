package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.RatingMpaa;
import ru.yandex.practicum.filmorate.service.RatingMpaaService;
import java.util.List;

@Validated
@RestController
@RequestMapping("/mpa")
public class RatingMpaaController {
    private final RatingMpaaService ratingMpaaService;

    public RatingMpaaController(RatingMpaaService ratingMpaaService) {
        this.ratingMpaaService = ratingMpaaService;
    }

    @GetMapping("/{ratingId}")
    public ResponseEntity<RatingMpaa> findById(@PathVariable @NotNull @Positive Long ratingId) {
        return ResponseEntity
                .ok(ratingMpaaService.getRatingById(ratingId));
    }

    @GetMapping
    public ResponseEntity<List<RatingMpaa>> findAll() {
        return ResponseEntity
                .ok(ratingMpaaService.getAllRatings());
    }
}