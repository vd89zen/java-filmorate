package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
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
}