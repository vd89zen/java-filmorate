package ru.yandex.practicum.filmorate.dal;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.RatingRars;
import java.util.List;
import java.util.Optional;

@Repository
public class RatingRarsDbStorage extends BaseDbStorage<RatingRars> {
    private static final String FIND_ALL_RATING_QUERY = "SELECT id, name FROM rating_rars ORDER BY id";
    private static final String FIND_RATING_BY_ID_QUERY = "SELECT id, name FROM rating_rars WHERE id = ?";

    public RatingRarsDbStorage(JdbcOperations jdbc, RowMapper<RatingRars> mapper) {
        super(jdbc, mapper);
    }

    @Cacheable(value = "rars", unless = "#result.isEmpty()")
    public List<RatingRars> findAll() {
        return findMany(FIND_ALL_RATING_QUERY);
    }

    public Optional<RatingRars> findById(Long id) {
        return findOne(FIND_RATING_BY_ID_QUERY, id);
    }
}