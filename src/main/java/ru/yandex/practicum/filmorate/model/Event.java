package ru.yandex.practicum.filmorate.model;

import lombok.*;

@Data
@EqualsAndHashCode(of = {"eventId"})
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Event {
    private Long eventId;
    private Long timestamp;
    private Long userId;
    private String eventType;
    private String operation;
    private Long entityId;
}
