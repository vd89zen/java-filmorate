package ru.yandex.practicum.filmorate.controller.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.security.UserPrincipal;
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

    @PutMapping
    public ResponseEntity<UserDto> update(@Valid @RequestBody UpdateUserRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        // Пользователь может обновить только свой профиль — id всегда из токена.
        request.setId(principal.getId());
        return ResponseEntity
                .ok(userService.update(request));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteSelf(@AuthenticationPrincipal UserPrincipal principal) {
        userService.delete(principal.getId());
        return ResponseEntity
                .noContent().build();
    }

    @PutMapping("/friends/{friendId}")
    public ResponseEntity<Void> addFriend(@PathVariable @NotNull @Positive Long friendId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        userService.addFriend(principal.getId(), friendId);
        return ResponseEntity
                .noContent().build();
    }

    @DeleteMapping("/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable @NotNull @Positive Long friendId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        userService.removeFriend(principal.getId(), friendId);
        return ResponseEntity
                .noContent().build();
    }

    @GetMapping("/friends")
    public ResponseEntity<List<UserPublicDto>> getFriends(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity
                .ok(userService.getUserFriends(principal.getId()));
    }

    @GetMapping("/friends/common/{friendId}")
    public ResponseEntity<List<UserPublicDto>> getCommonFriends(
            @PathVariable @NotNull @Positive Long friendId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity
                .ok(userService.getCommonFriends(principal.getId(), friendId));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<FilmDto>> getRecommendations(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity
                .ok(filmService.getRecommendations(principal.getId()));
    }

    // Ленты событий

    @GetMapping("/feed/user")
    public ResponseEntity<List<EventDto>> getFeedUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity
                .ok(feedService.getFeedUser(principal.getId()));
    }

    @GetMapping("/feed/friends")
    public ResponseEntity<List<EventDto>> getFeedFriends(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity
                .ok(feedService.getFeedFriends(principal.getId()));
    }

    @GetMapping("/feed/user/enriched")
    public ResponseEntity<List<EnrichedEventDto>> getEnrichedFeedUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity
                .ok(feedService.getEnrichedFeedUser(principal.getId()));
    }

    @GetMapping("/feed/friends/enriched")
    public ResponseEntity<List<EnrichedEventDto>> getEnrichedFeedFriends(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity
                .ok(feedService.getEnrichedFeedFriends(principal.getId()));
    }
}