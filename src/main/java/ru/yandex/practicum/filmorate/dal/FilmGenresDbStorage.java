package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class FilmGenresDbStorage {
    private static final String DELETE_GENRES_OF_FILM_QUERY = "DELETE FROM film_genres WHERE film_id = ? AND genre_id = ?";
    private static final String DELETE_ALL_GENRES_OF_FILM_QUERY = "DELETE FROM film_genres WHERE film_id = ?";
    private static final String INSERT_QUERY = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
    private static final String GET_GENRES_IDS_OF_FILM_QUERY = "SELECT genre_id FROM film_genres WHERE film_id = ?";
    private static final String GET_GENRES_OF_ONE_FILM_QUERY = """
            SELECT fg.genre_id, g.name AS genre_name
            FROM film_genres fg
            LEFT JOIN genres g ON g.id = fg.genre_id
            WHERE film_id = ?
            """;
    private static final String GET_GENRES_OF_FILMS_QUERY = """
            SELECT fg.film_id, fg.genre_id, g.name AS genre_name
            FROM film_genres fg
            LEFT JOIN genres g ON g.id = fg.genre_id
            WHERE fg.film_id IN (:filmsIds)
            """;

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public FilmGenresDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public void insert(Long filmId, Set<Long> genresIds) {
        jdbc.batchUpdate(
                INSERT_QUERY,
                genresIds,
                genresIds.size(),
                (ps, genreId) -> {
                    ps.setLong(1, filmId);
                    ps.setLong(2, genreId);
                }
        );
    }

    public List<Genre> getGenresOfFilm(Long filmId) {
        return jdbc.query(
                        GET_GENRES_OF_ONE_FILM_QUERY,
                        (PreparedStatement ps) -> ps.setLong(1, filmId),
                        (rs, rowNum) -> {
                            Genre genre = new Genre();
                            genre.setId(rs.getLong("genre_id"));
                            genre.setName(rs.getString("genre_name"));
                            return genre;
                        });
    }

    public Set<Long> getGenreIdsOfFilm(Long filmId) {
        return jdbc.query(
                        GET_GENRES_IDS_OF_FILM_QUERY,
                        (PreparedStatement ps) -> ps.setLong(1, filmId),
                        (rs, rowNum) -> rs.getLong("genre_id")
                ).stream()
                .collect(Collectors.toSet());
    }

    public Map<Long, List<Genre>> getGenresByFilmsIds(Set<Long> filmsIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("filmsIds", filmsIds);
        return namedJdbc.query(GET_GENRES_OF_FILMS_QUERY, params, (rs, rowNum) -> {
                    Long filmId = rs.getLong("film_id");
                    Genre genre = new Genre();
                    genre.setId(rs.getLong("genre_id"));
                    genre.setName(rs.getString("genre_name"));
                    return Map.entry(filmId, genre);
                })
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    public void deleteGenresFromFilm(Long filmId, Set<Long> genresIds) {
        jdbc.batchUpdate(
                DELETE_GENRES_OF_FILM_QUERY,
                genresIds,
                genresIds.size(),
                (ps, genreId) -> {
                    ps.setLong(1, filmId);
                    ps.setLong(2, genreId);
                }
        );
    }

    public void deleteAllGenresFromFilm(Long filmId) {
        jdbc.update(DELETE_ALL_GENRES_OF_FILM_QUERY, filmId);
    }
}