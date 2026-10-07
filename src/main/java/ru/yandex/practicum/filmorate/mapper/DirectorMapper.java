package ru.yandex.practicum.filmorate.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.dto.DirectorDto;
import ru.yandex.practicum.filmorate.dto.DirectorId;
import ru.yandex.practicum.filmorate.dto.NewDirectorRequest;
import ru.yandex.practicum.filmorate.dto.UpdateDirectorRequest;
import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DirectorMapper {

    public static Director mapToDirector(NewDirectorRequest request) {
        return Director.builder()
                .name(request.getName())
                .build();
    }

    public static DirectorDto mapToDto(Director director) {
        return DirectorDto.builder()
                .id(director.getId())
                .name(director.getName())
                .build();
    }

    public static Set<Long> mapDirectorIdToIds(Set<DirectorId> directorIds) {
        return directorIds.stream()
                .map(DirectorId::getId)
                .collect(Collectors.toSet());
    }

    public static List<DirectorDto> toDtoList(List<Director> directors) {
        return directors.stream()
                .map(DirectorMapper::mapToDto)
                .collect(Collectors.toUnmodifiableList());
    }

    public static void updateDirectorFields(Director director, UpdateDirectorRequest request) {
        director.setName(request.getName());
    }
}