package ru.yandex.practicum.filmorate.storage;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final ConcurrentHashMap<Long, User> users = new ConcurrentHashMap<>();

    @Override
    public User create(User user) {
        users.put(user.getId(), user);
        return user.selfCopy();
    }

    @Override
    public User update(User user) {
        users.put(user.getId(), user);
        return user.selfCopy();
    }

    @Override
    public void delete(long userId) {
        users.remove(userId);
    }

    @Override
    public Optional<User> findById(long userId) {
        return Optional.ofNullable(users.get(userId))
                .map(User::selfCopy);
    }

    @Override
    @Cacheable("users")
    public Collection<User> findAll() {
        return users.values().stream()
                .map(User::selfCopy)
                .collect(Collectors.toUnmodifiableList());
    }
}
