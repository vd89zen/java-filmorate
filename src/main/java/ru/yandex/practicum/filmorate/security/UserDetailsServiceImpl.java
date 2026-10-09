package ru.yandex.practicum.filmorate.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.UserDbStorage;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserDbStorage userStorage;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("UserDetailsService: попытка аутентификации email={}", email);

        String normalized = email == null ? null : email.trim().toLowerCase(Locale.ROOT);

        User user = userStorage.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format("Пользователь с email %s не найден.", email)));

        return new UserPrincipal(user);
    }
}