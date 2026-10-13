package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.RatingRarsDto;
import ru.yandex.practicum.filmorate.model.RatingRars;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RatingRarsMapper {

    public static RatingRarsDto toDto(RatingRars rating) {
        return new RatingRarsDto(
                rating.getId(),
                rating.getName()
        );
    }
}