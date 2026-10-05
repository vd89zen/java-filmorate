package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.NewUserRequest;
import ru.yandex.practicum.filmorate.dto.UpdateUserRequest;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.service.UserService;
import java.util.*;

@Validated
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody NewUserRequest newUserRequest) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.create(newUserRequest));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> findById(@PathVariable @NotNull @Positive Long userId) {
        return ResponseEntity
                .ok(userService.findById(userId));
    }

    @GetMapping
    public ResponseEntity<Collection<UserDto>> findAll() {
        return ResponseEntity
                .ok(userService.findAll());
    }

    @PutMapping
    public ResponseEntity<UserDto> update(@Valid @RequestBody UpdateUserRequest updateUserRequest) {
        return ResponseEntity
                .ok(userService.update(updateUserRequest));
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

    @GetMapping("/{userId}/feed/friends")
    public ResponseEntity<List<Event>> getFeedFriends(@PathVariable Long userId) {
        return ResponseEntity
                .ok(userService.getFeedFriends(userId));
    }

    @GetMapping("/{userId}/feed/user")
    public ResponseEntity<List<Event>> getFeedUser(@PathVariable Long userId) {
        return ResponseEntity
                .ok(userService.getFeedUser(userId));
    }
}