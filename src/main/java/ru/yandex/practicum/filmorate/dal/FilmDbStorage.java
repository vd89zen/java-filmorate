package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FilmStorage;
import java.util.List;
import java.util.Optional;

@Repository
public class FilmDbStorage extends BaseDbStorage<Film> implements FilmStorage {
    private static final String DELETE_FILM_QUERY = "DELETE FROM films WHERE id = ?";
    private static final String IS_FILM_EXISTS_QUERY = "SELECT EXISTS(SELECT 1 FROM films WHERE id = ? LIMIT 1)";
    private static final String FIND_ALL_FILMS_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id, rm.name AS mpa_name
        FROM films f
        LEFT JOIN rating_mpaa rm ON rm.id = f.rating_mpaa_id
        ORDER BY f.id
        """;
    private static final String FIND_FILM_BY_ID_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id, rm.name AS mpa_name
        FROM films f
        LEFT JOIN rating_mpaa rm ON rm.id = f.rating_mpaa_id
        WHERE f.id = ?
        """;
    private static final String FIND_FILMS_BY_IDS_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id, rm.name AS mpa_name
        FROM films f
        LEFT JOIN rating_mpaa rm ON rm.id = f.rating_mpaa_id
        WHERE f.id IN (:filmsIds)
        ORDER BY f.id
        """;
    private static final String INSERT_FILM_QUERY = """
        INSERT INTO films(name, description, release_date, duration, rating_mpaa_id)
        VALUES (?, ?, ?, ?, ?)
        """;
    private static final String UPDATE_FILM_QUERY = """
        UPDATE films
        SET name = ?, description = ?, release_date = ?, duration = ?, rating_mpaa_id = ?
        WHERE id = ?
        """;

    private final NamedParameterJdbcTemplate namedJdbc;

    public FilmDbStorage(JdbcTemplate jdbc, RowMapper<Film> mapper) {
        super(jdbc, mapper);
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    @Override
    public Film create(Film newFilm) {
        Long returnedId = insert(INSERT_FILM_QUERY,
                newFilm.getName(),
                newFilm.getDescription(),
                newFilm.getReleaseDate(),
                newFilm.getDuration(),
                newFilm.getRatingMpaa().getId());

        newFilm.setId(returnedId);
        return newFilm;
    }

    @Override
    public void update(Film updatingFilm) {
        update(UPDATE_FILM_QUERY,
                updatingFilm.getName(),
                updatingFilm.getDescription(),
                updatingFilm.getReleaseDate(),
                updatingFilm.getDuration(),
                updatingFilm.getRatingMpaa().getId(),
                updatingFilm.getId());
    }

    @Override
    public List<Film> findAll() {
        return findMany(FIND_ALL_FILMS_QUERY);
    }

    @Override
    public Optional<Film> findById(Long filmId) {
        return findOne(FIND_FILM_BY_ID_QUERY, filmId);
    }

    @Override
    public boolean delete(Long filmId) {
        return delete(DELETE_FILM_QUERY, filmId);
    }

    @Override
    public List<Film> findBySeveralIds(List<Long> filmsIds) {
        MapSqlParameterSource param = new MapSqlParameterSource("filmsIds", filmsIds);
        return namedJdbc.query(FIND_FILMS_BY_IDS_QUERY, param, mapper);
    }

    @Override
    public boolean isFilmExists(Long filmId) {
        return jdbc.queryForObject(IS_FILM_EXISTS_QUERY, Boolean.class, filmId);
    }
}
