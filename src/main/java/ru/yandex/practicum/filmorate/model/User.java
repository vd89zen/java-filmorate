package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Builder
@Data
@EqualsAndHashCode(of = {"id", "email"})
@AllArgsConstructor
@NoArgsConstructor
public class User {
    Long id;
    @NotBlank(message = "Не указана электронная почта (email).")
    @Email(message = "Неверный формат адреса электронной почты.")
    String email;
    @NotBlank(message = "Не указан логин (login).")
    String login;
    String name;
    @Past(message = "Дата рождения не может быть в будущем.")
    LocalDate birthday;
    @Builder.Default
    @Setter(AccessLevel.NONE)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Set<Long> friends = new HashSet<>();

    public User selfCopy() {
        return User.builder()
                .id(this.id)
                .email(this.email)
                .login(this.login)
                .name(this.name)
                .birthday(this.birthday)
                .friends(new HashSet<>(this.friends))
                .build();
    }
}
