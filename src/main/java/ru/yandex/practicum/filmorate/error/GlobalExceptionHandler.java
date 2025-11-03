package ru.yandex.practicum.filmorate.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<ValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                    .map(this::extractValidationError)
                    .collect(Collectors.toList());

        log.error("Произошла ошибка валидации (MethodArgumentNotValid): {}", errors);
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ValidationException.class})
    public ErrorResponse handleValidationException(ValidationException exception) {
        log.error("Ошибка валидации: {}", exception.getMessage());
        List<ValidationError> errors = Collections.singletonList(exception.getValidationError());
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NotFoundException.class})
    public ErrorResponse handleNotFoundException(NotFoundException exception) {
        log.error("Ресурс не найден: {}", exception.getMessage());
        List<ValidationError> errors = Collections.singletonList(new ValidationError(null, exception.getMessage(), null));
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception exception) {
        List<ValidationError> error = Collections.singletonList(
                new ValidationError(
                        "server_error",
                        exception.getMessage(),
                        Map.of(
                                "cause",
                                Optional.ofNullable(exception.getCause())
                                        .map(cause -> cause.getClass().getSimpleName() + ": " + cause.getMessage())
                                        .orElse("null")
                        )
                )
        );
        log.error("Произошла внутренняя ошибка сервера : {}", error);

        return new ErrorResponse(Collections.singletonList(new ValidationError("INTERNAL_SERVER_ERROR",
                "Произошла непредвиденная ошибка. Обратитесь в поддержку.", "unknown reason")));
    }

    private ValidationError extractValidationError(FieldError fieldError) {
        return new ValidationError(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
        );
    }

}
