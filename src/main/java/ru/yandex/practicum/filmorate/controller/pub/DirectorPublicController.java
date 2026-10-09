package ru.yandex.practicum.filmorate.controller.pub;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.dto.DirectorDto;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/directors")
public class DirectorPublicController {
    private final DirectorService directorService;

    public DirectorPublicController(DirectorService directorService) {
        this.directorService = directorService;
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