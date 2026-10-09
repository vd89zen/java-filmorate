package ru.yandex.practicum.filmorate.controller.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.Collection;

//TODO добавить админу возможность сброса пароля пользователя (уточнить - возможно это в секурити есть, тогда тут не надо)
@Validated
@RestController
@RequestMapping("/admin/users")
public class UserAdminController {
    private final UserService userService;

    public UserAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Collection<UserDto>> findAll(
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive @Max(100) int size) {
        return ResponseEntity.ok(userService.findAll(from, size));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(@PathVariable @NotNull @Positive Long userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }
}