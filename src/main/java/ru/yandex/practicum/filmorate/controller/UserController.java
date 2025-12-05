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

    @PutMapping
    public ResponseEntity<UserDto> update(@Valid @RequestBody UpdateUserRequest updateUserRequest) {
        return ResponseEntity
                .ok(userService.update(updateUserRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> findById(@PathVariable @NotNull @Positive Long id) {
        return ResponseEntity
                .ok(userService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Collection<UserDto>> findAll() {
        return ResponseEntity
                .ok(userService.findAll());
    }

    @PutMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> addFriend(@PathVariable @NotNull @Positive Long id,
                                          @PathVariable @NotNull @Positive Long friendId) {
        userService.addFriend(id, friendId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable @NotNull @Positive Long id,
                                             @PathVariable @NotNull @Positive Long friendId) {
        userService.removeFriend(id, friendId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/friends")
    public ResponseEntity<List<UserDto>> getFriends(@PathVariable @NotNull @Positive Long id) {
        return ResponseEntity
                .ok(userService.getUserFriends(id));
    }

    @GetMapping("/{id}/friends/common/{friendId}")
    public ResponseEntity<List<UserDto>> getCommonFriends(@PathVariable @NotNull @Positive Long id,
                                                      @PathVariable @NotNull @Positive Long friendId) {
        return ResponseEntity
                .ok(userService.getCommonFriends(id, friendId));
    }
}