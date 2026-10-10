package ru.yandex.practicum.filmorate.dal;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDbStorage extends BaseDbStorage<User> {
    private static final String DELETE_USER_QUERY = "DELETE FROM users WHERE id = ?";
    private static final String FIND_BY_EMAIL_QUERY = "SELECT * FROM users WHERE email = ?";
    private static final String IS_USER_EXISTS_QUERY = "SELECT EXISTS(SELECT 1 FROM users WHERE id = ?)";
    private static final String IS_EMAIL_ALREADY_USE_QUERY = "SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)";
    private static final String FIND_USER_BY_ID_QUERY = """
        SELECT id, email, login, name, birthday, password, role, created_at
        FROM users
        WHERE id = ?
        """;
    private static final String FIND_USERS_BY_IDS_QUERY = """
        SELECT id, email, login, name, birthday, password, role, created_at
        FROM users
        WHERE id IN (:usersIds)
        ORDER BY id
        """;
    private static final String FIND_ALL_USERS_PAGINATED_QUERY = """
        SELECT id, email, login, name, birthday, password, role, created_at
        FROM users
        ORDER BY id
        LIMIT ? OFFSET ?
        """;
    private static final String INSERT_USER_QUERY = """
        INSERT INTO users(email, login, name, birthday, password, role)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    private static final String UPDATE_USER_QUERY = """
        UPDATE users
        SET email = ?, login = ?, name = ?, birthday = ?, password = ?
        WHERE id = ?
        """;
    private static final String UPDATE_PASSWORD_QUERY = """
        UPDATE users
        SET password = ?
        WHERE id = ?
        """;

    private final NamedParameterJdbcTemplate namedJdbc;

    public UserDbStorage(JdbcOperations jdbc, RowMapper<User> mapper) {
        super(jdbc, mapper);
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public User create(User newUser) {
        Long returnedId = insert(INSERT_USER_QUERY,
                newUser.getEmail(),
                newUser.getLogin(),
                newUser.getName(),
                newUser.getBirthday(),
                newUser.getPassword(),
                newUser.getRole().name()
        );
        newUser.setId(returnedId);
        return newUser;
    }

    public void update(User updatingUser) {
        update(UPDATE_USER_QUERY,
                updatingUser.getEmail(),
                updatingUser.getLogin(),
                updatingUser.getName(),
                updatingUser.getBirthday(),
                updatingUser.getPassword(),
                updatingUser.getId());
    }

    public void updatePassword(Long userId, String passwordHash) {
        update(UPDATE_PASSWORD_QUERY, passwordHash, userId);
    }

    public List<User> findAll(int from, int size) {
        return findMany(FIND_ALL_USERS_PAGINATED_QUERY, size, from);
    }

    public Optional<User> findById(Long userId) {
        return findOne(FIND_USER_BY_ID_QUERY, userId);
    }

    /**
     * Ищет пользователя по email.
     * <p>Ожидает, что email уже нормализован (нижний регистр).
     * См. {@code UserService.normalizeCredential}.
     */
    public Optional<User> findByEmail(String email) {
        return findOne(FIND_BY_EMAIL_QUERY, email);
    }

    public boolean isEmailAlreadyUse(String email) {
        Boolean exists = jdbc.queryForObject(IS_EMAIL_ALREADY_USE_QUERY, Boolean.class, email);
        return Boolean.TRUE.equals(exists);
    }

    public boolean isUserExists(Long userId) {
        Boolean exists = jdbc.queryForObject(IS_USER_EXISTS_QUERY, Boolean.class, userId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean delete(Long userId) {
        return delete(DELETE_USER_QUERY, userId);
    }

    public List<User> findBySeveralIds(List<Long> usersIds) {
        MapSqlParameterSource param = new MapSqlParameterSource("usersIds", usersIds);
        return namedJdbc.query(FIND_USERS_BY_IDS_QUERY, param, mapper);
    }
}