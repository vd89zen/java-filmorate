package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.config.JwtProperties;
import ru.yandex.practicum.filmorate.dto.AuthResponse;
import ru.yandex.practicum.filmorate.dto.LoginRequest;
import ru.yandex.practicum.filmorate.security.JwtService;
import ru.yandex.practicum.filmorate.security.UserDetailsServiceImpl;
import ru.yandex.practicum.filmorate.security.UserPrincipal;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("AuthController: попытка входа email={}", request.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        if (passwordEncoder.matches(request.getPassword(), userDetails.getPassword()) == false) {
            log.warn("AuthController: неверный пароль для email={}", request.getEmail());
            throw new BadCredentialsException("Неверный email или пароль.");
        }

        UserPrincipal principal = (UserPrincipal) userDetails;
        String token = jwtService.generateToken(principal.getUser());
        long expiresInSeconds = jwtProperties.getExpirationMinutes() * 60;

        log.info("AuthController: успешный вход email={}, role={}",
                request.getEmail(), principal.getUser().getRole());

        return ResponseEntity.ok(new AuthResponse(token, expiresInSeconds));
    }
}