package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class DirectorDbStorage extends BaseDbStorage<Director> {
    private final NamedParameterJdbcTemplate namedJdbc;
    private static final String DELETE_DIRECTOR_QUERY = "DELETE FROM directors WHERE id = ?";
    private static final String IS_DIRECTOR_EXISTS_QUERY =
            "SELECT EXISTS(SELECT 1 FROM directors WHERE id = ?)";
    private static final String IS_NAME_ALREADY_USE_QUERY =
            "SELECT EXISTS(SELECT 1 FROM directors WHERE name = ?)";
    private static final String FIND_ALL_DIRECTORS_QUERY =
            "SELECT id, name FROM directors ORDER BY id";
    private static final String FIND_DIRECTOR_BY_ID_QUERY =
            "SELECT id, name FROM directors WHERE id = ?";
    private static final String INSERT_DIRECTOR_QUERY =
            "INSERT INTO directors(name) VALUES (?)";
    private static final String UPDATE_DIRECTOR_QUERY =
            "UPDATE directors SET name = ? WHERE id = ?";
    private static final String FIND_DIRECTORS_BY_IDS_QUERY = """
        SELECT id, name FROM directors WHERE id IN (:directorIds) ORDER BY id
        """;

    public DirectorDbStorage(JdbcOperations jdbc, RowMapper<Director> mapper) {
        super(jdbc, mapper);
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public Director create(Director director) {
        Long id = insert(INSERT_DIRECTOR_QUERY, director.getName());
        director.setId(id);
        return director;
    }

    public void update(Director director) {
        update(UPDATE_DIRECTOR_QUERY, director.getName(), director.getId());
    }

    public boolean delete(Long directorId) {
        return delete(DELETE_DIRECTOR_QUERY, directorId);
    }

    public Optional<Director> findById(Long directorId) {
        return findOne(FIND_DIRECTOR_BY_ID_QUERY, directorId);
    }

    public List<Director> findAll() {
        return findMany(FIND_ALL_DIRECTORS_QUERY);
    }

    public List<Director> findByIds(Set<Long> directorIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("directorIds", directorIds);
        return namedJdbc.query(FIND_DIRECTORS_BY_IDS_QUERY, params, mapper);
    }

    public boolean isDirectorExists(Long directorId) {
        Boolean exists = jdbc.queryForObject(IS_DIRECTOR_EXISTS_QUERY, Boolean.class, directorId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean isNameAlreadyUse(String name) {
        Boolean exists = jdbc.queryForObject(IS_NAME_ALREADY_USE_QUERY, Boolean.class, name);
        return Boolean.TRUE.equals(exists);
    }
}