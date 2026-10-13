package com.barbatosdev.bdevpersonalschedule.schedule.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
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
class ProfessionalScheduleControllerIntegrationTest {

    private static Path databaseFile;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcClient jdbcClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() throws IOException {
        databaseFile = Files.createTempFile("rehab-scheduler-phase2-test-", ".db");
    }

    @AfterAll
    static void afterAll() throws IOException {
        if (databaseFile != null) {
            Files.deleteIfExists(databaseFile);
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + databaseFile.toAbsolutePath());
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
    }

    @BeforeEach
    void cleanData() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        jdbcClient.sql("DELETE FROM schedule_exception").update();
        jdbcClient.sql("DELETE FROM working_window").update();
        jdbcClient.sql("DELETE FROM professional").update();
    }

    @Test
    void replaceAndGetWorkingWindowsShouldWork() throws Exception {
        String professionalId = createProfessional();

        String body = """
            {
              "windows": [
                {"dayOfWeek": 1, "startTime": "08:00", "endTime": "12:00"},
                {"dayOfWeek": 1, "startTime": "12:00", "endTime": "17:00"}
              ]
            }
            """;

        mockMvc.perform(put("/api/v1/professionals/{id}/working-windows", professionalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.windows", hasSize(2)));

        mockMvc.perform(get("/api/v1/professionals/{id}/working-windows", professionalId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.windows", hasSize(2)))
            .andExpect(jsonPath("$.windows[0].dayOfWeek").value(1));
    }

    @Test
    void replaceWorkingWindowsShouldRejectOverlaps() throws Exception {
        String professionalId = createProfessional();

        String body = """
            {
              "windows": [
                {"dayOfWeek": 1, "startTime": "08:00", "endTime": "12:30"},
                {"dayOfWeek": 1, "startTime": "12:00", "endTime": "17:00"}
              ]
            }
            """;

        mockMvc.perform(put("/api/v1/professionals/{id}/working-windows", professionalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void exceptionsCrudShouldWork() throws Exception {
        String professionalId = createProfessional();

        String createBody = """
            {
              "date": "2026-10-16",
              "startTime": "08:00",
              "endTime": "12:00",
              "mode": "BLOCK",
              "reason": "Permiso"
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/professionals/{id}/exceptions", professionalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String exceptionId = createJson.get("id").asText();

        mockMvc.perform(get("/api/v1/professionals/{id}/exceptions", professionalId)
                .param("from", "2026-10-12")
                .param("to", "2026-10-18"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/v1/professionals/{id}/exceptions/{exceptionId}", professionalId, exceptionId))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/professionals/{id}/exceptions", professionalId)
                .param("from", "2026-10-12")
                .param("to", "2026-10-18"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getWorkingWindowsShouldReturnNotFoundWhenProfessionalDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/professionals/{id}/working-windows", UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private String createProfessional() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/professionals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Profesional Fase2\",\"specialty\":\"KINE\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String id = json.get("id").asText();
        Assertions.assertNotNull(id);
        return id;
    }
}

