package ru.yandex.practicum.filmorate.model;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    Film create(Film film);

    void update(Film film);

    boolean delete(Long filmId);

    Optional<Film> findById(Long filmId);

    List<Film> findAll();

    List<Film> findBySeveralIds(List<Long> filmsIds);

    boolean isFilmExists(Long filmId);
}
