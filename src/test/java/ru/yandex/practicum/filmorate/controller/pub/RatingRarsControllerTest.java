package ru.yandex.practicum.filmorate.controller.pub;

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
@DisplayName("RatingRarsController Тесты")
class RatingRarsControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("GET /rars → 200 и 5 рейтингов")
    void findAll_Should_ReturnAll_Test() throws Exception {
        mockMvc.perform(get("/rars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @DisplayName("GET /rars/{id}: существующий → 200")
    void findById_Should_Return_Test() throws Exception {
        mockMvc.perform(get("/rars/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("0+"));
    }

    @Test
    @DisplayName("GET /rars/{id}: несуществующий → 404")
    void findById_Should_ReturnNotFound_Test() throws Exception {
        mockMvc.perform(get("/rars/{id}", 999L))
                .andExpect(status().isNotFound());
    }
}