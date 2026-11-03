package com.barbatosdev.bdevpersonalschedule.reservation.api;

import static org.hamcrest.Matchers.hasSize;
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
class ReservationControllerIntegrationTest {

    private static Path databaseFile;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcClient jdbcClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() throws IOException {
        databaseFile = Files.createTempFile("rehab-scheduler-reservation-test-", ".db");
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
        jdbcClient.sql("DELETE FROM reservation_professional").update();
        jdbcClient.sql("DELETE FROM reservation").update();
        jdbcClient.sql("DELETE FROM calendar_event").update();
        jdbcClient.sql("DELETE FROM schedule_exception").update();
        jdbcClient.sql("DELETE FROM working_window").update();
        jdbcClient.sql("DELETE FROM professional").update();
    }

    @Test
    void createAndListAndPatchStatusShouldWork() throws Exception {
        String professionalId = createProfessional("Profesional Reserva");
        configureFullDayWindow(professionalId);

        MvcResult created = mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "professionalIds":["%s"],
                      "startsAt":"2026-10-19T08:00:00",
                      "endsAt":"2026-10-19T08:45:00",
                      "referenceCode":"CASO-001",
                      "description":"Sesion de movilidad"
                    }
                    """.formatted(professionalId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("HOLD"))
            .andExpect(jsonPath("$.description").value("Sesion de movilidad"))
            .andReturn();

        String reservationId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/reservations")
                .param("from", "2026-10-19")
                .param("to", "2026-10-19")
                .param("status", "HOLD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch("/api/v1/reservations/{id}/status", reservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void createShouldReturnConflictWhenReservationOverlaps() throws Exception {
        String professionalId = createProfessional("Profesional Solape");
        configureFullDayWindow(professionalId);

        mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "professionalIds":["%s"],
                      "startsAt":"2026-10-19T09:00:00",
                      "endsAt":"2026-10-19T09:45:00",
                      "referenceCode":"CASO-001",
                      "description":"Bloque base"
                    }
                    """.formatted(professionalId)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "professionalIds":["%s"],
                      "startsAt":"2026-10-19T09:30:00",
                      "endsAt":"2026-10-19T10:00:00",
                      "referenceCode":"CASO-002",
                      "description":"Bloque en conflicto"
                    }
                    """.formatted(professionalId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"));
    }

    @Test
    void createShouldRejectOutsideWorkingHours() throws Exception {
        String professionalId = createProfessional("Profesional Horario");
        configureMorningWindow(professionalId);

        mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "professionalIds":["%s"],
                      "startsAt":"2026-10-19T14:00:00",
                      "endsAt":"2026-10-19T14:30:00",
                      "referenceCode":"CASO-003",
                      "description":"Fuera de horario"
                    }
                    """.formatted(professionalId)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("OUTSIDE_WORKING_HOURS"));
    }

    @Test
    void patchStatusShouldRejectInvalidTransition() throws Exception {
        String professionalId = createProfessional("Profesional Estado");
        configureFullDayWindow(professionalId);

        String reservationId = createReservation(professionalId, "2026-10-19T11:00:00", "2026-10-19T11:45:00");

        mockMvc.perform(patch("/api/v1/reservations/{id}/status", reservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/reservations/{id}/status", reservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONFIRMED\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void patchMetadataShouldUpdateReferenceCodeAndDescription() throws Exception {
        String professionalId = createProfessional("Profesional Edicion");
        configureFullDayWindow(professionalId);
        String reservationId = createReservation(professionalId, "2026-10-19T10:00:00", "2026-10-19T10:45:00");

        mockMvc.perform(patch("/api/v1/reservations/{id}", reservationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "referenceCode":"CASO-EDITADO",
                      "description":"Descripcion actualizada"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.referenceCode").value("CASO-EDITADO"))
            .andExpect(jsonPath("$.description").value("Descripcion actualizada"));
    }

    private String createProfessional(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/professionals")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"" + name + "\",\"specialty\":\"KINE\"}"))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String id = json.get("id").asText();
        Assertions.assertNotNull(id);
        return id;
    }

    private void configureMorningWindow(String professionalId) throws Exception {
        mockMvc.perform(put("/api/v1/professionals/{id}/working-windows", professionalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "windows": [
                        {"dayOfWeek":1,"startTime":"08:00","endTime":"12:00"}
                      ]
                    }
                    """))
            .andExpect(status().isOk());
    }

    private void configureFullDayWindow(String professionalId) throws Exception {
        mockMvc.perform(put("/api/v1/professionals/{id}/working-windows", professionalId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "windows": [
                        {"dayOfWeek":1,"startTime":"08:00","endTime":"17:00"}
                      ]
                    }
                    """))
            .andExpect(status().isOk());
    }

    private String createReservation(String professionalId, String startsAt, String endsAt) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "professionalIds":["%s"],
                      "startsAt":"%s",
                      "endsAt":"%s",
                      "referenceCode":"CASO-X",
                      "description":"Reserva de prueba"
                    }
                    """.formatted(professionalId, startsAt, endsAt)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}

