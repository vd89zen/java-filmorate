package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.UserStorage;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDbStorage extends BaseDbStorage implements UserStorage {
    private static final String DELETE_USER_QUERY = "DELETE FROM users WHERE id = ?";
    private static final String FIND_BY_EMAIL_QUERY = "SELECT * FROM users WHERE email = ?";
    private static final String IS_USER_EXISTS_QUERY = "SELECT EXISTS(SELECT 1 FROM users WHERE id = ?)";
    private static final String IS_EMAIL_ALREADY_USE_QUERY = "SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)";
    private static final String FIND_USER_BY_ID_QUERY = """
        SELECT id, email, login, name, birthday
        FROM users
        WHERE id = ?
        """;
    private static final String FIND_USERS_BY_IDS_QUERY = """
        SELECT id, email, login, name, birthday
        FROM users
        WHERE id IN (:usersIds)
        ORDER BY id
        """;
    private static final String FIND_ALL_USERS_QUERY = """
        SELECT id, email, login, name, birthday
        FROM users
        ORDER BY id
        """;
    private static final String INSERT_USER_QUERY = """
        INSERT INTO users(email, login, name, birthday)
        VALUES (?, ?, ?, ?)
        """;
    private static final String UPDATE_USER_QUERY = """
        UPDATE users
        SET email = ?, login = ?, name = ?, birthday = ?
        WHERE id = ?
        """;

    private final NamedParameterJdbcTemplate namedJdbc;

    public UserDbStorage(JdbcTemplate jdbc, RowMapper<User> mapper) {
        super(jdbc, mapper);
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    @Override
    public User create(User newUser) {
        Long returnedId = insert(INSERT_USER_QUERY,
                newUser.getEmail().toLowerCase(),
                newUser.getLogin().toLowerCase(),
                newUser.getName().toLowerCase(),
                newUser.getBirthday()
        );
        newUser.setId(returnedId);
        return newUser;
    }

    @Override
    public void update(User updatingUser) {
        update(UPDATE_USER_QUERY,
                updatingUser.getEmail(),
                updatingUser.getLogin(),
                updatingUser.getName(),
                updatingUser.getBirthday(),
                updatingUser.getId());
    }

    @Override
    public List<User> findAll() {
        return findMany(FIND_ALL_USERS_QUERY);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return findOne(FIND_USER_BY_ID_QUERY, userId);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return findOne(FIND_BY_EMAIL_QUERY, email);
    }

    @Override
    public boolean isEmailAlreadyUse(String email) {
        return jdbc.queryForObject(IS_EMAIL_ALREADY_USE_QUERY, Boolean.class, email);
    }

    @Override
    public boolean isUserExists(Long userId) {
        return jdbc.queryForObject(IS_USER_EXISTS_QUERY, Boolean.class, userId);
    }

    @Override
    public boolean delete(Long userId) {
        return delete(DELETE_USER_QUERY, userId);
    }

    @Override
    public List<User> findBySeveralIds(List<Long> usersIds) {
        MapSqlParameterSource param = new MapSqlParameterSource("usersIds", usersIds);
        return namedJdbc.query(FIND_USERS_BY_IDS_QUERY, param, mapper);
    }
}
