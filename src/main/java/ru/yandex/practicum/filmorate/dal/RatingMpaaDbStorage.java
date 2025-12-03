package ru.yandex.practicum.filmorate.dal;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.RatingMpaa;
import java.util.List;
import java.util.Optional;

@Repository
public class RatingMpaaDbStorage extends BaseDbStorage<RatingMpaa> {
    private static final String FIND_ALL_RATING_QUERY = "SELECT id, name FROM rating_mpaa ORDER BY id";
    private static final String FIND_RATING_BY_ID_QUERY = "SELECT id, name FROM rating_mpaa WHERE id = ?";

    public RatingMpaaDbStorage(JdbcTemplate jdbc, RowMapper<RatingMpaa> mapper) {
        super(jdbc, mapper);
    }

    @Cacheable(value = "ratings", unless = "#result.isEmpty()")
    public List<RatingMpaa> findAll() {
        return findMany(FIND_ALL_RATING_QUERY);
    }

    public Optional<RatingMpaa> findById(Long id) {
        return findOne(FIND_RATING_BY_ID_QUERY, id);
    }

    @CacheEvict(value = "ratings", allEntries = true)
    public void evictCache() {
        // Кэш очищен
    }
}

