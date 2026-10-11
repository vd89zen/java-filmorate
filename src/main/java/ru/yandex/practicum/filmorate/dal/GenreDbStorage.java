package ru.yandex.practicum.filmorate.dal;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class GenreDbStorage extends BaseDbStorage<Genre> {
    private static final String FIND_ALL_GENRES_QUERY = "SELECT id, name FROM genres ORDER BY id";
    private static final String FIND_GENRE_BY_ID_QUERY = "SELECT id, name FROM genres WHERE id = ?";
    private static final String FIND_GENRES_BY_IDS_QUERY = """
        SELECT id, name FROM genres WHERE id IN (:genresIds) ORDER BY id""";
    private static final String INSERT_GENRE_QUERY = "INSERT INTO genres(name) VALUES (?)";
    private static final String UPDATE_GENRE_QUERY = "UPDATE genres SET name = ? WHERE id = ?";
    private static final String DELETE_GENRE_QUERY = "DELETE FROM genres WHERE id = ?";
    private static final String IS_GENRE_EXISTS_QUERY = "SELECT EXISTS(SELECT 1 FROM genres WHERE id = ?)";
    private static final String IS_NAME_ALREADY_USE_QUERY = "SELECT EXISTS(SELECT 1 FROM genres WHERE name = ?)";
    private static final String IS_GENRE_IN_USE_QUERY =
            "SELECT EXISTS(SELECT 1 FROM film_genres WHERE genre_id = ?)";

    private final NamedParameterJdbcTemplate namedJdbc;

    public GenreDbStorage(JdbcOperations jdbc, RowMapper<Genre> mapper) {
        super(jdbc, mapper);
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    @Cacheable(value = "genres", unless = "#result.isEmpty()")
    public List<Genre> findAll() {
        return findMany(FIND_ALL_GENRES_QUERY);
    }

    public Optional<Genre> findById(Long id) {
        return findOne(FIND_GENRE_BY_ID_QUERY, id);
    }

    public List<Genre> findByIds(Set<Long> genresIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("genresIds", genresIds);
        return namedJdbc.query(FIND_GENRES_BY_IDS_QUERY, params, mapper);
    }

    @CacheEvict(value = "genres", allEntries = true)
    public Genre create(Genre newGenre) {
        Long returnedId = insert(INSERT_GENRE_QUERY, newGenre.getName());
        newGenre.setId(returnedId);
        return newGenre;
    }

    @CacheEvict(value = "genres", allEntries = true)
    public void update(Genre updatingGenre) {
        update(UPDATE_GENRE_QUERY, updatingGenre.getName(), updatingGenre.getId());
    }

    @CacheEvict(value = "genres", allEntries = true)
    public boolean delete(Long genreId) {
        return delete(DELETE_GENRE_QUERY, genreId);
    }

    public boolean isGenreExists(Long genreId) {
        Boolean exists = jdbc.queryForObject(IS_GENRE_EXISTS_QUERY, Boolean.class, genreId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean isNameAlreadyUse(String name) {
        Boolean exists = jdbc.queryForObject(IS_NAME_ALREADY_USE_QUERY, Boolean.class, name);
        return Boolean.TRUE.equals(exists);
    }

    public boolean isGenreInUse(Long genreId) {
        Boolean exists = jdbc.queryForObject(IS_GENRE_IN_USE_QUERY, Boolean.class, genreId);
        return Boolean.TRUE.equals(exists);
    }
}

