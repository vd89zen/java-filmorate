package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.error.ValidationError;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.interfaces.UserStorage;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {
    private final UserStorage userStorage;
    private Long nextId;

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;

        log.info("Определение начального значения для генерации ID пользователей.");
        Collection<User> allUsers = userStorage.findAll();
        if (allUsers.isEmpty()) {
            this.nextId = 1L;
        } else {
            long maxId = allUsers.stream()
                    .mapToLong(User::getId)
                    .max()
                    .orElse(0L);

            this.nextId = (maxId == Long.MAX_VALUE) ? 1 : maxId + 1;
        }
        log.info("Начальное значение для генерации ID пользователей: {}.", this.nextId);
    }

    public User create(User newUser) {
        log.info("Создание нового пользователя: {}.", newUser);

        if (newUser.getId() != null) {
            throw new ValidationException(ValidationError.builder()
                    .field("id")
                    .message("При создании нового пользователя id должен быть null.")
                    .rejectedValue(newUser.getId())
                    .build());
        }

        if (newUser.getFriends().isEmpty() == false) {
            throw new ValidationException(ValidationError.builder()
                    .field("friends")
                    .message("При создании нового пользователя список друзей должен быть пустым.")
                    .rejectedValue(newUser.getFriends())
                    .build());
        }

        newUser.setId(getNextId());

        if (newUser.getName() == null || newUser.getName().isEmpty()) {
            newUser.setName(newUser.getLogin());
            log.info("Так как имя пользователя не указано, для него использован login {}.", newUser.getLogin());
        }

        User createdUser = userStorage.create(newUser.selfCopy());
        log.info("Успешно создан новый пользователь {}.", createdUser);
        return createdUser;
    }

    public User update(User user) {
        log.info("Обновление пользователя: {}.", user);

        if (user.getId() == null) {
            throw new ValidationException(ValidationError.builder()
                    .field("id")
                    .message("Не указан Id.")
                    .rejectedValue(null)
                    .build());
        }

        User oldUser = getUserOrThrow(user.getId());

        String newName;
        if (user.getName() == null || user.getName().isEmpty()) {
            newName = oldUser.getName();
            log.info("Имя пользователя не будет обновлено, так как не указано.");
        } else {
            newName = user.getName();
        }

        User updatedUser = User.builder()
                .id(user.getId())
                .email(user.getEmail())
                .login(user.getLogin())
                .name(newName)
                .birthday(user.getBirthday())
                .friends(new HashSet<>(oldUser.getFriends()))
                .build();
        log.info("Успешно обновлён пользователь: {}.", updatedUser);
        return userStorage.update(updatedUser);
    }

    public void delete(long userId) {
        log.info("Удаление пользователя ID {}.", userId);
        User deletingUser = getUserOrThrow(userId);
        Set<Long> friendsOfDeletingUser = deletingUser.getFriends();

        for (Long friendId : friendsOfDeletingUser) {
            User friend = getUserOrThrow(friendId);
            friend.getFriends().remove(userId);
            userStorage.update(friend);
            log.info("Пользователь ID {} удалён из списка друзей пользователя ID {}.", userId, friendId);
        }

        userStorage.delete(userId);
        log.info("Пользователь ID {} успешно удалён.", userId);
    }

    public User findById(long userId) {
        log.info("Поиск пользователя ID {}.", userId);
        return getUserOrThrow(userId);
    }

    public Collection<User> findAll() {
        log.info("Получение списка всех пользователей.");
        return userStorage.findAll();
    }

    public void addFriend(long userId, long friendId) {
        log.info("Добавление в друзья: пользователь ID {}, пользователь ID {}.", userId, friendId);
        if (userId == friendId) {
            throw new ValidationException(ValidationError.builder()
                    .field("id")
                    .message("При добавление в друзья ID не должны совпадать.")
                    .rejectedValue(userId + " = " + friendId)
                    .build());
        }

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        userStorage.update(user);
        userStorage.update(friend);
        log.info("Успешное добавление в друзья: пользователь ID {}, пользователь ID {}.", userId, friendId);
    }

    public void removeFriend(long userId, long friendId) {
        log.info("Удаление из друзей: пользователь ID {}, пользователь ID {}.", userId, friendId);
        if (userId == friendId) {
            throw new ValidationException(ValidationError.builder()
                    .field("id")
                    .message("При удалении из друзей ID не должны совпадать.")
                    .rejectedValue(userId + " = " + friendId)
                    .build());
        }

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        userStorage.update(user);
        userStorage.update(friend);
        log.info("Успешное удаление из друзей: пользователь ID {}, пользователь ID {}.", userId, friendId);
    }

    public List<User> getFriends(long userId) {
        log.info("Получение друзей пользователя ID {}.", userId);
        return getUserOrThrow(userId).getFriends().stream()
                .map(id -> userStorage.findById(id).get())
                .collect(Collectors.toUnmodifiableList());
    }

    public List<User> getCommonFriends(long firstUserId, long secondUserId) {
        log.info("Поиск общих друзей: пользователь ID {}, пользователь ID {}.", firstUserId, secondUserId);
        User firstUser = getUserOrThrow(firstUserId);
        User secondUser = getUserOrThrow(secondUserId);

        Set<Long> friendsFirstUser = firstUser.getFriends();
        Set<Long> friendsSecondUser = secondUser.getFriends();

        return friendsFirstUser.stream()
                .filter(friendsSecondUser::contains)
                .map(id -> userStorage.findById(id).get())
                .collect(Collectors.toUnmodifiableList());
    }

    private User getUserOrThrow(long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден.", id)));
    }

    private synchronized long getNextId() {
        return nextId++;
    }
}
