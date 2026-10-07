package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewDirectorRequest {
    @NotBlank
    @Size(max = 255)
    private String name;
}