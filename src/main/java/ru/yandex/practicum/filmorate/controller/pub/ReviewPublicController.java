package ru.yandex.practicum.filmorate.controller.pub;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.ReviewDto;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/reviews")
public class ReviewPublicController {
    private final ReviewService reviewService;

    public ReviewPublicController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDto> findById(@PathVariable @NotNull @Positive Long id) {
        return ResponseEntity.ok(reviewService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<ReviewDto>> findAll(
            @RequestParam(required = false) @Positive Long filmId,
            @RequestParam(defaultValue = "10") @Positive @Max(100) Integer count) {
        return ResponseEntity.ok(reviewService.findAll(filmId, count));
    }
}