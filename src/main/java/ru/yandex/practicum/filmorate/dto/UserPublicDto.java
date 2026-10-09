package ru.yandex.practicum.filmorate.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Builder
@Data
@EqualsAndHashCode(of = {"id"})
public class UserPublicDto {
    private Long id;
    private String login;
    private String name;
    private LocalDateTime createdAt;
}