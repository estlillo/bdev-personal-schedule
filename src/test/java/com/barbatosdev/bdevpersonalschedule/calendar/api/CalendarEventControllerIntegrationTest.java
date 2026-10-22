package com.barbatosdev.bdevpersonalschedule.calendar.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class CalendarEventControllerIntegrationTest {

    private static Path databaseFile;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcClient jdbcClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() throws IOException {
        databaseFile = Files.createTempFile("rehab-scheduler-calendar-test-", ".db");
    }

    @AfterAll
    static void afterAll() {
        if (databaseFile != null) {
            try {
                Files.deleteIfExists(databaseFile);
            } catch (IOException ignored) {
                // SQLite file can remain locked briefly on Windows.
            }
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + databaseFile.toAbsolutePath());
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jdbcClient.sql("DELETE FROM calendar_event").update();
        jdbcClient.sql("DELETE FROM schedule_exception").update();
        jdbcClient.sql("DELETE FROM working_window").update();
        jdbcClient.sql("DELETE FROM professional").update();
    }

    @Test
    void createListPatchDeleteFlowShouldWork() throws Exception {
        String professionalId = createProfessional();

        String createBody = """
            {
              "professionalId": "%s",
              "startsAt": "2026-10-16T08:00:00",
              "endsAt": "2026-10-16T08:45:00",
              "category": "MEETING",
              "note": "Bloqueo"
            }
            """.formatted(professionalId);

        MvcResult created = mockMvc.perform(post("/api/v1/calendar-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andReturn();

        JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        String eventId = createdJson.get("id").asText();

        mockMvc.perform(get("/api/v1/calendar-events")
                .param("professionalId", professionalId)
                .param("from", "2026-10-12")
                .param("to", "2026-10-18"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        String patchBody = """
            {
              "endsAt": "2026-10-16T09:00:00",
              "note": "  Reprogramado  "
            }
            """;

        mockMvc.perform(patch("/api/v1/calendar-events/{id}", eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.endsAt").value("2026-10-16T09:00:00"))
            .andExpect(jsonPath("$.note").value("Reprogramado"));

        mockMvc.perform(delete("/api/v1/calendar-events/{id}", eventId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/calendar-events")
                .param("professionalId", professionalId)
                .param("from", "2026-10-12")
                .param("to", "2026-10-18"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createShouldReturnConflictWhenOverlaps() throws Exception {
        String professionalId = createProfessional();

        String firstBody = """
            {
              "professionalId": "%s",
              "startsAt": "2026-10-16T10:00:00",
              "endsAt": "2026-10-16T10:45:00",
              "category": "MEETING"
            }
            """.formatted(professionalId);

        mockMvc.perform(post("/api/v1/calendar-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstBody))
            .andExpect(status().isCreated());

        String overlappingBody = """
            {
              "professionalId": "%s",
              "startsAt": "2026-10-16T10:30:00",
              "endsAt": "2026-10-16T11:00:00",
              "category": "WORKSHOP"
            }
            """.formatted(professionalId);

        mockMvc.perform(post("/api/v1/calendar-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(overlappingBody))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"));
    }

    @Test
    void createShouldAllowOutsideWorkingHoursInPhase3() throws Exception {
        String professionalId = createProfessional();

        String body = """
            {
              "professionalId": "%s",
              "startsAt": "2026-10-16T20:00:00",
              "endsAt": "2026-10-16T21:00:00",
              "category": "OTHER"
            }
            """.formatted(professionalId);

        mockMvc.perform(post("/api/v1/calendar-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());
    }

    private String createProfessional() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/professionals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Profesional Calendar\",\"specialty\":\"KINE\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String id = json.get("id").asText();
        Assertions.assertNotNull(id);
        return id;
    }
}
