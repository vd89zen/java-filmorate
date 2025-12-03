package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.RatingMpaaDbStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.RatingMpaa;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RatingMpaaService {
    private final RatingMpaaDbStorage ratingMpaaDbStorage;

    public RatingMpaaService(RatingMpaaDbStorage ratingMpaaDbStorage) {
        this.ratingMpaaDbStorage = ratingMpaaDbStorage;
    }

    public List<RatingMpaa> getAllRatings() {
        return ratingMpaaDbStorage.findAll();
    }

    public RatingMpaa getRatingById(Long ratingId) {
        return ratingMpaaDbStorage.findById(ratingId)
                .orElseThrow(() -> new NotFoundException(String.format("Рейтинг с id = %d не найден.", ratingId)));
    }

    public Set<Long> getAllRatingIds() {
        return ratingMpaaDbStorage.findAll().stream()
                .map(RatingMpaa::getId)
                .collect(Collectors.toSet());
    }

    public void refreshCache() {
        ratingMpaaDbStorage.evictCache();
    }
}

