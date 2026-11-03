package com.barbatosdev.bdevpersonalschedule.availability.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class AvailabilityControllerIntegrationTest {

    private static Path databaseFile;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcClient jdbcClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() throws IOException {
        databaseFile = Files.createTempFile("rehab-scheduler-availability-test-", ".db");
    }

    @AfterAll
    static void afterAll() {
        if (databaseFile != null) {
            try {
                Files.deleteIfExists(databaseFile);
            } catch (IOException ignored) {
                // In Windows the DB file can remain locked briefly during context shutdown.
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
    void searchShouldReturnInstitutionalSlotsApplyingEvents() throws Exception {
        String professionalId = createProfessional();

        mockMvc.perform(put("/api/v1/professionals/{id}/working-windows", professionalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "windows": [
                        {"dayOfWeek":1,"startTime":"08:00","endTime":"12:00"},
                        {"dayOfWeek":1,"startTime":"13:00","endTime":"17:00"}
                      ]
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/professionals/{id}/exceptions", professionalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date":"2026-10-19",
                      "startTime":"10:15",
                      "endTime":"10:30",
                      "mode":"BLOCK"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/professionals/{id}/exceptions", professionalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "date":"2026-10-19",
                      "startTime":"14:00",
                      "endTime":"14:45",
                      "mode":"ADD"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/calendar-events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "professionalId":"%s",
                      "startsAt":"2026-10-19T09:00:00",
                      "endsAt":"2026-10-19T09:45:00",
                      "category":"MEETING"
                    }
                    """.formatted(professionalId)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/availability/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "professionalIds":["%s"],
                      "startDate":"2026-10-19",
                      "endDate":"2026-10-19",
                      "durationMinutes":45,
                      "stepMinutes":45
                    }
                    """.formatted(professionalId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.warning").value("Disponibilidad basada en registros locales; verificar agenda oficial"))
            .andExpect(jsonPath("$.slots", hasSize(org.hamcrest.Matchers.greaterThan(0))))
            .andExpect(jsonPath("$.slots[0].startsAt").value("2026-10-19T08:00:00"))
            .andExpect(jsonPath("$.slots[?(@.startsAt == '2026-10-19T09:00:00')]", hasSize(0)));
    }

    @Test
    void searchShouldRejectInactiveProfessional() throws Exception {
        String professionalId = createProfessional();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/professionals/{id}", professionalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"active\":false" + "}"))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/availability/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "professionalIds":["%s"],
                      "startDate":"2026-10-19",
                      "endDate":"2026-10-19",
                      "durationMinutes":45,
                      "stepMinutes":45
                    }
                    """.formatted(professionalId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String createProfessional() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/professionals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Profesional Disponibilidad\",\"specialty\":\"KINE\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String id = json.get("id").asText();
        Assertions.assertNotNull(id);
        return id;
    }
}
