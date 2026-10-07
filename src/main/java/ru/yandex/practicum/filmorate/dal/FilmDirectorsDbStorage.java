package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class FilmDirectorsDbStorage {
    private static final String INSERT_QUERY =
            "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
    private static final String DELETE_ALL_OF_FILM_QUERY =
            "DELETE FROM film_directors WHERE film_id = ?";
    private static final String GET_DIRECTORS_IDS_OF_FILM_QUERY =
            "SELECT director_id FROM film_directors WHERE film_id = ?";
    private static final String GET_DIRECTORS_OF_ONE_FILM_QUERY = """
            SELECT fd.director_id, d.name AS director_name
            FROM film_directors fd
            LEFT JOIN directors d ON d.id = fd.director_id
            WHERE fd.film_id = ?
            ORDER BY fd.director_id
            """;
    private static final String GET_DIRECTORS_OF_FILMS_QUERY = """
            SELECT fd.film_id, fd.director_id, d.name AS director_name
            FROM film_directors fd
            LEFT JOIN directors d ON d.id = fd.director_id
            WHERE fd.film_id IN (:filmIds)
            """;

    private final JdbcOperations jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public FilmDirectorsDbStorage(JdbcOperations jdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public void insert(Long filmId, Set<Long> directorsIds) {
        if (directorsIds == null) {
            throw new IllegalArgumentException("directorsIds must not be null");
        }
        if (directorsIds.isEmpty()) {
            return;
        }

        jdbc.batchUpdate(
                INSERT_QUERY,
                directorsIds,
                directorsIds.size(),
                (ps, directorId) -> {
                    ps.setLong(1, filmId);
                    ps.setLong(2, directorId);
                }
        );
    }

    public List<Director> getDirectorsOfFilm(Long filmId) {
        return jdbc.query(
                GET_DIRECTORS_OF_ONE_FILM_QUERY,
                (PreparedStatement ps) -> ps.setLong(1, filmId),
                (rs, rowNum) -> Director.builder()
                        .id(rs.getLong("director_id"))
                        .name(rs.getString("director_name"))
                        .build());
    }

    public Set<Long> getDirectorIdsOfFilm(Long filmId) {
        return jdbc.query(
                        GET_DIRECTORS_IDS_OF_FILM_QUERY,
                        (PreparedStatement ps) -> ps.setLong(1, filmId),
                        (rs, rowNum) -> rs.getLong("director_id"))
                .stream()
                .collect(Collectors.toSet());
    }

    public Map<Long, List<Director>> getDirectorsByFilmsIds(Set<Long> filmIds) {
        if (filmIds == null || filmIds.isEmpty()) {
            return Map.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource("filmIds", filmIds);
        return namedJdbc.query(GET_DIRECTORS_OF_FILMS_QUERY, params, (rs, rowNum) -> {
                    Long filmId = rs.getLong("film_id");
                    Director director = Director.builder()
                            .id(rs.getLong("director_id"))
                            .name(rs.getString("director_name"))
                            .build();
                    return Map.entry(filmId, director);
                })
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    public void deleteAllDirectorsFromFilm(Long filmId) {
        jdbc.update(DELETE_ALL_OF_FILM_QUERY, filmId);
    }
}