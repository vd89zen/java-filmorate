package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.NewReviewRequest;
import ru.yandex.practicum.filmorate.dto.ReviewDto;
import ru.yandex.practicum.filmorate.dto.UpdateReviewRequest;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewDto> create(@Valid @RequestBody NewReviewRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reviewService.create(request));
    }

    @PutMapping
    public ResponseEntity<ReviewDto> update(@Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity
                .ok(reviewService.update(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NotNull @Positive Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDto> findById(@PathVariable @NotNull @Positive Long id) {
        return ResponseEntity
                .ok(reviewService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<ReviewDto>> findAll(
            @RequestParam(required = false) @Positive Long filmId,
            @RequestParam(defaultValue = "10") @Positive @Max(100) Integer count) {
        return ResponseEntity
                .ok(reviewService.findAll(filmId, count));
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable @NotNull @Positive Long id,
                                        @PathVariable @NotNull @Positive Long userId) {
        reviewService.addLike(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/dislike/{userId}")
    public ResponseEntity<Void> addDislike(@PathVariable @NotNull @Positive Long id,
                                           @PathVariable @NotNull @Positive Long userId) {
        reviewService.addDislike(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable @NotNull @Positive Long id,
                                           @PathVariable @NotNull @Positive Long userId) {
        reviewService.removeLike(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public ResponseEntity<Void> removeDislike(@PathVariable @NotNull @Positive Long id,
                                              @PathVariable @NotNull @Positive Long userId) {
        reviewService.removeDislike(id, userId);
        return ResponseEntity.noContent().build();
    }
}