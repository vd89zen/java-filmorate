package ru.yandex.practicum.filmorate.controller.pub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.NewUserRequest;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.dto.UserPublicDto;
import ru.yandex.practicum.filmorate.service.UserService;

@Validated
@RestController
@RequestMapping("/users")
public class UserPublicController {
    private final UserService userService;

    public UserPublicController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody NewUserRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.create(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserPublicDto> findById(@PathVariable @NotNull @Positive Long userId) {
        return ResponseEntity.ok(userService.findPublicById(userId));
    }
}