package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final ConcurrentHashMap<Long, Film> films = new ConcurrentHashMap<>();

    @Override
    public Film create(Film film) {
        films.put(film.getId(), film);
        return film.selfCopy();
    }

    @Override
    public Film update(Film film) {
        films.put(film.getId(), film);
        return film.selfCopy();
    }

    @Override
    public void delete(long filmId) {
        films.remove(filmId);
    }

    @Override
    public Optional<Film> findById(long filmId) {
        return Optional.ofNullable(films.get(filmId))
                .map(Film::selfCopy);
    }

    @Override
    public Collection<Film> findAll() {
        return films.values().stream()
                .map(Film::selfCopy)
                .collect(Collectors.toUnmodifiableList());
    }
}
