package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Getter
public class ErrorResponse {
    final List<ValidationError> errors;
    final String timestamp = LocalDateTime.now().toString();
}