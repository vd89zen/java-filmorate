package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import java.util.List;
import java.util.Optional;

@Repository
public class FilmDbStorage extends BaseDbStorage<Film> {
    private static final String DELETE_FILM_QUERY = "DELETE FROM films WHERE id = ?";
    private static final String IS_FILM_EXISTS_QUERY = "SELECT EXISTS(SELECT 1 FROM films WHERE id = ? LIMIT 1)";
    private static final String FIND_ALL_FILMS_PAGINATED_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id
        FROM films f
        ORDER BY f.id
        LIMIT ? OFFSET ?
        """;
    private static final String FIND_FILM_BY_ID_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id
        FROM films f
        WHERE f.id = ?
        """;
    private static final String FIND_FILMS_BY_IDS_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id
        FROM films f
        WHERE f.id IN (:filmsIds)
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
    private static final String FIND_FILMS_BY_DIRECTOR_SORTED_BY_YEAR_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id
        FROM films f
        JOIN film_directors fd ON fd.film_id = f.id
        WHERE fd.director_id = ?
        ORDER BY f.release_date ASC, f.id ASC
        """;
    private static final String FIND_FILMS_BY_DIRECTOR_SORTED_BY_LIKES_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id
        FROM films f
        JOIN film_directors fd ON fd.film_id = f.id
        LEFT JOIN film_likes fl ON fl.film_id = f.id
        WHERE fd.director_id = ?
        GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id
        ORDER BY COUNT(fl.user_id) DESC, f.id ASC
        """;

    private final NamedParameterJdbcTemplate namedJdbc;

    public FilmDbStorage(JdbcOperations jdbc, RowMapper<Film> mapper) {
        super(jdbc, mapper);
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public Film create(Film newFilm) {
        Long returnedId = insert(INSERT_FILM_QUERY,
                newFilm.getName(),
                newFilm.getDescription(),
                newFilm.getReleaseDate(),
                newFilm.getDuration(),
                newFilm.getMpa().getId());

        newFilm.setId(returnedId);
        return newFilm;
    }

    public void update(Film updatingFilm) {
        update(UPDATE_FILM_QUERY,
                updatingFilm.getName(),
                updatingFilm.getDescription(),
                updatingFilm.getReleaseDate(),
                updatingFilm.getDuration(),
                updatingFilm.getMpa().getId(),
                updatingFilm.getId());
    }

    public List<Film> findAll(int from, int size) {
        return findMany(FIND_ALL_FILMS_PAGINATED_QUERY, size, from);
    }

    public Optional<Film> findById(Long filmId) {
        return findOne(FIND_FILM_BY_ID_QUERY, filmId);
    }

    public boolean delete(Long filmId) {
        return delete(DELETE_FILM_QUERY, filmId);
    }

    /**
     * Возвращает фильмы по набору id.
     * <p>Порядок результата НЕ гарантирован. Если порядок важен,
     * восстанавливайте его самостоятельно по исходному списку id.
     */
    public List<Film> findBySeveralIds(List<Long> filmsIds) {
        MapSqlParameterSource param = new MapSqlParameterSource("filmsIds", filmsIds);
        return namedJdbc.query(FIND_FILMS_BY_IDS_QUERY, param, mapper);
    }

    public boolean isFilmExists(Long filmId) {
        Boolean exists = jdbc.queryForObject(IS_FILM_EXISTS_QUERY, Boolean.class, filmId);
        return Boolean.TRUE.equals(exists);
    }

    public List<Film> findByDirectorSortedByYear(Long directorId) {
        return findMany(FIND_FILMS_BY_DIRECTOR_SORTED_BY_YEAR_QUERY, directorId);
    }

    public List<Film> findByDirectorSortedByLikes(Long directorId) {
        return findMany(FIND_FILMS_BY_DIRECTOR_SORTED_BY_LIKES_QUERY, directorId);
    }
}