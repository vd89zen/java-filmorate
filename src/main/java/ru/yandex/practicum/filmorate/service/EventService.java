package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.EventDbStorage;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;
import ru.yandex.practicum.filmorate.model.enums.OperationTypes;

import java.util.List;

@Slf4j
@Service
public class EventService {

    private final EventDbStorage eventsDbStorage;

    public EventService(EventDbStorage eventsDbStorage) {
        this.eventsDbStorage = eventsDbStorage;
    }

    @Transactional
    public List<Event> getFeedFriends(Long userId) {
        log.info("EventService: Получение ленты событий друзей пользователя ID {}", userId);
        return List.copyOf(eventsDbStorage.getFeedFriends(userId));
    }

    @Transactional
    public List<Event> getFeedUser(Long userId) {
        log.info("EventService: Получение ленты событий пользователя ID {}", userId);
        return List.copyOf(eventsDbStorage.getFeedUser(userId));
    }

    @Transactional
    public Event addEvent(Long userId, EventTypes event, OperationTypes operation, Long entityId) {
        log.info("EventService: Добавление события {}-{} в ленту пользователя ID {}", event, operation, userId);
        return eventsDbStorage.addEvent(userId, event, operation, entityId);
    }
}
