package ru.yandex.practicum.filmorate.dal.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.RatingRars;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class RatingRarsRowMapper implements RowMapper<RatingRars> {
    @Override
    public RatingRars mapRow(ResultSet rs, int rowNum) throws SQLException {
        RatingRars rating = new RatingRars();
        rating.setId(rs.getLong("id"));
        rating.setName(rs.getString("name"));
        return rating;
    }
}