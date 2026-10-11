package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.GenreDbStorage;
import ru.yandex.practicum.filmorate.dto.GenreDto;
import ru.yandex.practicum.filmorate.dto.NewGenreRequest;
import ru.yandex.practicum.filmorate.dto.UpdateGenreRequest;
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
    private static final String GENRE_NOT_FOUND = "Жанр с id = %d не найден.";

    private final GenreDbStorage genreDbStorage;

    public GenreService(GenreDbStorage genreDbStorage) {
        this.genreDbStorage = genreDbStorage;
    }

    public List<GenreDto> findAll() {
        log.info("Получаем список всех жанров.");
        return GenreMapper.toDtoList(genreDbStorage.findAll());
    }

    public GenreDto findById(Long genreId) {
        log.info("Получаем жанр по ID: {}.", genreId);
        return GenreMapper.toDto(getGenreOrThrow(genreId));
    }

    public void checkGenreExists(Long genreId) {
        if (!genreDbStorage.isGenreExists(genreId)) {
            throw new NotFoundException(String.format(GENRE_NOT_FOUND, genreId));
        }
    }

    @Transactional
    public GenreDto create(NewGenreRequest request) {
        log.info("GenreService: создание жанра {}.", request);

        if (genreDbStorage.isNameAlreadyUse(request.getName())) {
            throw new ValidationException(ValidationError.builder()
                    .field("name")
                    .message("Жанр с таким названием уже существует.")
                    .rejectedValue(request.getName())
                    .build());
        }

        Genre newGenre = GenreMapper.mapToGenre(request);
        Genre saved = genreDbStorage.create(newGenre);
        return GenreMapper.toDto(saved);
    }

    @Transactional
    public GenreDto update(UpdateGenreRequest request) {
        log.info("GenreService: обновление жанра {}.", request);

        Genre genre = getGenreOrThrow(request.getId());

        if (!genre.getName().equals(request.getName())
                && genreDbStorage.isNameAlreadyUse(request.getName())) {
            throw new ValidationException(ValidationError.builder()
                    .field("name")
                    .message("Жанр с таким названием уже существует.")
                    .rejectedValue(request.getName())
                    .build());
        }

        genre.setName(request.getName());
        genreDbStorage.update(genre);
        return GenreMapper.toDto(genre);
    }

    @Transactional
    public void delete(Long genreId) {
        log.info("GenreService: удаление жанра ID {}.", genreId);

        checkGenreExists(genreId);

        if (genreDbStorage.isGenreInUse(genreId)) {
            throw new ValidationException(ValidationError.builder()
                    .field("genreId")
                    .message("Нельзя удалить жанр, который используется в фильмах.")
                    .rejectedValue(genreId)
                    .build());
        }

        genreDbStorage.delete(genreId);
    }

    public List<GenreDto> getGenresDto(Set<Long> genreIds) {
        log.info("Получаем жанры по списку ID: {}.", genreIds);

        if (genreIds == null) {
            throw new IllegalArgumentException("genresIds must not be null");
        }
        if (genreIds.isEmpty()) {
            return List.of();
        }

        List<Genre> foundGenres = genreDbStorage.findByIds(genreIds);
        if (foundGenres.size() < genreIds.size()) {
            Set<Long> foundIds = foundGenres.stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());
            Set<Long> missingIds = new HashSet<>(genreIds);
            missingIds.removeAll(foundIds);
            throw new NotFoundException(String.format("Не найдены жанры с ID: %s", missingIds));
        }
        return GenreMapper.toDtoList(foundGenres);
    }

    private Genre getGenreOrThrow(Long genreId) {
        return genreDbStorage.findById(genreId)
                .orElseThrow(() -> new NotFoundException(String.format(GENRE_NOT_FOUND, genreId)));
    }
}
