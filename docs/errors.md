# Обработка ошибок

Все ошибки — в едином формате `ErrorResponse`:

```json
{
  "errors": [
    {
      "field": "email",
      "message": "Неверный формат адреса электронной почты.",
      "rejectedValue": "invalid-email"
    }
  ],
  "timestamp": "2026-09-16T15:20:11.123"
}
```

## Соответствие исключений и кодов

| Исключение | HTTP-статус |
|---|---|
| `MethodArgumentNotValidException` | `400 Bad Request` |
| `ConstraintViolationException` | `400 Bad Request` |
| `ValidationException` | `400 Bad Request` |
| `MissingServletRequestParameterException` | `400 Bad Request` |
| `HttpMessageNotReadableException` | `400 Bad Request` |
| `NotFoundException` | `404 Not Found` |
| `NoResourceFoundException` | `404 Not Found` |
| `HttpRequestMethodNotSupportedException` | `405 Method Not Allowed` |
| `AuthenticationException` (в т.ч. `BadCredentialsException`, `UsernameNotFoundException`) | `401 Unauthorized` |
| Анонимный доступ к защищённому эндпоинту (Spring Security) | `401 Unauthorized` |
| Доступ без нужной роли (Spring Security) | `403 Forbidden` |
| любое другое | `500 Internal Server Error` |

## Разделение ответственности за 401/403

Два разных слоя — потому что ошибки возникают в разных местах.

**Spring Security** отвечает за 401/403, когда запрос **не доходит** до контроллера:
- анонимный на `/me/**` → 401;
- без роли `ADMIN` на `/admin/**` → 403.

Настраивается в `SecurityConfig.exceptionHandling` (`HttpStatusEntryPoint` + `AccessDeniedHandlerImpl`).

**`GlobalExceptionHandler`** отвечает за те же коды, когда исключение возникло **внутри**
контроллера: `BadCredentialsException` из `AuthController.login` → 401.

Причина разделения: исключения Spring Security обрабатываются внутри цепочки фильтров,
до `DispatcherServlet`, и `@RestControllerAdvice` их просто не видит.