package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.ReviewDbStorage;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.ReviewMapper;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.ValidationError;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;
import ru.yandex.practicum.filmorate.model.enums.OperationTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewService {
    private static final String REVIEW_NOT_FOUND = "Отзыв с id = %d не найден.";

    private final ReviewDbStorage reviewStorage;
    private final UserService userService;
    private final FilmService filmService;
    private final EventService eventService;

    @Transactional
    public ReviewDto create(NewReviewRequest request) {
        log.info("ReviewService: создание отзыва {}", request);

        userService.checkUserExists(request.getUserId());
        filmService.checkFilmExists(request.getFilmId());

        if (reviewStorage.isReviewExistsByUserAndFilm(request.getUserId(), request.getFilmId())) {
            throw new ValidationException(ValidationError.builder()
                    .field("review")
                    .message("Пользователь уже оставил отзыв на этот фильм.")
                    .rejectedValue(String.format("userId=%d, filmId=%d",
                            request.getUserId(), request.getFilmId()))
                    .build());
        }

        Review review = ReviewMapper.mapToReview(request);
        Review saved = reviewStorage.create(review);

        eventService.addEvent(
                saved.getUserId(), EventTypes.REVIEW, OperationTypes.ADD, saved.getReviewId());

        return ReviewMapper.mapToDto(saved);
    }

    @Transactional
    public ReviewDto update(UpdateReviewRequest request) {
        log.info("ReviewService: обновление отзыва {}", request);

        if (!request.hasContent() && !request.hasIsPositive()) {
            throw new ValidationException(ValidationError.builder()
                    .field("update")
                    .message("Нужно указать хотя бы одно поле для обновления.")
                    .rejectedValue(request)
                    .build());
        }

        if (request.hasContent() && request.getContent().isBlank()) {
            throw new ValidationException(ValidationError.builder()
                    .field("content")
                    .message("Содержание отзыва не может быть пустым.")
                    .rejectedValue(request.getContent())
                    .build());
        }

        Review review = getReviewOrThrow(request.getReviewId());

        ReviewMapper.updateReviewFields(review, request);
        reviewStorage.update(review);

        eventService.addEvent(
                review.getUserId(), EventTypes.REVIEW, OperationTypes.UPDATE, review.getReviewId());

        return ReviewMapper.mapToDto(review);
    }

    @Transactional
    public void delete(Long reviewId) {
        log.info("ReviewService: удаление отзыва ID {}", reviewId);

        Review review = getReviewOrThrow(reviewId);
        reviewStorage.delete(reviewId);

        eventService.addEvent(
                review.getUserId(), EventTypes.REVIEW, OperationTypes.REMOVE, reviewId);
    }

    @Transactional(readOnly = true)
    public ReviewDto findById(Long reviewId) {
        log.info("ReviewService: получение отзыва ID {}", reviewId);
        return ReviewMapper.mapToDto(getReviewOrThrow(reviewId));
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> findAll(Long filmId, int count) {
        log.info("ReviewService: получение отзывов filmId={}, count={}", filmId, count);
        List<Review> reviews = filmId == null
                ? reviewStorage.findAll(count)
                : reviewStorage.findByFilmId(filmId, count);

        return reviews.stream()
                .map(ReviewMapper::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Long, ReviewShortDto> findShortByIds(Set<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return Map.of();
        }

        List<Review> reviews = reviewStorage.findBySeveralIds(new ArrayList<>(reviewIds));
        if (reviews.isEmpty()) {
            return Map.of();
        }

        // Один batch-запрос за фильмами
        Set<Long> filmIds = reviews.stream()
                .map(Review::getFilmId)
                .collect(Collectors.toSet());
        Map<Long, FilmShortDto> films = filmService.findShortByIds(filmIds);

        return reviews.stream()
                .collect(Collectors.toMap(
                        Review::getReviewId,
                        review -> ReviewShortDto.builder()
                                .reviewId(review.getReviewId())
                                .content(review.getContent())
                                .isPositive(review.getIsPositive())
                                .film(films.get(review.getFilmId()))
                                .build()));
    }

    @Transactional
    public void addLike(Long reviewId, Long userId) {
        log.info("ReviewService: лайк отзыву {} от пользователя {}", reviewId, userId);
        checkReviewExists(reviewId);
        userService.checkUserExists(userId);

        if (reviewStorage.addLikeIfNotExists(reviewId, userId) == false) {
            log.info("ReviewService: у отзыва ID {} уже есть оценка пользователя ID {}", reviewId, userId);
        }
    }

    @Transactional
    public void addDislike(Long reviewId, Long userId) {
        log.info("ReviewService: дизлайк отзыву {} от пользователя {}", reviewId, userId);
        checkReviewExists(reviewId);
        userService.checkUserExists(userId);

        if (!reviewStorage.addDislikeIfNotExists(reviewId, userId)) {
            log.info("ReviewService: пользователь ID {} уже оценил отзыв ID {}", userId, reviewId);
        }
    }

    @Transactional
    public void removeLike(Long reviewId, Long userId) {
        log.info("ReviewService: снятие лайка с отзыва {} пользователем {}", reviewId, userId);
        checkReviewExists(reviewId);
        userService.checkUserExists(userId);

        if (!reviewStorage.deleteLikeIfExists(reviewId, userId)) {
            log.info("ReviewService: лайка от пользователя ID {} у отзыва ID {} не было", userId, reviewId);
        }
    }

    @Transactional
    public void removeDislike(Long reviewId, Long userId) {
        log.info("ReviewService: снятие дизлайка с отзыва {} пользователем {}", reviewId, userId);
        checkReviewExists(reviewId);
        userService.checkUserExists(userId);

        if (!reviewStorage.deleteDislikeIfExists(reviewId, userId)) {
            log.info("ReviewService: дизлайка от пользователя ID {} у отзыва ID {} не было", userId, reviewId);
        }
    }

    public void checkReviewExists(Long reviewId) {
        if (!reviewStorage.isReviewExists(reviewId)) {
            throw new NotFoundException(String.format(REVIEW_NOT_FOUND, reviewId));
        }
    }

    private Review getReviewOrThrow(Long reviewId) {
        return reviewStorage.findById(reviewId)
                .orElseThrow(() -> new NotFoundException(String.format(REVIEW_NOT_FOUND, reviewId)));
    }
}