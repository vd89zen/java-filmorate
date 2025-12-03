package ru.yandex.practicum.filmorate.dal;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import java.util.List;
import java.util.Optional;

@Repository
public class GenreDbStorage extends BaseDbStorage<Genre> {
    private static final String FIND_ALL_GENRES_QUERY = "SELECT id, name FROM genres ORDER BY id";
    private static final String FIND_GENRE_BY_ID_QUERY = "SELECT id, name FROM genres WHERE id = ?";

    public GenreDbStorage(JdbcTemplate jdbc, RowMapper<Genre> mapper) {
        super(jdbc, mapper);
    }

    @Cacheable(value = "genres", unless = "#result.isEmpty()")
    public List<Genre> findAll() {
        return findMany(FIND_ALL_GENRES_QUERY);
    }

    public Optional<Genre> findById(Long id) {
        return findOne(FIND_GENRE_BY_ID_QUERY, id);
    }

    @CacheEvict(value = "genres", allEntries = true)
    public void evictCache() {
        // Кэш очищен
    }
}

