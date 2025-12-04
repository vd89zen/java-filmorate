package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.GenreDbStorage;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.GenreMapper;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.ValidationError;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GenreService {
    private final GenreDbStorage genreDbStorage;

    public GenreService(GenreDbStorage genreDbStorage) {
        this.genreDbStorage = genreDbStorage;
    }

    public List<GenreDto> findAll() {
        log.info("Получаем список всех жанров.");
        return GenreMapper.toDtoSet(genreDbStorage.findAll());
    }

    public GenreDto findById(Long genreId) {
        log.info("Получаем жанр по ID: {}.", genreId);
        return GenreMapper.toDto(genreDbStorage.findById(genreId)
                .orElseThrow(() -> new NotFoundException(String.format("Жанр с id = %d не найден.", genreId))));
    }

    public List<GenreDto> getGenresDto(Set<Long> genresIds) {
        log.info("Получаем жанры по списку ID: {}.", genresIds);
        if (genresIds == null || genresIds.isEmpty()) {
            throw new ValidationException(ValidationError.builder()
                    .field("genresIds")
                    .message("Список ID жанров пуст либо null")
                    .rejectedValue(genresIds)
                    .build());
        }

        List<Genre> foundGenres = genreDbStorage.findByIds(genresIds);
        if (foundGenres.size() < genresIds.size()) {
            Set<Long> foundIds = foundGenres.stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());

            Set<Long> missingIds = new HashSet<>(genresIds);
            missingIds.removeAll(foundIds);

            throw new NotFoundException(String.format("Не найдены жанры с ID: %s", missingIds));
        }

        return GenreMapper.toDtoSet(foundGenres);
    }

    public void refreshCache() {
        genreDbStorage.evictCache();
    }
}
