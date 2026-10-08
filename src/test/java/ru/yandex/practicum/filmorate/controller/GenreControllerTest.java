package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("GenreController Тесты")
class GenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /genres → 200 и 6 жанров")
    void findAll_Should_ReturnAll_Test() throws Exception {
        mockMvc.perform(get("/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    @DisplayName("GET /genres/{id}: существующий → 200")
    void findById_Should_Return_Test() throws Exception {
        mockMvc.perform(get("/genres/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /genres/{id}: несуществующий → 404")
    void findById_Should_ReturnNotFound_Test() throws Exception {
        mockMvc.perform(get("/genres/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /genres/{id}: id < 1 → 400")
    void findById_Should_ReturnBadRequest_ForNegativeId_Test() throws Exception {
        mockMvc.perform(get("/genres/{id}", 0L))
                .andExpect(status().isBadRequest());
    }
}