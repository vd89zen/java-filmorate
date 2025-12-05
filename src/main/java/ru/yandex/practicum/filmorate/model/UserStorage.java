package ru.yandex.practicum.filmorate.model;

import java.util.List;
import java.util.Optional;

public interface UserStorage {
    User create(User user);

    void update(User user);

    boolean delete(Long userId);

    Optional<User> findById(Long userId);

    List<User> findBySeveralIds(List<Long> usersIds);

    List<User> findAll();

    Optional<User> findByEmail(String email);

    boolean isEmailAlreadyUse(String email);

    boolean isUserExists(Long userId);
}
