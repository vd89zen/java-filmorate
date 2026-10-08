package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.dto.NewDirectorRequest;
import ru.yandex.practicum.filmorate.dto.UpdateDirectorRequest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("DirectorController Тесты")
class DirectorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM film_directors");
        jdbcTemplate.execute("DELETE FROM directors");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM film_directors");
        jdbcTemplate.execute("DELETE FROM directors");
    }

    @Nested
    @DisplayName("POST /directors")
    class CreateTests {

        @Test
        @DisplayName("Создание: 201 + тело с id и name")
        void create_Should_ReturnCreated_Test() throws Exception {
            NewDirectorRequest req = new NewDirectorRequest();
            req.setName("Nolan");

            mockMvc.perform(post("/directors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("Nolan"));
        }

        @Test
        @DisplayName("Пустое имя → 400")
        void create_Should_ReturnBadRequest_ForBlankName_Test() throws Exception {
            NewDirectorRequest req = new NewDirectorRequest();
            req.setName("");

            mockMvc.perform(post("/directors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Дубль имени → 400")
        void create_Should_ReturnBadRequest_OnDuplicate_Test() throws Exception {
            NewDirectorRequest req = new NewDirectorRequest();
            req.setName("Nolan");
            String body = objectMapper.writeValueAsString(req);

            mockMvc.perform(post("/directors")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/directors")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /directors")
    class UpdateTests {

        @Test
        @DisplayName("Обновление имени")
        void update_Should_ChangeName_Test() throws Exception {
            Long id = createDirector("Old");

            UpdateDirectorRequest req = new UpdateDirectorRequest();
            req.setId(id);
            req.setName("New");

            mockMvc.perform(put("/directors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New"));
        }

        @Test
        @DisplayName("Несуществующий id → 404")
        void update_Should_ReturnNotFound_Test() throws Exception {
            UpdateDirectorRequest req = new UpdateDirectorRequest();
            req.setId(999L);
            req.setName("X");

            mockMvc.perform(put("/directors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /directors/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Удаление существующего → 204")
        void delete_Should_ReturnNoContent_Test() throws Exception {
            Long id = createDirector("ToDelete");
            mockMvc.perform(delete("/directors/{id}", id))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Несуществующий → 404")
        void delete_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(delete("/directors/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /directors и GET /directors/{id}")
    class FindTests {

        @Test
        @DisplayName("Пустой список")
        void findAll_Should_ReturnEmpty_Test() throws Exception {
            mockMvc.perform(get("/directors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Несколько режиссёров — сортировка по id")
        void findAll_Should_ReturnSortedById_Test() throws Exception {
            createDirector("A");
            createDirector("B");

            mockMvc.perform(get("/directors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("A"))
                    .andExpect(jsonPath("$[1].name").value("B"));
        }

        @Test
        @DisplayName("GET /directors/{id}: 404 для несуществующего")
        void findById_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(get("/directors/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    private Long createDirector(String name) throws Exception {
        NewDirectorRequest req = new NewDirectorRequest();
        req.setName(name);

        String response = mockMvc.perform(post("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}