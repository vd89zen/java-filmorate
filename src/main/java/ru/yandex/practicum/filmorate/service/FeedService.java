package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dto.*;
import ru.yandex.practicum.filmorate.mapper.EventMapper;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {
    private final UserService userService;
    private final FilmService filmService;
    private final ReviewService reviewService;
    private final EventService eventService;

    @Transactional(readOnly = true)
    public List<EventDto> getFeedFriends(Long userId) {
        log.info("Получение событий друзей пользователя ID {}.", userId);
        userService.checkUserExists(userId);
        return EventMapper.toDtoList(eventService.getFeedFriends(userId));
    }

    @Transactional(readOnly = true)
    public List<EventDto> getFeedUser(Long userId) {
        log.info("Получение событий пользователя ID {}.", userId);
        userService.checkUserExists(userId);
        return EventMapper.toDtoList(eventService.getFeedUser(userId));
    }

    @Transactional(readOnly = true)
    public List<EnrichedEventDto> getEnrichedFeedUser(Long userId) {
        log.info("FeedService: обогащённая лента пользователя ID {}", userId);
        userService.checkUserExists(userId);
        return enrich(eventService.getFeedUser(userId));
    }

    @Transactional(readOnly = true)
    public List<EnrichedEventDto> getEnrichedFeedFriends(Long userId) {
        log.info("FeedService: обогащённая лента друзей пользователя ID {}", userId);
        userService.checkUserExists(userId);
        return enrich(eventService.getFeedFriends(userId));
    }

    private List<EnrichedEventDto> enrich(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }

        Set<Long> filmIds = events.stream()
                .filter(e -> EventTypes.LIKE.name().equals(e.getEventType()))
                .map(Event::getEntityId)
                .collect(Collectors.toSet());

        Set<Long> userIds = events.stream()
                .filter(e -> EventTypes.FRIEND.name().equals(e.getEventType()))
                .map(Event::getEntityId)
                .collect(Collectors.toSet());

        Set<Long> reviewIds = events.stream()
                .filter(e -> EventTypes.REVIEW.name().equals(e.getEventType()))
                .map(Event::getEntityId)
                .collect(Collectors.toSet());

        Map<Long, FilmShortDto> films = filmService.findShortByIds(filmIds);
        Map<Long, UserShortDto> users = userService.findShortByIds(userIds);
        Map<Long, ReviewShortDto> reviews = reviewService.findShortByIds(reviewIds);

        return events.stream()
                .map(e -> EnrichedEventDto.builder()
                        .eventId(e.getEventId())
                        .timestamp(e.getTimestamp())
                        .userId(e.getUserId())
                        .eventType(e.getEventType())
                        .operation(e.getOperation())
                        .film(EventTypes.LIKE.name().equals(e.getEventType())
                                ? films.get(e.getEntityId())
                                : null)
                        .user(EventTypes.FRIEND.name().equals(e.getEventType())
                                ? users.get(e.getEntityId())
                                : null)
                        .review(EventTypes.REVIEW.name().equals(e.getEventType())
                                ? reviews.get(e.getEntityId())
                                : null)
                        .build())
                .collect(Collectors.toList());
    }
}