package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class UpdatePasswordRequest {

    @NotBlank(message = "Не указан пароль.")
    @Size(min = 6, max = 100, message = "Пароль должен быть от 6 до 100 символов.")
    @ToString.Exclude
    private String password;
}