package ru.yandex.practicum.filmorate.exception;

import ru.yandex.practicum.filmorate.error.ValidationError;

public class ValidationException extends RuntimeException {
    private final ValidationError validationError;

    public ValidationException(ValidationError validationError) {
        super(validationError.getMessage());
        this.validationError = validationError;
    }

    public ValidationError getValidationError() {
        return validationError;
    }
}
