package ru.yandex.practicum.filmorate.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Data
@Component
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {
    private String email;
    private String login;
    private String name;
    private LocalDate birthday;
    @ToString.Exclude
    private String password;
}