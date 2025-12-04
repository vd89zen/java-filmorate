package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.RatingMpaaDto;
import ru.yandex.practicum.filmorate.model.RatingMpaa;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class  RatingMpaaMapper {

    public static RatingMpaaDto toDto(RatingMpaa rating) {
        return new RatingMpaaDto(
                rating.getId(),
                rating.getName()
        );
    }
}
