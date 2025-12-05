package ru.yandex.practicum.filmorate.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(of = "id")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateUserRequest {
    @NotNull(message = "При обновлении ID пользователя не может быть null.")
    @Positive(message = "ID пользователя не может быть меньше 1.")
    Long id;
    @Email(message = "Неверный формат адреса электронной почты.")
    String email;
    @Pattern(regexp = ".*\\S+.*",
            message = "(Если поле не null: login должен содержать хотя бы один непробельный символ.")
    String login;
    @Pattern(regexp = ".*\\S+.*",
            message = "(Если поле не null: имя должно содержать хотя бы один непробельный символ.")
    String name;
    @Past(message = "Дата рождения не может быть в будущем.")
    LocalDate birthday;

    public boolean hasEmail() {
        return email != null;
    }

    public boolean hasLogin() {
        return login != null;
    }

    public boolean hasName() {
        return name != null;
    }

    public boolean hasBirthday() {
        return birthday != null;
    }
}