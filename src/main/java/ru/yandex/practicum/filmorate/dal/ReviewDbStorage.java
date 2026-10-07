package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;
import java.util.Optional;

@Repository
public class ReviewDbStorage extends BaseDbStorage<Review> {
    private final NamedParameterJdbcTemplate namedJdbc;
    private static final String DELETE_REVIEW_QUERY = "DELETE FROM reviews WHERE id = ?";
    private static final String INSERT_REVIEW_QUERY = """
        INSERT INTO reviews(content, is_positive, user_id, film_id)
        VALUES (?, ?, ?, ?)
        """;
    private static final String UPDATE_REVIEW_QUERY = """
        UPDATE reviews
        SET content = ?, is_positive = ?
        WHERE id = ?
        """;
    private static final String FIND_REVIEW_BY_ID_QUERY = """
        SELECT id, content, is_positive, user_id, film_id, useful
        FROM reviews
        WHERE id = ?
        """;
    private static final String FIND_REVIEWS_BY_FILM_QUERY = """
        SELECT id, content, is_positive, user_id, film_id, useful
        FROM reviews
        WHERE film_id = ?
        ORDER BY useful DESC, id ASC
        LIMIT ?
        """;
    private static final String FIND_ALL_REVIEWS_QUERY = """
        SELECT id, content, is_positive, user_id, film_id, useful
        FROM reviews
        ORDER BY useful DESC, id ASC
        LIMIT ?
        """;
    private static final String FIND_REVIEWS_BY_IDS_QUERY = """
        SELECT id, content, is_positive, user_id, film_id, useful
        FROM reviews
        WHERE id IN (:reviewIds)
        """;
    private static final String ADD_OPINION_QUERY = """
        INSERT INTO review_opinions(review_id, user_id, is_useful)
        SELECT ?, ?, ?
        WHERE NOT EXISTS (SELECT 1 FROM review_opinions WHERE review_id = ? AND user_id = ?)
        """;
    private static final String DELETE_OPINION_QUERY = """
        DELETE FROM review_opinions
        WHERE review_id = ? AND user_id = ? AND is_useful = ?
        """;
    private static final String INCREMENT_USEFUL_QUERY = "UPDATE reviews SET useful = useful + 1 WHERE id = ?";
    private static final String DECREMENT_USEFUL_QUERY = "UPDATE reviews SET useful = useful - 1 WHERE id = ?";
    private static final String IS_REVIEW_EXISTS_QUERY = "SELECT EXISTS(SELECT 1 FROM reviews WHERE id = ?)";
    private static final String IS_REVIEW_EXISTS_BY_USER_AND_FILM_QUERY = """
        SELECT EXISTS(SELECT 1 FROM reviews WHERE user_id = ? AND film_id = ?)
        """;

    public ReviewDbStorage(JdbcOperations jdbc, RowMapper<Review> mapper) {
        super(jdbc, mapper);
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public Review create(Review review) {
        Long id = insert(INSERT_REVIEW_QUERY,
                review.getContent(),
                review.getIsPositive(),
                review.getUserId(),
                review.getFilmId());
        review.setReviewId(id);
        review.setUseful(0);
        return review;
    }

    public void update(Review review) {
        update(UPDATE_REVIEW_QUERY,
                review.getContent(),
                review.getIsPositive(),
                review.getReviewId());
    }

    public boolean delete(Long reviewId) {
        return delete(DELETE_REVIEW_QUERY, reviewId);
    }

    public Optional<Review> findById(Long reviewId) {
        return findOne(FIND_REVIEW_BY_ID_QUERY, reviewId);
    }

    public List<Review> findByFilmId(Long filmId, int count) {
        return findMany(FIND_REVIEWS_BY_FILM_QUERY, filmId, count);
    }

    public List<Review> findAll(int count) {
        return findMany(FIND_ALL_REVIEWS_QUERY, count);
    }

    public List<Review> findBySeveralIds(List<Long> reviewIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("reviewIds", reviewIds);
        return namedJdbc.query(FIND_REVIEWS_BY_IDS_QUERY, params, mapper);
    }

    public boolean addLikeIfNotExists(Long reviewId, Long userId) {
        boolean added = jdbc.update(ADD_OPINION_QUERY, reviewId, userId, true, reviewId, userId) > 0;
        if (added) {
            jdbc.update(INCREMENT_USEFUL_QUERY, reviewId);
        }
        return added;
    }

    public boolean addDislikeIfNotExists(Long reviewId, Long userId) {
        boolean added = jdbc.update(ADD_OPINION_QUERY, reviewId, userId, false, reviewId, userId) > 0;
        if (added) {
            jdbc.update(DECREMENT_USEFUL_QUERY, reviewId);
        }
        return added;
    }

    public boolean deleteLikeIfExists(Long reviewId, Long userId) {
        boolean removed = jdbc.update(DELETE_OPINION_QUERY, reviewId, userId, true) > 0;
        if (removed) {
            jdbc.update(DECREMENT_USEFUL_QUERY, reviewId);
        }
        return removed;
    }

    public boolean deleteDislikeIfExists(Long reviewId, Long userId) {
        boolean removed = jdbc.update(DELETE_OPINION_QUERY, reviewId, userId, false) > 0;
        if (removed) {
            jdbc.update(INCREMENT_USEFUL_QUERY, reviewId);
        }
        return removed;
    }

    public boolean isReviewExists(Long reviewId) {
        Boolean exists = jdbc.queryForObject(IS_REVIEW_EXISTS_QUERY, Boolean.class, reviewId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean isReviewExistsByUserAndFilm(Long userId, Long filmId) {
        Boolean exists = jdbc.queryForObject(
                IS_REVIEW_EXISTS_BY_USER_AND_FILM_QUERY, Boolean.class, userId, filmId);
        return Boolean.TRUE.equals(exists);
    }
}