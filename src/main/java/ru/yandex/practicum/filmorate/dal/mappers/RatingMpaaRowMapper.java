package ru.yandex.practicum.filmorate.dal.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.RatingMpaa;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class RatingMpaaRowMapper implements RowMapper<RatingMpaa> {
    @Override
    public RatingMpaa mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        RatingMpaa rating = new RatingMpaa();
        rating.setId(resultSet.getLong("id"));
        rating.setName(resultSet.getString("name"));
        return rating;
    }
}