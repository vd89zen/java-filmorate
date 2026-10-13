package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.dto.SearchRequest;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

@Repository
public class FilmDbStorage extends BaseDbStorage<Film> {
    private static final String DELETE_FILM_QUERY = "DELETE FROM films WHERE id = ?";
    private static final String IS_FILM_EXISTS_QUERY = "SELECT EXISTS(SELECT 1 FROM films WHERE id = ? LIMIT 1)";
    private static final String FIND_ALL_FILMS_PAGINATED_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id, f.rating_rars_id
        FROM films f
        ORDER BY f.id
        LIMIT ? OFFSET ?
        """;
    private static final String FIND_FILM_BY_ID_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id, f.rating_rars_id
        FROM films f
        WHERE f.id = ?
        """;
    private static final String FIND_FILMS_BY_IDS_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id, f.rating_rars_id
        FROM films f
        WHERE f.id IN (:filmsIds)
        """;
    private static final String INSERT_FILM_QUERY = """
        INSERT INTO films(name, description, release_date, duration, rating_mpaa_id, rating_rars_id)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    private static final String UPDATE_FILM_QUERY = """
        UPDATE films
        SET name = ?, description = ?, release_date = ?, duration = ?, rating_mpaa_id = ?, rating_rars_id = ?
        WHERE id = ?
        """;
    private static final String FIND_FILMS_BY_DIRECTOR_SORTED_BY_YEAR_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id, f.rating_rars_id
        FROM films f
        JOIN film_directors fd ON fd.film_id = f.id
        WHERE fd.director_id = ?
        ORDER BY f.release_date ASC, f.id ASC
        """;
    private static final String FIND_FILMS_BY_DIRECTOR_SORTED_BY_LIKES_QUERY = """
        SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id, f.rating_rars_id
        FROM films f
        JOIN film_directors fd ON fd.film_id = f.id
        LEFT JOIN film_likes fl ON fl.film_id = f.id
        WHERE fd.director_id = ?
        GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id, f.rating_rars_id
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
                newFilm.getMpa() == null ? null : newFilm.getMpa().getId(),
                newFilm.getRars() == null ? null : newFilm.getRars().getId());

        newFilm.setId(returnedId);
        return newFilm;
    }

    public void update(Film updatingFilm) {
        update(UPDATE_FILM_QUERY,
                updatingFilm.getName(),
                updatingFilm.getDescription(),
                updatingFilm.getReleaseDate(),
                updatingFilm.getDuration(),
                updatingFilm.getMpa() == null ? null : updatingFilm.getMpa().getId(),
                updatingFilm.getRars() == null ? null : updatingFilm.getRars().getId(),
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

    public List<Film> search(SearchRequest request) {
        StringBuilder sql = new StringBuilder("""
            SELECT f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id, f.rating_rars_id
            FROM films f
            LEFT JOIN film_likes fl ON fl.film_id = f.id
            """);

        List<String> conditions = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        appendTextCondition(request, conditions, params);
        appendYearCondition(request, conditions, params);
        appendDurationCondition(request, conditions, params);
        appendMpaCondition(request, conditions, params);
        appendRarsCondition(request, conditions, params);

        if (conditions.isEmpty() == false) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        sql.append(" GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.rating_mpaa_id, f.rating_rars_id");
        sql.append(" ORDER BY COUNT(fl.user_id) DESC, f.id ASC");
        sql.append(" LIMIT :limit OFFSET :offset");

        params.addValue("limit", request.getSize());
        params.addValue("offset", request.getFrom());

        return namedJdbc.query(sql.toString(), params, mapper);
    }

    private void appendTextCondition(SearchRequest request, List<String> conditions,
                                     MapSqlParameterSource params) {
        String query = request.getQuery();
        if (query == null || query.isBlank()) {
            return;
        }
        String pattern = "%" + escapeLike(query.toLowerCase(Locale.ROOT)) + "%";
        params.addValue("queryPattern", pattern);

        List<String> orParts = new ArrayList<>();
        Set<String> by = request.getBy() == null ? Set.of() : request.getBy();
        if (by.contains("title")) {
            orParts.add("LOWER(f.name) LIKE :queryPattern ESCAPE '!'");
        }
        if (by.contains("description")) {
            orParts.add("LOWER(f.description) LIKE :queryPattern ESCAPE '!'");
        }
        if (by.contains("director")) {
            orParts.add("""
                EXISTS (
                    SELECT 1 FROM film_directors fd
                    JOIN directors d ON d.id = fd.director_id
                    WHERE fd.film_id = f.id
                      AND LOWER(d.name) LIKE :queryPattern ESCAPE '!'
                )
                """);
        }
        if (orParts.isEmpty() == false) {
            conditions.add("(" + String.join(" OR ", orParts) + ")");
        }
    }

    private void appendYearCondition(SearchRequest request, List<String> conditions,
                                     MapSqlParameterSource params) {
        if (request.getYear() != null) {
            conditions.add("EXTRACT(YEAR FROM f.release_date) = :year");
            params.addValue("year", request.getYear());
            return;
        }
        if (request.getYearFrom() != null) {
            conditions.add("EXTRACT(YEAR FROM f.release_date) >= :yearFrom");
            params.addValue("yearFrom", request.getYearFrom());
        }
        if (request.getYearTo() != null) {
            conditions.add("EXTRACT(YEAR FROM f.release_date) <= :yearTo");
            params.addValue("yearTo", request.getYearTo());
        }
    }

    private void appendDurationCondition(SearchRequest request, List<String> conditions,
                                         MapSqlParameterSource params) {
        if (request.getDuration() != null) {
            conditions.add("f.duration = :duration");
            params.addValue("duration", request.getDuration());
            return;
        }
        if (request.getDurationFrom() != null) {
            conditions.add("f.duration >= :durationFrom");
            params.addValue("durationFrom", request.getDurationFrom());
        }
        if (request.getDurationTo() != null) {
            conditions.add("f.duration <= :durationTo");
            params.addValue("durationTo", request.getDurationTo());
        }
    }

    private void appendMpaCondition(SearchRequest request, List<String> conditions,
                                    MapSqlParameterSource params) {
        Set<Long> mpaIds = request.getMpaIds();
        if (mpaIds != null && mpaIds.isEmpty() == false) {
            conditions.add("f.rating_mpaa_id IN (:mpaIds)");
            params.addValue("mpaIds", mpaIds);
        }
    }

    private void appendRarsCondition(SearchRequest request, List<String> conditions,
                                     MapSqlParameterSource params) {
        Set<Long> rarsIds = request.getRarsIds();
        if (rarsIds != null && rarsIds.isEmpty() == false) {
            conditions.add("f.rating_rars_id IN (:rarsIds)");
            params.addValue("rarsIds", rarsIds);
        }
    }

    private static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}