package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.error.ValidationError;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/users")
@Validated
@Slf4j
public class UserController {
    private final ConcurrentHashMap<Long, User> users = new ConcurrentHashMap<>();

    private synchronized long getNextId() {
        return users.keySet().isEmpty()
                ? 1
                : Collections.max(users.keySet()) + 1;
    }

    @GetMapping
    @Cacheable("users")
    public Collection<User> findAll() {
        return users.values();
    }

    @PostMapping
    @CacheEvict(value = "users", allEntries = true)
    public User create(@Valid @RequestBody User newUser) {
        log.info("Попытка создания нового пользователя: {}", newUser);

        if (newUser.getId() != null) {
            throw new ValidationException(ValidationError.builder()
                    .field("id")
                    .message("При создании нового пользователя, id должен быть null.")
                    .rejectedValue(newUser.getId())
                    .build());
        }

        newUser.setId(getNextId());

        if (newUser.getName() == null || newUser.getName().isEmpty()) {
            newUser.setName(newUser.getLogin());
            log.info("Так как имя пользователя не указано, для него использован login.");
        }

        users.put(newUser.getId(), newUser);
        log.info("Успешно создан новый пользователь {}.", newUser);
        return newUser;
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        log.info("Попытка обновления пользователя: {}.", user);

        if (user.getId() == null) {
            throw new ValidationException(ValidationError.builder()
                    .field("id")
                    .message("Не указан Id.")
                    .rejectedValue(null)
                    .build());
        }

        if (users.containsKey(user.getId())) {
            users.put(user.getId(), user);
            log.info("Успешно обновлён пользователь: {}.", user);
            return user;
        } else {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден.", user.getId()));
        }
    }
}
