package ru.yandex.practicum.filmorate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    private String query;
    private Set<String> by;
    private Integer year;
    private Integer yearFrom;
    private Integer yearTo;
    private Integer duration;
    private Integer durationFrom;
    private Integer durationTo;
    private Set<Long> mpaIds;
    private Set<Long> rarsIds;
    private int from;
    private int size;
}