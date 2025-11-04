package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import java.util.*;

@Slf4j
@Validated
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @CacheEvict(value = "users", allEntries = true)
    public ResponseEntity<User> create(@Valid @RequestBody User newUser) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.create(newUser));
    }

    @PutMapping
    @CacheEvict(value = "users", key = "#user.id")
    public ResponseEntity<User> update(@Valid @RequestBody User user) {
        return ResponseEntity
                .ok(userService.update(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> findById(@PathVariable long id) {
        return ResponseEntity
                .ok(userService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Collection<User>> findAll() {
        return ResponseEntity
                .ok(userService.findAll());
    }

    @PutMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> addFriend(@PathVariable long id, @PathVariable long friendId) {
        userService.addFriend(id, friendId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable long id, @PathVariable long friendId) {
        userService.removeFriend(id, friendId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/friends")
    public ResponseEntity<List<User>> getFriends(@PathVariable long id) {
        return ResponseEntity
                .ok(userService.getFriends(id));
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<User>> getCommonFriends(@PathVariable long id, @PathVariable long otherId) {
        return ResponseEntity
                .ok(userService.getCommonFriends(id, otherId));
    }
}
