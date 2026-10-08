package ru.yandex.practicum.filmorate.controller.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.service.FeedService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/me")
public class UserAuthController {
    private final UserService userService;
    private final FeedService feedService;
    private final FilmService filmService;

    public UserAuthController(UserService userService, FeedService feedService, FilmService filmService) {
        this.userService = userService;
        this.feedService = feedService;
        this.filmService = filmService;
    }

    // Обновление своего профиля (id в теле запроса)
    // TODO(auth): убрать id из тела — брать из SecurityContext
    @PutMapping
    public ResponseEntity<UserDto> update(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity
                .ok(userService.update(request));
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteSelf(@PathVariable @NotNull @Positive Long userId) {
        userService.delete(userId);
        return ResponseEntity
                .noContent().build();
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @PutMapping("/{userId}/friends/{friendId}")
    public ResponseEntity<Void> addFriend(@PathVariable @NotNull @Positive Long userId,
                                          @PathVariable @NotNull @Positive Long friendId) {
        userService.addFriend(userId, friendId);
        return ResponseEntity
                .noContent().build();
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @DeleteMapping("/{userId}/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable @NotNull @Positive Long userId,
                                             @PathVariable @NotNull @Positive Long friendId) {
        userService.removeFriend(userId, friendId);
        return ResponseEntity
                .noContent().build();
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @GetMapping("/{userId}/friends")
    public ResponseEntity<List<UserPublicDto>> getFriends(@PathVariable @NotNull @Positive Long userId) {
        return ResponseEntity
                .ok(userService.getUserFriends(userId));
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @GetMapping("/{userId}/friends/common/{friendId}")
    public ResponseEntity<List<UserPublicDto>> getCommonFriends(
            @PathVariable @NotNull @Positive Long userId,
            @PathVariable @NotNull @Positive Long friendId) {
        return ResponseEntity
                .ok(userService.getCommonFriends(userId, friendId));
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @GetMapping("/{userId}/recommendations")
    public ResponseEntity<List<FilmDto>> getRecommendations(@PathVariable @NotNull @Positive Long userId) {
        return ResponseEntity
                .ok(filmService.getRecommendations(userId));
    }

    // Ленты событий

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @GetMapping("/{userId}/feed/user")
    public ResponseEntity<List<EventDto>> getFeedUser(@PathVariable @NotNull @Positive Long userId) {
        return ResponseEntity
                .ok(feedService.getFeedUser(userId));
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @GetMapping("/{userId}/feed/friends")
    public ResponseEntity<List<EventDto>> getFeedFriends(@PathVariable @NotNull @Positive Long userId) {
        return ResponseEntity
                .ok(feedService.getFeedFriends(userId));
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @GetMapping("/{userId}/feed/user/enriched")
    public ResponseEntity<List<EnrichedEventDto>> getEnrichedFeedUser(
            @PathVariable @NotNull @Positive Long userId) {
        return ResponseEntity
                .ok(feedService.getEnrichedFeedUser(userId));
    }

    // TODO(auth): убрать {userId} из пути — брать из SecurityContext
    @GetMapping("/{userId}/feed/friends/enriched")
    public ResponseEntity<List<EnrichedEventDto>> getEnrichedFeedFriends(
            @PathVariable @NotNull @Positive Long userId) {
        return ResponseEntity
                .ok(feedService.getEnrichedFeedFriends(userId));
    }
}