package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.ErrorResponse;
import ru.yandex.practicum.filmorate.model.ValidationError;
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

        log.warn("Произошла ошибка валидации (MethodArgumentNotValid): {}", errors);
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ValidationException.class})
    public ErrorResponse handleValidationException(ValidationException exception) {
        log.warn("Ошибка валидации: {}", exception.getMessage());
        List<ValidationError> errors = Collections.singletonList(exception.getValidationError());
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NotFoundException.class})
    public ErrorResponse handleNotFoundException(NotFoundException exception) {
        log.warn("Ресурс не найден: {}", exception.getMessage());
        List<ValidationError> errors = Collections.singletonList(
                new ValidationError(null, exception.getMessage(), null));
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ConstraintViolationException.class})
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException exception) {
        List<ValidationError> errors = exception.getConstraintViolations().stream()
                .map(v -> new ValidationError(
                        v.getPropertyPath().toString(),
                        v.getMessage(),
                        v.getInvalidValue()))
                .collect(Collectors.toList());

        log.warn("Произошла ошибка валидации параметров (ConstraintViolation): {}", errors);
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public ErrorResponse handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        log.warn("Неподдерживаемый HTTP-метод: {}", exception.getMessage());
        List<ValidationError> errors = Collections.singletonList(
                new ValidationError("method", exception.getMessage(), exception.getMethod()));
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NoResourceFoundException.class})
    public ErrorResponse handleNoResourceFoundException(NoResourceFoundException exception) {
        log.warn("Запрошен неизвестный URL: {}", exception.getMessage());
        List<ValidationError> errors = Collections.singletonList(
                new ValidationError(null, exception.getMessage(), null));
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ErrorResponse handleMessageNotReadable(HttpMessageNotReadableException exception) {
        log.warn("Некорректное тело запроса: {}", exception.getMessage());
        List<ValidationError> errors = Collections.singletonList(
                new ValidationError("body", "Тело запроса не соответствует ожидаемому формату", null));
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({AuthenticationException.class})
    public ErrorResponse handleAuthenticationException(AuthenticationException exception) {
        log.warn("Ошибка аутентификации: {}", exception.getMessage());
        List<ValidationError> errors = Collections.singletonList(
                new ValidationError(null, exception.getMessage(), null));
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MissingServletRequestParameterException.class})
    public ErrorResponse handleMissingServletRequestParameter(MissingServletRequestParameterException exception) {
        log.warn("Отсутствует обязательный параметр: {}", exception.getMessage());
        List<ValidationError> errors = Collections.singletonList(
                new ValidationError(
                        exception.getParameterName(),
                        "Обязательный параметр не указан",
                        null));
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception exception) {
        log.error("Непредвиденная ошибка сервера", exception);
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