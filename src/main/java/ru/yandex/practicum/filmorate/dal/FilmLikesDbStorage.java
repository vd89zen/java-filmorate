package ru.yandex.practicum.filmorate.dal;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class FilmLikesDbStorage {
    private static final String DELETE_LIKE_OF_FILM_QUERY = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
    private static final String DELETE_ALL_LIKES_OF_FILM_QUERY = "DELETE FROM film_likes WHERE film_id = ?";
    private static final String FIND_FILMS_IDS_LIKED_USER_QUERY = """
            SELECT film_id
            FROM film_likes
            WHERE user_id = :userId
            """;
    private static final String GET_LIKES_COUNT_OF_ONE_FILM_QUERY = """
            SELECT COUNT(user_id)
            FROM film_likes
            WHERE film_id = :filmId
            """;
    private static final String CHECK_USER_ALREADY_LIKED = """
            SELECT COUNT(*)
            FROM film_likes
            WHERE film_id = ? AND user_id = ?
            """;
    private static final String GET_LIKES_COUNT_OF_FILMS_QUERY = """
            SELECT film_id, COUNT(user_id) AS likes_count
            FROM film_likes
            WHERE film_id IN (:filmsIds)
            GROUP BY film_id
            """;
    private static final String ADD_LIKE_IF_NOT_EXISTS_QUERY = """
            INSERT INTO film_likes (film_id, user_id)
            SELECT ?, ?
            WHERE NOT EXISTS (SELECT 1 FROM film_likes WHERE film_id = ? AND user_id = ?)
            """;
    private static final String GET_TOP_POPULAR_FILMS_IDS_QUERY = """
            SELECT film_id, COUNT(user_id) AS likes_count
            FROM film_likes
            GROUP BY film_id
            HAVING COUNT(user_id) > 0
            ORDER BY likes_count DESC
            LIMIT :limit
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public FilmLikesDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public boolean hasUserLikedFilm(Long filmId, Long userId) {
        return jdbc.queryForObject(CHECK_USER_ALREADY_LIKED, Integer.class, filmId, userId) > 0;
    }

    public boolean addLikeIfNotExists(Long filmId, Long userId) {
        return jdbc.update(ADD_LIKE_IF_NOT_EXISTS_QUERY, filmId, userId, filmId, userId) > 0;
    }

    public int getLikesCountOfFilm(Long filmId) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource("filmId", filmId);
            return namedJdbc.queryForObject(GET_LIKES_COUNT_OF_ONE_FILM_QUERY, params, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    public Map<Long, Integer> getLikesCountByFilmsIds(Set<Long> filmsIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("filmsIds", filmsIds);
        Map<Long, Integer> result = namedJdbc.query(GET_LIKES_COUNT_OF_FILMS_QUERY, params,
                        (rs, rowNum) -> Map.entry(
                                rs.getLong("film_id"),
                                rs.getInt("likes_count")
                        ))
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
        filmsIds.forEach(filmId -> result.putIfAbsent(filmId, 0));
        return result;
    }

    public Set<Long> getFilmsIdsLikedByUser(Long userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return namedJdbc.query(
                FIND_FILMS_IDS_LIKED_USER_QUERY, params,
                (ResultSet rs) -> {
                    Set<Long> set = new HashSet<>();
                    while (rs.next()) {
                        set.add(rs.getLong("film_id"));
                    }
                    return set;
                }
        );
    }

    public LinkedHashMap<Long, Integer> getTopPopularFilmsIds(int count) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", count);
        return namedJdbc.query(
                        GET_TOP_POPULAR_FILMS_IDS_QUERY, params,
                        (rs, rowNum) -> Map.entry(
                                rs.getLong("film_id"),
                                rs.getInt("likes_count")
                        ))
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    public boolean deleteLikeFromFilmIfExists(Long filmId, Long userId) {
        return jdbc.update(DELETE_LIKE_OF_FILM_QUERY, filmId, userId) > 0;
    }

    public boolean deleteAllLikesFromFilmIfExists(Long filmId) {
        return jdbc.update(DELETE_ALL_LIKES_OF_FILM_QUERY, filmId) > 0;
    }
}
