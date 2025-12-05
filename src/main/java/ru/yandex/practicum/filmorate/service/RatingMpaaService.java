package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.RatingMpaaDbStorage;
import ru.yandex.practicum.filmorate.dto.RatingMpaaDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.mapper.RatingMpaaMapper;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RatingMpaaService {
    private final RatingMpaaDbStorage ratingMpaaDbStorage;

    public RatingMpaaService(RatingMpaaDbStorage ratingMpaaDbStorage) {
        this.ratingMpaaDbStorage = ratingMpaaDbStorage;
    }

    public List<RatingMpaaDto> findAll() {
        log.info("Получаем список (DTO) всех рейтингов MPA.");
        return ratingMpaaDbStorage.findAll().stream()
                .map(RatingMpaaMapper::toDto)
                .collect(Collectors.toUnmodifiableList());
    }

    public RatingMpaaDto getRatingMpaaDtoById(Long ratingId) {
        log.info("Получение рейтинга (DTO) по ID: {}.", ratingId);
        return RatingMpaaMapper.toDto(ratingMpaaDbStorage.findById(ratingId)
                .orElseThrow(() -> new NotFoundException(String.format("Рейтинг с id = %d не найден.", ratingId))));
    }

    public void refreshCache() {
        ratingMpaaDbStorage.evictCache();
    }
}

