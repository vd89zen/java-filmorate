package ru.yandex.practicum.filmorate.model;

import lombok.*;
import ru.yandex.practicum.filmorate.model.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
@EqualsAndHashCode(of = {"id", "email"})
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    private String email;
    private String login;
    private String name;
    private LocalDate birthday;

    @ToString.Exclude
    private String password;

    private Role role;
    private LocalDateTime createdAt;
}

