package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.model.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserMapper {

    public static User mapToUser(NewUserRequest newUserRequest) {
        User user = User.builder()
                .email(newUserRequest.getEmail())
                .login(newUserRequest.getLogin())
                .name(newUserRequest.getName())
                .birthday(newUserRequest.getBirthday())
                .build();
        return user;
    }

    public static UserDto mapToUserDto(User user) {
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .login(user.getLogin())
                .name(user.getName())
                .birthday(user.getBirthday())
                .build();
        return userDto;
    }

    public static User updateUserFields(User updatingUser, UpdateUserRequest request) {
        if (request.hasEmail()) {
            updatingUser.setEmail(request.getEmail());
        }

        if (request.hasLogin()) {
            updatingUser.setLogin(request.getLogin());
        }

        if (request.hasName()) {
            updatingUser.setName(request.getName());
        }

        if (request.hasBirthday()) {
            updatingUser.setBirthday(request.getBirthday());
        }

        return updatingUser;
    }
}