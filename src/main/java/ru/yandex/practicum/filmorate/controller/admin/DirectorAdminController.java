package ru.yandex.practicum.filmorate.controller.admin;

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

@Validated
@RestController
@RequestMapping("/admin/directors")
public class DirectorAdminController {
    private final DirectorService directorService;

    public DirectorAdminController(DirectorService directorService) {
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
        return ResponseEntity
                .ok(directorService.update(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NotNull @Positive Long id) {
        directorService.delete(id);
        return ResponseEntity
                .noContent().build();
    }
}