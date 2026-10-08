package ru.yandex.practicum.filmorate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Builder
@Data
@EqualsAndHashCode(of = {"id"})
public class UserPublicDto {
    Long id;
    String login;
    String name;
    LocalDate birthday;
}