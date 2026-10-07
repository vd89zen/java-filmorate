package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.DirectorDto;
import ru.yandex.practicum.filmorate.dto.NewDirectorRequest;
import ru.yandex.practicum.filmorate.dto.UpdateDirectorRequest;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/directors")
public class DirectorController {
    private final DirectorService directorService;

    public DirectorController(DirectorService directorService) {
        this.directorService = directorService;
    }

    @PostMapping
    public ResponseEntity<DirectorDto> create(@Valid @RequestBody NewDirectorRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(directorService.create(request));
    }

    @PutMapping
    public ResponseEntity<DirectorDto> update(@Valid @RequestBody UpdateDirectorRequest request) {
        return ResponseEntity.ok(directorService.update(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NotNull @Positive Long id) {
        directorService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DirectorDto> findById(@PathVariable @NotNull @Positive Long id) {
        return ResponseEntity.ok(directorService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<DirectorDto>> findAll() {
        return ResponseEntity.ok(directorService.findAll());
    }
}