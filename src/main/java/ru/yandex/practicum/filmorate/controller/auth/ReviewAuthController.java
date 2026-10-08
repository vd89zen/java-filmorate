package ru.yandex.practicum.filmorate.controller.auth;

import jakarta.validation.Valid;
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

@Validated
@RestController
@RequestMapping("/reviews")
public class ReviewAuthController {
    private final ReviewService reviewService;

    public ReviewAuthController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // TODO(auth): userId брать из SecurityContext, а не из тела
    @PostMapping
    public ResponseEntity<ReviewDto> create(@Valid @RequestBody NewReviewRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reviewService.create(request));
    }

    // TODO(auth): проверять, что отзыв принадлежит текущему пользователю
    @PutMapping
    public ResponseEntity<ReviewDto> update(@Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.update(request));
    }

    // TODO(auth): проверять, что отзыв принадлежит текущему пользователю
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NotNull @Positive Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable @NotNull @Positive Long id,
                                        @PathVariable @NotNull @Positive Long userId) {
        reviewService.addLike(id, userId);
        return ResponseEntity.noContent().build();
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @PutMapping("/{id}/dislike/{userId}")
    public ResponseEntity<Void> addDislike(@PathVariable @NotNull @Positive Long id,
                                           @PathVariable @NotNull @Positive Long userId) {
        reviewService.addDislike(id, userId);
        return ResponseEntity.noContent().build();
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable @NotNull @Positive Long id,
                                           @PathVariable @NotNull @Positive Long userId) {
        reviewService.removeLike(id, userId);
        return ResponseEntity.noContent().build();
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @DeleteMapping("/{id}/dislike/{userId}")
    public ResponseEntity<Void> removeDislike(@PathVariable @NotNull @Positive Long id,
                                              @PathVariable @NotNull @Positive Long userId) {
        reviewService.removeDislike(id, userId);
        return ResponseEntity.noContent().build();
    }
}