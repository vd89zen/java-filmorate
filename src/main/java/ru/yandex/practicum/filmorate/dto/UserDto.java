package ru.yandex.practicum.filmorate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Builder
@Data
@EqualsAndHashCode(of = {"id"})
public class UserDto {
    Long id;
    String email;
    String login;
    String name;
    LocalDate birthday;
}
