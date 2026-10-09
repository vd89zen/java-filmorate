package ru.yandex.practicum.filmorate.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;
import ru.yandex.practicum.filmorate.util.TrimDeserializer;

@Data
public class LoginRequest {
    @NotBlank
    @Email
    @JsonDeserialize(using = TrimDeserializer.class)
    private String email;

    @NotBlank
    @ToString.Exclude
    private String password;
}