package ru.yandex.practicum.filmorate.dal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.enums.EventTypes;
import ru.yandex.practicum.filmorate.model.enums.OperationTypes;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

@Slf4j
@Repository
public class EventDbStorage {
    private final JdbcOperations jdbc;
    private final RowMapper<Event> mapper;

    @Autowired
    public EventDbStorage(final JdbcOperations jdbc, final RowMapper<Event> mapper) {
            this.jdbc = jdbc;
            this.mapper = mapper;
        }

    public List<Event> getFeed(Long userId) {
        final String GET_USER_FEED_QUERY = """
                SELECT e.id, e.time_stamp, e.user_id, e.entity_id, et.name AS event_type, ot.name AS operation_type
                FROM events AS e
                LEFT OUTER JOIN event_types AS et ON et.id = e.event_type_id
                LEFT OUTER JOIN operation_types AS ot ON ot.id = e.operation_type_id
                WHERE e.user_id = ?
                ORDER BY e.id
                """;

        return jdbc.query(GET_USER_FEED_QUERY, mapper, userId);
    }

    public Event addEvent(Long userId, EventTypes event, OperationTypes operation, Long entityId) {
        final String ADD_EVENT_QUERY = """
                INSERT INTO events (time_stamp, user_id, event_type_id, operation_type_id, entity_id)
                VALUES (?, ?, ?, ?, ?);
                """;
        final long timestamp = Instant.now().toEpochMilli();

        final Object[] params = {
                timestamp,
                userId,
                event.getId(),
                operation.getId(),
                entityId
        };

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement(ADD_EVENT_QUERY, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps;
        }, keyHolder);

        Long generatedId = keyHolder.getKeyAs(Long.class);
        if (generatedId == null) {
            throw new RuntimeException("Не удалось сохранить данные события/Event");
        }

        return Event.builder()
                .eventId(generatedId)
                .timestamp(timestamp)
                .userId(userId)
                .eventType(event.name())
                .operation(operation.name())
                .entityId(entityId)
                .build();
    }
}
