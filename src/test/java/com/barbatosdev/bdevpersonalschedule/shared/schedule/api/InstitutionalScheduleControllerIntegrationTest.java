package com.barbatosdev.bdevpersonalschedule.shared.schedule.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class InstitutionalScheduleControllerIntegrationTest {

    private static Path databaseFile;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcClient jdbcClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() throws IOException {
        databaseFile = Files.createTempFile("rehab-scheduler-institutional-test-", ".db");
    }

    @AfterAll
    static void afterAll() {
        if (databaseFile != null) {
            try {
                Files.deleteIfExists(databaseFile);
            } catch (IOException ignored) {
                // SQLite file may remain locked briefly on Windows.
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
        jdbcClient.sql("DELETE FROM institutional_block_rule").update();
    }

    @Test
    void getAndUpdateScheduleShouldWork() throws Exception {
        mockMvc.perform(get("/api/v1/institutional-schedule"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dayStart").value("08:00:00"))
            .andExpect(jsonPath("$.dayEnd").value("17:00:00"))
            .andExpect(jsonPath("$.slotMinutes").value(45));

        mockMvc.perform(put("/api/v1/institutional-schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "dayStart":"07:30:00",
                      "dayEnd":"16:30:00",
                      "slotMinutes":30
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dayStart").value("07:30:00"))
            .andExpect(jsonPath("$.slotMinutes").value(30));
    }

    @Test
    void createPatchAndDeleteBlockShouldWork() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/institutional-schedule/blocks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Reunion general",
                      "category":"MEETING",
                      "ruleType":"DATE_RANGE",
                      "startTime":"11:15:00",
                      "endTime":"12:00:00",
                      "startDate":"2026-10-20",
                      "endDate":"2026-10-22",
                      "active":true
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Reunion general"))
            .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String blockId = body.get("id").asText();
        Assertions.assertNotNull(blockId);

        mockMvc.perform(get("/api/v1/institutional-schedule/blocks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(org.hamcrest.Matchers.greaterThan(0))));

        mockMvc.perform(patch("/api/v1/institutional-schedule/blocks/{blockId}", blockId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Reunion general ajustada",
                      "category":"MEETING",
                      "ruleType":"DATE_RANGE",
                      "startTime":"11:15:00",
                      "endTime":"12:00:00",
                      "startDate":"2026-10-20",
                      "endDate":"2026-10-22",
                      "active":false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Reunion general ajustada"))
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/v1/institutional-schedule/blocks/{blockId}", blockId))
            .andExpect(status().isNoContent());
    }
}

