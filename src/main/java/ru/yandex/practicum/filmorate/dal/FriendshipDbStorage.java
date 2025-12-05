package ru.yandex.practicum.filmorate.dal;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class FriendshipDbStorage {
    private static final String ADD_FRIEND_REQUEST = """
        INSERT INTO friendship (user_id, friend_id)
        VALUES (?, ?)
        """;
    private static final String GET_FRIENDS_IDS_OF_USER_QUERY = """
        SELECT friend_id
        FROM friendship
        WHERE user_id = ?
        """;
    private static final String REMOVE_FRIEND_QUERY = """
        DELETE
        FROM friendship
        WHERE user_id = ? AND friend_id = ?
        """;
    private static final String CHEK_IS_FRIEND_QUERY = """
        SELECT EXISTS(SELECT 1
        FROM friendship
        WHERE user_id = ? AND friend_id = ?)
        """;
    private static final String FIND_COMMON_FRIENDS_QUERY = """
        SELECT f1.friend_id
        FROM friendship f1
        JOIN friendship f2 ON f1.friend_id = f2.friend_id
        WHERE f1.user_id = ? AND f2.user_id = ?
        """;
    private static final String GET_FRIENDS_COUNT_OF_ONE_USER_QUERY = """
        SELECT COUNT(friend_id)
        FROM friendship
        WHERE user_id = :userId
        """;
    private static final String GET_FRIENDS_COUNT_OF_USERS_QUERY = """
        SELECT user_id, COUNT(friend_id) AS friends_count
        FROM friendship
        WHERE user_id IN (:usersIds)
        GROUP BY user_id
        """;

    private  final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public FriendshipDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public void addFriend(Long userId, Long friendId) {
        try {
            int rowsUpdated = jdbc.update(ADD_FRIEND_REQUEST, userId, friendId);
            if (rowsUpdated == 0) {
                throw new RuntimeException("Не удалось добавить запись в БД.");
            }
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("Не удалось добавить запись в БД: дубликат", e);
        }
    }

    public List<Long> getFriendsIdsOfUser(Long userId) {
        return jdbc.queryForList(GET_FRIENDS_IDS_OF_USER_QUERY, Long.class, userId);
    }

    public int getUserFriendsCount(Long userId) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
            return namedJdbc.queryForObject(GET_FRIENDS_COUNT_OF_ONE_USER_QUERY, params, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    public Map<Long, Integer> getFriendsCountByUsersIds(Set<Long> usersIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("usersIds", usersIds);
        Map<Long, Integer> result = namedJdbc.query(GET_FRIENDS_COUNT_OF_USERS_QUERY, params,
                        (rs, rowNum) -> Map.entry(
                                rs.getLong("user_id"),
                                rs.getInt("friends_count")
                        ))
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
        usersIds.forEach(userId -> result.putIfAbsent(userId, 0));
        return result;
    }

    public boolean isFriend(Long userId, Long friendId) {
        return jdbc.queryForObject(CHEK_IS_FRIEND_QUERY, Boolean.class, userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        int rowsDeleted = jdbc.update(REMOVE_FRIEND_QUERY, userId, friendId);
    }

    public List<Long> getCommonFriends(Long userId, Long otherUserId) {
        return jdbc.queryForList(FIND_COMMON_FRIENDS_QUERY, Long.class, userId, otherUserId);
    }
}