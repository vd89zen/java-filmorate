package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.dto.NewUserRequest;
import ru.yandex.practicum.filmorate.dto.UpdateUserRequest;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.mapper.UserMapper;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;
    private final FriendshipDbStorage friendshipDbStorage;

    public UserDto create(NewUserRequest newUserRequest) {
        log.info("Создание нового пользователя: {}.", newUserRequest);

        if (userStorage.isEmailAlreadyUse(newUserRequest.getEmail())) {
            throw new ValidationException(ValidationError.builder()
                    .field("email")
                    .message("Данный email уже используется.")
                    .rejectedValue(newUserRequest.getEmail())
                    .build());
        }

        if (newUserRequest.getName().isBlank()) {
            newUserRequest.setName(newUserRequest.getLogin());
            log.info("Так как имя пользователя не указано, для него использован login {}.", newUserRequest.getLogin());
        }

        User newUser = UserMapper.mapToUser(newUserRequest);
        newUser = userStorage.create(newUser);
        return UserMapper.mapToUserDto(newUser);
    }

    public UserDto update(UpdateUserRequest updateUserRequest) {
        log.info("Обновление пользователя: {}.", updateUserRequest);

        Long userId = updateUserRequest.getId();
        User updatingUser = getUserOrThrow(userId);
        updatingUser = UserMapper.updateUserFields(updatingUser, updateUserRequest);
        userStorage.update(updatingUser);

        return UserMapper.mapToUserDto(updatingUser);
    }

    public void delete(Long userId) {
        log.info("Удаление пользователя ID {}.", userId);
        if (userStorage.delete(userId) == false) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден.", userId));
        }
    }

    public UserDto findById(Long userId) {
        log.info("Поиск пользователя ID {}.", userId);
        User user = getUserOrThrow(userId);
        return UserMapper.mapToUserDto(user);
    }

    public List<UserDto> findAll() {
        log.info("Получение списка всех пользователей.");
        List<User> users = userStorage.findAll();

        return users.stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toList());
    }

    public void checkUserExists(Long userId) {
        if (userStorage.isUserExists(userId) == false) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден.", userId));
        }
    }

    private User getUserOrThrow(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден.", id)));
    }

    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException(ValidationError.builder()
                    .field("friendship")
                    .message("Нельзя добавить себя в друзья.")
                    .rejectedValue(String.format("пользователь ID %d, друг ID %d.", userId, friendId))
                    .build());
        }

        checkUserExists(userId);
        checkUserExists(friendId);
        friendshipDbStorage.addFriend(userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        checkUserExists(userId);
        checkUserExists(friendId);
        friendshipDbStorage.removeFriend(userId, friendId);
    }

    public List<UserDto> getUserFriends(Long userId) {
        log.info("Получение списка друзей пользователя ID {}.", userId);
        checkUserExists(userId);
        List<User> userFriends = userStorage.findBySeveralIds(friendshipDbStorage.getFriendsIdsOfUser(userId));
        userFriends.sort(Comparator.comparing(User::getId));

        return userFriends.stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toList());
    }

    public List<UserDto> getCommonFriends(Long userId, Long otherUserId) {
        checkUserExists(userId);
        checkUserExists(otherUserId);
        List<User> commonFriends = userStorage.findBySeveralIds(
                friendshipDbStorage.getCommonFriends(userId, otherUserId)
        );

        return commonFriends.stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toList());
    }
}