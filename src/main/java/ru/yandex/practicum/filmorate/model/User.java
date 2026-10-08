package ru.yandex.practicum.filmorate.model;

import lombok.*;
import ru.yandex.practicum.filmorate.model.enums.Role;

import java.time.LocalDate;

@Builder
@Data
@EqualsAndHashCode(of = {"id", "email"})
@AllArgsConstructor
@NoArgsConstructor
public class User {
    Long id;
    String email;
    String login;
    String name;
    LocalDate birthday;
    @ToString.Exclude
    String password;
    Role role;
}

