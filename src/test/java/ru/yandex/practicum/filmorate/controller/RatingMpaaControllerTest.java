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
@DisplayName("Тесты RatingMpaaController")
class RatingMpaaControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("GET /mpa → 200 и 5 рейтингов")
    void findAll_Should_ReturnAll_Test() throws Exception {
        mockMvc.perform(get("/mpa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @DisplayName("GET /mpa/{id}: существующий → 200")
    void findById_Should_Return_Test() throws Exception {
        mockMvc.perform(get("/mpa/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /mpa/{id}: несуществующий → 404")
    void findById_Should_ReturnNotFound_Test() throws Exception {
        mockMvc.perform(get("/mpa/{id}", 999L))
                .andExpect(status().isNotFound());
    }
}