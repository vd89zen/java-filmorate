package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.dto.NewUserRequest;
import ru.yandex.practicum.filmorate.dto.UpdateUserRequest;
import ru.yandex.practicum.filmorate.dto.UserDto;
import ru.yandex.practicum.filmorate.mapper.UserMapper;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;
import ru.yandex.practicum.filmorate.model.enums.OperationTypes;
import ru.yandex.practicum.filmorate.model.interfaces.UserStorage;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;
    private final FriendshipDbStorage friendshipDbStorage;
    private final EventService eventService;

    public void checkUserExists(Long userId) {
        if (userStorage.isUserExists(userId) == false) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден.", userId));
        }
    }

    @Transactional
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

    @Transactional
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
        log.info("Пользователь ID {} успешно удалён.", userId);
    }

    @Transactional
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
        if (friendshipDbStorage.isFriend(userId, friendId)) {
            log.info("UserService: Друг уже был добавлен ранее.");
        } else {
            friendshipDbStorage.addFriend(userId, friendId);
            log.info("UserService: Друг успешно добавлен.");
            eventService.addEvent(userId, EventTypes.FRIEND, OperationTypes.ADD, friendId);
            log.info("UserService: Добавлено событие (add friend) в ленту пользователя.");
        }
    }

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        checkUserExists(userId);
        checkUserExists(friendId);
        if (friendshipDbStorage.isFriend(userId, friendId)) {
            friendshipDbStorage.removeFriend(userId, friendId);
            log.info("UserService: Пользователь ID {} успешно удален из друзей пользователя ID {}.", friendId, userId);
            eventService.addEvent(userId, EventTypes.FRIEND, OperationTypes.REMOVE, friendId);
            log.info("UserService: Добавлено событие (remove friend) в ленту пользователя.");
        } else {
            log.info("UserService: Пользователь ID {} не является другом пользователя ID {}.", friendId, userId);
        }
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUserFriends(Long userId) {
        log.info("Получение списка друзей пользователя ID {}.", userId);
        checkUserExists(userId);
        List<User> userFriends = userStorage.findBySeveralIds(friendshipDbStorage.getFriendsIdsOfUser(userId));
        userFriends.sort(Comparator.comparing(User::getId));

        return userFriends.stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getCommonFriends(Long userId, Long otherUserId) {
        log.info("Получение списка общих друзей пользователей ID {} и {}.", userId, otherUserId);
        checkUserExists(userId);
        checkUserExists(otherUserId);
        List<User> commonFriends = userStorage.findBySeveralIds(
                friendshipDbStorage.getCommonFriends(userId, otherUserId)
        );

        return commonFriends.stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Event> getFeedFriends(Long userId) {
        log.info("Получение событий друзей пользователя ID {}.", userId);
        checkUserExists(userId);
        return eventService.getFeedFriends(userId);
    }

    @Transactional(readOnly = true)
    public List<Event> getFeedUser(Long userId) {
        log.info("Получение событий пользователя ID {}.", userId);
        checkUserExists(userId);
        return eventService.getFeedUser(userId);
    }

    private User getUserOrThrow(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден.", id)));
    }
}