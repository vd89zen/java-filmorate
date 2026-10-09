package ru.yandex.practicum.filmorate.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.enums.Role;

import java.util.Locale;

@Profile("!test")
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final UserDbStorage userStorage;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = normalize(adminProperties.getEmail());

        if (userStorage.isEmailAlreadyUse(email)) {
            log.info("AdminInitializer: администратор {} уже существует, пропускаем создание", email);
            return;
        }

        User admin = User.builder()
                .email(email)
                .login(normalize(adminProperties.getLogin()))
                .name(adminProperties.getName())
                .birthday(adminProperties.getBirthday())
                .password(passwordEncoder.encode(adminProperties.getPassword()))
                .role(Role.ADMIN)
                .build();

        User saved = userStorage.create(admin);
        log.info("AdminInitializer: создан администратор id={}, email={}", saved.getId(), email);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}