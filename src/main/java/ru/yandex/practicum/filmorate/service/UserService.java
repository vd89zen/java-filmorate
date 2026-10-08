package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.mapper.UserMapper;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;
import ru.yandex.practicum.filmorate.model.enums.OperationTypes;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserDbStorage userStorage;
    private final FriendshipDbStorage friendshipDbStorage;
    private final EventService eventService;

    public void checkUserExists(Long userId) {
        if (userStorage.isUserExists(userId) == false) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден.", userId));
        }
    }

    @Transactional
    public UserDto create(NewUserRequest request) {
        log.info("Создание нового пользователя: {}.", request);

        if (userStorage.isEmailAlreadyUse(normalizeCredential(request.getEmail()))) {
            throw new ValidationException(ValidationError.builder()
                    .field("email")
                    .message("Данный email уже используется.")
                    .rejectedValue(request.getEmail())
                    .build());
        }

        if (request.getName() == null || request.getName().isBlank()) {
            request.setName(request.getLogin());
            log.info("Так как имя пользователя не указано, для него использован login {}.", request.getLogin());
        }

        User newUser = UserMapper.mapToUser(request);
        normalizeUser(newUser);
        User savedUser = userStorage.create(newUser);
        return UserMapper.mapToUserDto(savedUser);
    }

    public UserDto findById(Long userId) {
        log.info("Поиск пользователя ID {}.", userId);
        User user = getUserOrThrow(userId);
        return UserMapper.mapToUserDto(user);
    }

    public UserPublicDto findPublicById(Long userId) {
        log.info("Поиск публичного профиля пользователя ID {}.", userId);
        User user = getUserOrThrow(userId);
        return UserMapper.mapToUserPublicDto(user);
    }

    public List<UserDto> findAll(int from, int size) {
        log.info("Получение списка пользователей: from={}, size={}.", from, size);
        List<User> users = userStorage.findAll(from, size);

        return users.stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toList());
    }

    /**
     * Батч-получение пользователей(полный объект) по набору id.
     * Используется для обогащения ленты событий, чтобы не было N+1.
     */
    @Transactional(readOnly = true)
    public Map<Long, UserDto> findByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userStorage.findBySeveralIds(new ArrayList<>(userIds)).stream()
                .map(UserMapper::mapToUserDto)
                .collect(Collectors.toMap(UserDto::getId, userDto -> userDto));
    }

    @Transactional(readOnly = true)
    public Map<Long, UserShortDto> findShortByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userStorage.findBySeveralIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> UserShortDto.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .build()));
    }

    @Transactional
    public UserDto update(UpdateUserRequest request) {
        log.info("Обновление пользователя: {}.", request);
        Long userId = request.getId();
        User user = getUserOrThrow(userId);
        UserMapper.updateUserFields(user, request);
        normalizeUser(user);
        userStorage.update(user);
        return UserMapper.mapToUserDto(user);
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
    public List<UserPublicDto> getUserFriends(Long userId) {
        log.info("Получение списка друзей пользователя ID {}.", userId);
        checkUserExists(userId);
        List<User> userFriends = userStorage.findBySeveralIds(friendshipDbStorage.getFriendsIdsOfUser(userId));
        userFriends.sort(Comparator.comparing(User::getId));

        return userFriends.stream()
                .map(UserMapper::mapToUserPublicDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserPublicDto> getCommonFriends(Long userId, Long otherUserId) {
        log.info("Получение списка общих друзей пользователей ID {} и {}.", userId, otherUserId);
        checkUserExists(userId);
        checkUserExists(otherUserId);
        List<User> commonFriends = userStorage.findBySeveralIds(
                friendshipDbStorage.getCommonFriends(userId, otherUserId)
        );

        return commonFriends.stream()
                .map(UserMapper::mapToUserPublicDto)
                .collect(Collectors.toList());
    }

    private User getUserOrThrow(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден.", id)));
    }

    private void normalizeUser(User user) {
        user.setEmail(normalizeCredential(user.getEmail()));
        user.setLogin(normalizeCredential(user.getLogin()));
    }

    private String normalizeCredential(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}