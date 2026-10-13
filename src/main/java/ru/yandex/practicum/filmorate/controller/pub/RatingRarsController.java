package ru.yandex.practicum.filmorate.controller.pub;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.dto.RatingRarsDto;
import ru.yandex.practicum.filmorate.service.RatingRarsService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/rars")
public class RatingRarsController {
    private final RatingRarsService ratingRarsService;

    public RatingRarsController(RatingRarsService ratingRarsService) {
        this.ratingRarsService = ratingRarsService;
    }

    @GetMapping("/{ratingId}")
    public ResponseEntity<RatingRarsDto> findById(@PathVariable @NotNull @Positive Long ratingId) {
        return ResponseEntity
                .ok(ratingRarsService.getRatingRarsDtoById(ratingId));
    }

    @GetMapping
    public ResponseEntity<List<RatingRarsDto>> findAll() {
        return ResponseEntity
                .ok(ratingRarsService.findAll());
    }
}