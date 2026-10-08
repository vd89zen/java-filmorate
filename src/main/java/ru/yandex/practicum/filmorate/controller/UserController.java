package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.service.FeedService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import java.util.*;

@Validated
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final FeedService feedService;
    private final FilmService filmService;

    public UserController(UserService userService, FeedService feedService, FilmService filmService) {
        this.userService = userService;
        this.feedService = feedService;
        this.filmService = filmService;
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody NewUserRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.create(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> findById(@PathVariable @NotNull @Positive Long userId) {
        return ResponseEntity
                .ok(userService.findById(userId));
    }

    @GetMapping
    public ResponseEntity<Collection<UserDto>> findAll(
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive @Max(100) int size) {
        return ResponseEntity
                .ok(userService.findAll(from, size));
    }

    @PutMapping
    public ResponseEntity<UserDto> update(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity
                .ok(userService.update(request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/friends/{friendId}")
    public ResponseEntity<Void> addFriend(@PathVariable @NotNull @Positive Long userId,
                                          @PathVariable @NotNull @Positive Long friendId) {
        userService.addFriend(userId, friendId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable @NotNull @Positive Long userId,
                                             @PathVariable @NotNull @Positive Long friendId) {
        userService.removeFriend(userId, friendId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/friends")
    public ResponseEntity<List<UserDto>> getFriends(@PathVariable @NotNull @Positive Long userId) {
        return ResponseEntity
                .ok(userService.getUserFriends(userId));
    }

    @GetMapping("/{userId}/friends/common/{friendId}")
    public ResponseEntity<List<UserDto>> getCommonFriends(@PathVariable @NotNull @Positive Long userId,
                                                          @PathVariable @NotNull @Positive Long friendId) {
        return ResponseEntity
                .ok(userService.getCommonFriends(userId, friendId));
    }

    @GetMapping("/{userId}/recommendations")
    public ResponseEntity<List<FilmDto>> getRecommendations(
            @PathVariable @NotNull @Positive Long userId) {
        return ResponseEntity.ok(filmService.getRecommendations(userId));
    }

    // «Сырые» ленты событий — id объектов.

    @GetMapping("/{userId}/feed/friends")
    public ResponseEntity<List<EventDto>> getFeedFriends(@PathVariable Long userId) {
        return ResponseEntity
                .ok(feedService.getFeedFriends(userId));
    }

    @GetMapping("/{userId}/feed/user")
    public ResponseEntity<List<EventDto>> getFeedUser(@PathVariable Long userId) {
        return ResponseEntity
                .ok(feedService.getFeedUser(userId));
    }

    // Обогащённые ленты событий — с полными FilmDto / UserDto.

    @GetMapping("/{userId}/feed/friends/enriched")
    public ResponseEntity<List<EnrichedEventDto>> getEnrichedFeedFriends(@PathVariable Long userId) {
        return ResponseEntity
                .ok(feedService.getEnrichedFeedFriends(userId));
    }

    @GetMapping("/{userId}/feed/user/enriched")
    public ResponseEntity<List<EnrichedEventDto>> getEnrichedFeedUser(@PathVariable Long userId) {
        return ResponseEntity
                .ok(feedService.getEnrichedFeedUser(userId));
    }
}