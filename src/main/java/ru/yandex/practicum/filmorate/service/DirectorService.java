package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.dal.DirectorDbStorage;
import ru.yandex.practicum.filmorate.dto.DirectorDto;
import ru.yandex.practicum.filmorate.dto.NewDirectorRequest;
import ru.yandex.practicum.filmorate.dto.UpdateDirectorRequest;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.mapper.DirectorMapper;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.ValidationError;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DirectorService {
    private static final String DIRECTOR_NOT_FOUND = "Режиссёр с id = %d не найден.";

    private final DirectorDbStorage directorStorage;

    public void checkDirectorExists(Long directorId) {
        if (directorStorage.isDirectorExists(directorId) == false) {
            throw new NotFoundException(String.format(DIRECTOR_NOT_FOUND, directorId));
        }
    }

    @Transactional
    public DirectorDto create(NewDirectorRequest request) {
        log.info("DirectorService: создание режиссёра {}", request);

        if (directorStorage.isNameAlreadyUse(request.getName())) {
            throw new ValidationException(ValidationError.builder()
                    .field("name")
                    .message("Режиссёр с таким именем уже существует.")
                    .rejectedValue(request.getName())
                    .build());
        }

        Director director = DirectorMapper.mapToDirector(request);
        Director saved = directorStorage.create(director);
        return DirectorMapper.mapToDto(saved);
    }

    @Transactional
    public DirectorDto update(UpdateDirectorRequest request) {
        log.info("DirectorService: обновление режиссёра {}", request);

        Director director = getDirectorOrThrow(request.getId());
        if (director.getName().equals(request.getName()) == false
                && directorStorage.isNameAlreadyUse(request.getName())) {
            throw new ValidationException(ValidationError.builder()
                    .field("name")
                    .message("Режиссёр с таким именем уже существует.")
                    .rejectedValue(request.getName())
                    .build());
        }

        DirectorMapper.updateDirectorFields(director, request);
        directorStorage.update(director);
        return DirectorMapper.mapToDto(director);
    }

    @Transactional
    public void delete(Long directorId) {
        log.info("DirectorService: удаление режиссёра ID {}", directorId);
        if (directorStorage.delete(directorId) == false) {
            throw new NotFoundException(String.format(DIRECTOR_NOT_FOUND, directorId));
        }
    }

    @Transactional(readOnly = true)
    public DirectorDto findById(Long directorId) {
        log.info("DirectorService: получение режиссёра ID {}", directorId);
        return DirectorMapper.mapToDto(getDirectorOrThrow(directorId));
    }

    @Transactional(readOnly = true)
    public List<DirectorDto> findAll() {
        log.info("DirectorService: получение всех режиссёров");
        return directorStorage.findAll().stream()
                .map(DirectorMapper::mapToDto)
                .collect(Collectors.toList());
    }

    public List<DirectorDto> getDirectorsDto(Set<Long> directorIds) {
        log.info("Получаем режиссёров по списку ID: {}.", directorIds);

        if (directorIds == null) {
            throw new IllegalArgumentException("directorIds must not be null");
        }
        if (directorIds.isEmpty()) {
            return List.of();
        }

        List<Director> found = directorStorage.findByIds(directorIds);
        if (found.size() < directorIds.size()) {
            Set<Long> foundIds = found.stream()
                    .map(Director::getId)
                    .collect(Collectors.toSet());

            Set<Long> missingIds = new HashSet<>(directorIds);
            missingIds.removeAll(foundIds);

            throw new NotFoundException(String.format("Не найдены режиссёры с ID: %s", missingIds));
        }

        return found.stream()
                .map(DirectorMapper::mapToDto)
                .collect(Collectors.toList());
    }

    private Director getDirectorOrThrow(Long directorId) {
        return directorStorage.findById(directorId)
                .orElseThrow(() -> new NotFoundException(String.format(DIRECTOR_NOT_FOUND, directorId)));
    }
}