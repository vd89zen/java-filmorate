package ru.yandex.practicum.filmorate.model;

import lombok.*;
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
}

