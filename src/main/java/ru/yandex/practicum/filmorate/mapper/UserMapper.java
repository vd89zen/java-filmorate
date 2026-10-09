package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.Role;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserMapper {

    public static User mapToUser(NewUserRequest newUserRequest) {
        return User.builder()
                .email(newUserRequest.getEmail())
                .login(newUserRequest.getLogin())
                .name(newUserRequest.getName())
                .birthday(newUserRequest.getBirthday())
                .password(newUserRequest.getPassword())
                .role(Role.USER)
                .build();
    }

    public static UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .login(user.getLogin())
                .name(user.getName())
                .birthday(user.getBirthday())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static UserPublicDto mapToUserPublicDto(User user) {
        return UserPublicDto.builder()
                .id(user.getId())
                .login(user.getLogin())
                .name(user.getName())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static void updateUserFields(User user, UpdateUserRequest request) {
        if (request.hasEmail()) {
            user.setEmail(request.getEmail());
        }
        if (request.hasLogin()) {
            user.setLogin(request.getLogin());
        }
        if (request.hasName()) {
            user.setName(request.getName());
        }
        if (request.hasBirthday()) {
            user.setBirthday(request.getBirthday());
        }
    }
}