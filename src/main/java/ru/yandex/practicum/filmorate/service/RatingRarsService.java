package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.RatingRarsDbStorage;
import ru.yandex.practicum.filmorate.dto.RatingRarsDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.RatingRarsMapper;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RatingRarsService {
    private final RatingRarsDbStorage ratingRarsDbStorage;

    public RatingRarsService(RatingRarsDbStorage ratingRarsDbStorage) {
        this.ratingRarsDbStorage = ratingRarsDbStorage;
    }

    public List<RatingRarsDto> findAll() {
        log.info("Получаем список (DTO) всех рейтингов RARS.");
        return ratingRarsDbStorage.findAll().stream()
                .map(RatingRarsMapper::toDto)
                .collect(Collectors.toUnmodifiableList());
    }

    public RatingRarsDto getRatingRarsDtoById(Long ratingId) {
        log.info("Получение рейтинга RARS (DTO) по ID: {}.", ratingId);
        return RatingRarsMapper.toDto(ratingRarsDbStorage.findById(ratingId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Рейтинг RARS с id = %d не найден.", ratingId))));
    }
}