package ru.yandex.practicum.filmorate.controller.pub;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.dal.DirectorDbStorage;
import ru.yandex.practicum.filmorate.model.Director;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("DirectorController Тесты (публичные эндпоинты)")
class DirectorPublicControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final DirectorDbStorage directorStorage;

    @BeforeEach
    void setUp() {
        cleanUp();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM film_directors");
        jdbcTemplate.execute("DELETE FROM directors");
    }

    private Long createDirector(String name) {
        return directorStorage.create(Director.builder().name(name).build()).getId();
    }

    @Nested
    @DisplayName("GET /directors")
    class FindAllTests {

        @Test
        @DisplayName("Пустой список — без аутентификации")
        void findAll_Should_ReturnEmpty_Test() throws Exception {
            mockMvc.perform(get("/directors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Несколько режиссёров — сортировка по id, без аутентификации")
        void findAll_Should_ReturnSortedById_Test() throws Exception {
            createDirector("A");
            createDirector("B");

            mockMvc.perform(get("/directors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("A"))
                    .andExpect(jsonPath("$[1].name").value("B"));
        }
    }

    @Nested
    @DisplayName("GET /directors/{id}")
    class FindByIdTests {

        @Test
        @DisplayName("Существующий → 200")
        void findById_Should_ReturnOk_Test() throws Exception {
            Long id = createDirector("Nolan");

            mockMvc.perform(get("/directors/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.name").value("Nolan"));
        }

        @Test
        @DisplayName("Несуществующий → 404")
        void findById_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(get("/directors/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }
}