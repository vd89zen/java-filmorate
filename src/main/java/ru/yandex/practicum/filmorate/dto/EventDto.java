package ru.yandex.practicum.filmorate.dto;

import lombok.*;

@Builder
@Data
@EqualsAndHashCode(of = {"id"})
public class EventDto {
    private Long eventId;
    private Long timestamp;
    private Long userId;
    private String eventType;
    private String operation;
    private Long entityId;
}
