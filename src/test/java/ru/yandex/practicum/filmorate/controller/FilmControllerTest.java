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
import ru.yandex.practicum.filmorate.dto.NewFilmRequest;
import ru.yandex.practicum.filmorate.dto.RatingMpaaId;
import java.time.LocalDate;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Тесты FilmController")
class FilmControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanUp();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.execute("DELETE FROM film_likes");
        jdbcTemplate.execute("DELETE FROM film_genres");
        jdbcTemplate.execute("DELETE FROM film_directors");
        jdbcTemplate.execute("DELETE FROM films");
        jdbcTemplate.execute("DELETE FROM directors");
    }

    private String newFilmJson(String name) throws Exception {
        NewFilmRequest r = new NewFilmRequest();
        r.setName(name);
        r.setDescription("desc");
        r.setReleaseDate(LocalDate.of(2000, 1, 1));
        r.setDuration(120);
        r.setMpa(new RatingMpaaId(1L));
        return objectMapper.writeValueAsString(r);
    }

    @Nested
    @DisplayName("POST /films")
    class CreateTests {

        @Test
        @DisplayName("201 + тело")
        void create_Should_ReturnCreated_Test() throws Exception {
            mockMvc.perform(post("/films")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newFilmJson("Film")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("Film"));
        }

        @Test
        @DisplayName("Пустое имя → 400")
        void create_Should_ReturnBadRequest_ForBlankName_Test() throws Exception {
            mockMvc.perform(post("/films")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newFilmJson("")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /films")
    class FindAllTests {

        @Test
        @DisplayName("size=100 максимум — ок")
        void findAll_Should_ReturnOk_AtMaxSize_Test() throws Exception {
            mockMvc.perform(get("/films").param("size", "100"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("size=101 → 400")
        void findAll_Should_ReturnBadRequest_ForSizeOverMax_Test() throws Exception {
            mockMvc.perform(get("/films").param("size", "101"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /films/director/{directorId}")
    class FilmsByDirectorTests {

        private Long createDirector(String name) throws Exception {
            NewDirectorRequest req = new NewDirectorRequest();
            req.setName(name);

            String response = mockMvc.perform(post("/directors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andReturn().getResponse().getContentAsString();

            return objectMapper.readTree(response).get("id").asLong();
        }

        @Test
        @DisplayName("sortBy=year → 200 и пустой список, если фильмов нет")
        void year_Should_ReturnOk_Test() throws Exception {
            Long directorId = createDirector("Nolan");

            mockMvc.perform(get("/films/director/{id}", directorId).param("sortBy", "year"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Без sortBy → дефолт year")
        void defaultSort_Should_ReturnOk_Test() throws Exception {
            Long directorId = createDirector("Nolan");

            mockMvc.perform(get("/films/director/{id}", directorId))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("sortBy=bad → 400")
        void badSort_Should_ReturnBadRequest_Test() throws Exception {
            Long directorId = createDirector("Nolan");

            mockMvc.perform(get("/films/director/{id}", directorId).param("sortBy", "bad"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Несуществующий режиссёр → 404")
        void missingDirector_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(get("/films/director/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /films/{filmId}")
    class DeleteTests {

        @Test
        @DisplayName("Удаление несуществующего → 404")
        void delete_Should_ReturnNotFound_Test() throws Exception {
            mockMvc.perform(delete("/films/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /films/search")
    class SearchTests {

        @Test
        @DisplayName("by=title → 200")
        void search_ByTitle_Should_ReturnOk_Test() throws Exception {
            mockMvc.perform(get("/films/search")
                            .param("query", "test")
                            .param("by", "title"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("by=director,title,description → 200")
        void search_ByAllFields_Should_ReturnOk_Test() throws Exception {
            mockMvc.perform(get("/films/search")
                            .param("query", "test")
                            .param("by", "director,title,description"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("by без query → 400")
        void search_ByWithoutQuery_Should_ReturnBadRequest_Test() throws Exception {
            mockMvc.perform(get("/films/search")
                            .param("by", "director"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Недопустимое by → 400")
        void search_InvalidBy_Should_ReturnBadRequest_Test() throws Exception {
            mockMvc.perform(get("/films/search")
                            .param("query", "test")
                            .param("by", "year"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("year + yearFrom → 400")
        void search_YearConflict_Should_ReturnBadRequest_Test() throws Exception {
            mockMvc.perform(get("/films/search")
                            .param("year", "2010")
                            .param("yearFrom", "2000"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("mpaIds=1,2 → 200")
        void search_MpaIdsCsv_Should_ReturnOk_Test() throws Exception {
            mockMvc.perform(get("/films/search")
                            .param("mpaIds", "1,2"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Без параметров → 200 и все фильмы")
        void search_NoParams_Should_ReturnOk_Test() throws Exception {
            mockMvc.perform(get("/films/search"))
                    .andExpect(status().isOk());
        }
    }
}