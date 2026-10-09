package ru.yandex.practicum.filmorate.controller.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.NewReviewRequest;
import ru.yandex.practicum.filmorate.dto.ReviewDto;
import ru.yandex.practicum.filmorate.dto.UpdateReviewRequest;
import ru.yandex.practicum.filmorate.security.UserPrincipal;
import ru.yandex.practicum.filmorate.service.ReviewService;

@Validated
@RestController
@RequestMapping("/me/reviews")
public class ReviewAuthController {
    private final ReviewService reviewService;

    public ReviewAuthController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewDto> create(@Valid @RequestBody NewReviewRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reviewService.create(request, principal.getId()));
    }

    // TODO(auth): проверять, что отзыв принадлежит текущему пользователю
    @PutMapping
    public ResponseEntity<ReviewDto> update(@Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity
                .ok(reviewService.update(request));
    }

    // TODO(auth): проверять, что отзыв принадлежит текущему пользователю
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NotNull @Positive Long id) {
        reviewService.delete(id);
        return ResponseEntity
                .noContent().build();
    }

    @PutMapping("/{id}/like")
    public ResponseEntity<Void> addLike(@PathVariable @NotNull @Positive Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        reviewService.addLike(id, principal.getId());
        return ResponseEntity
                .noContent().build();
    }

    @PutMapping("/{id}/dislike")
    public ResponseEntity<Void> addDislike(@PathVariable @NotNull @Positive Long id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        reviewService.addDislike(id, principal.getId());
        return ResponseEntity
                .noContent().build();
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Void> removeLike(@PathVariable @NotNull @Positive Long id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        reviewService.removeLike(id, principal.getId());
        return ResponseEntity
                .noContent().build();
    }

    @DeleteMapping("/{id}/dislike")
    public ResponseEntity<Void> removeDislike(@PathVariable @NotNull @Positive Long id,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        reviewService.removeDislike(id, principal.getId());
        return ResponseEntity
                .noContent().build();
    }
}