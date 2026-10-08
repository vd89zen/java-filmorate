package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(of = "email")
@AllArgsConstructor
@NoArgsConstructor
public class NewUserRequest {
    @NotBlank(message = "Не указана электронная почта (email).")
    @Email(message = "Неверный формат адреса электронной почты.")
    String email;

    @NotBlank(message = "Не указан логин (login).")
    String login;

    String name;

    @NotNull(message = "Не указана дата рождения.")
    @Past(message = "Дата рождения не может быть в будущем.")
    LocalDate birthday;

    @NotBlank(message = "Не указан пароль.")
    @Size(min = 6, max = 100, message = "Пароль должен быть от 6 до 100 символов.")
    String password;
}