package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Builder
@Data
@EqualsAndHashCode(of = {"id", "email"})
@AllArgsConstructor
public class User {
    Long id;
    @NotBlank(message = "Не указана электронная почта (email).")
    @Email(message = "Неверный формат адреса электронной почты.")
    String email;
    @NotBlank(message = "Не указан логин (login).")
    String login;
    String name;
    @Past(message = "Дата рождения не может быть в будущем.")
    LocalDate birthday;
}
