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