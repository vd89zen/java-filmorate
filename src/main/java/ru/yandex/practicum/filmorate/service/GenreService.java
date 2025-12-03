package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.dal.GenreDbStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GenreService {
    private final GenreDbStorage genreDbStorage;

    public GenreService(GenreDbStorage genreDbStorage) {
        this.genreDbStorage = genreDbStorage;
    }

    public List<Genre> getAllGenres() {
        return genreDbStorage.findAll();
    }

    public Genre getGenreById(Long genreId) {
        return genreDbStorage.findById(genreId)
                .orElseThrow(() -> new NotFoundException(String.format("Жанр с id = %d не найден.", genreId)));
    }

    public Set<Long> getAllGenreIds() {
        return genreDbStorage.findAll().stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());
    }

    public void validateGenreIds(Set<Long> checkingIds) {
        if (checkingIds == null) {
            return;
        }

        Set<Long> allGenreIds = getAllGenreIds();
        for (Long id : checkingIds) {
            if (allGenreIds.contains(id) == false) {
                throw new NotFoundException(String.format("Жанр с id = %d не найден.", id));
            }
        }
    }

    public void refreshCache() {
        genreDbStorage.evictCache();
    }
}
