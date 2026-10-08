package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class FilmLikesDbStorage {
    private static final String DELETE_LIKE_OF_FILM_QUERY = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
    private static final String DELETE_ALL_LIKES_OF_FILM_QUERY = "DELETE FROM film_likes WHERE film_id = ?";
    private static final String FIND_FILMS_IDS_LIKED_USER_QUERY = """
            SELECT film_id
            FROM film_likes
            WHERE user_id = ?
            """;
    private static final String GET_LIKES_COUNT_OF_ONE_FILM_QUERY = """
            SELECT COUNT(user_id)
            FROM film_likes
            WHERE film_id = ?
            """;
    private static final String CHECK_USER_ALREADY_LIKED = """
            SELECT EXISTS(
            SELECT 1 FROM film_likes
            WHERE film_id = ? AND user_id = ?)
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
    private static final String GET_TOP_POPULAR_FILMS_IDS_FILTERED_QUERY = """
        SELECT fl.film_id, COUNT(fl.user_id) AS likes_count
        FROM film_likes fl
        JOIN films f ON f.id = fl.film_id
        WHERE (CAST(:genreId AS BIGINT) IS NULL OR EXISTS (
            SELECT 1 FROM film_genres fg
            WHERE fg.film_id = f.id AND fg.genre_id = :genreId
        ))
        AND (CAST(:year AS INTEGER) IS NULL OR EXTRACT(YEAR FROM f.release_date) = :year)
        GROUP BY fl.film_id
        ORDER BY likes_count DESC, fl.film_id ASC
        LIMIT :limit
        """;
    private static final String GET_RECOMMENDATION_FILM_IDS_QUERY = """
        WITH user_similarity AS (
            SELECT fl2.user_id AS similar_user_id, COUNT(*) AS common_likes
            FROM film_likes fl1
            JOIN film_likes fl2 ON fl1.film_id = fl2.film_id
            WHERE fl1.user_id = :userId AND fl2.user_id != :userId
            GROUP BY fl2.user_id
        )
        SELECT fl.film_id
        FROM film_likes fl
        JOIN user_similarity us ON fl.user_id = us.similar_user_id
        WHERE fl.film_id NOT IN (SELECT film_id FROM film_likes WHERE user_id = :userId)
        GROUP BY fl.film_id
        ORDER BY SUM(us.common_likes) DESC
        """;

    private final JdbcOperations jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public FilmLikesDbStorage(JdbcOperations jdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public boolean hasUserLikedFilm(Long filmId, Long userId) {
        Boolean exists = jdbc.queryForObject(CHECK_USER_ALREADY_LIKED, Boolean.class, filmId, userId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean addLikeIfNotExists(Long filmId, Long userId) {
        return jdbc.update(ADD_LIKE_IF_NOT_EXISTS_QUERY, filmId, userId, filmId, userId) > 0;
    }

    public int getLikesCountOfFilm(Long filmId) {
        Integer count = jdbc.queryForObject(GET_LIKES_COUNT_OF_ONE_FILM_QUERY, Integer.class, filmId);
        return count == null ? 0 : count;
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
        List<Long> list = jdbc.queryForList(FIND_FILMS_IDS_LIKED_USER_QUERY, Long.class, userId);
        return new HashSet<>(list);
    }

    public LinkedHashMap<Long, Integer> getTopPopularFilmsIds(int count, Long genreId, Integer year) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", count)
                .addValue("genreId", genreId)
                .addValue("year", year);

        return namedJdbc.query(
                        GET_TOP_POPULAR_FILMS_IDS_FILTERED_QUERY, params,
                        (rs, rowNum) -> Map.entry(
                                rs.getLong("film_id"),
                                rs.getInt("likes_count")))
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    public boolean deleteLikeFromFilmIfExists(Long filmId, Long userId) {
        return jdbc.update(DELETE_LIKE_OF_FILM_QUERY, filmId, userId) > 0;
    }

    public boolean deleteAllLikesFromFilmIfExists(Long filmId) {
        return jdbc.update(DELETE_ALL_LIKES_OF_FILM_QUERY, filmId) > 0;
    }

    public List<Long> getRecommendationFilmIds(Long userId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return namedJdbc.queryForList(GET_RECOMMENDATION_FILM_IDS_QUERY, params, Long.class);
    }
}
