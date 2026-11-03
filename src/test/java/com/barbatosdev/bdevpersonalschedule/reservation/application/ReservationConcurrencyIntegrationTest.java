package com.barbatosdev.bdevpersonalschedule.reservation.application;

import com.barbatosdev.bdevpersonalschedule.professional.api.dto.CreateProfessionalRequest;
import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
import com.barbatosdev.bdevpersonalschedule.professional.persistence.ProfessionalRepository;
import com.barbatosdev.bdevpersonalschedule.schedule.domain.WorkingWindow;
import com.barbatosdev.bdevpersonalschedule.schedule.persistence.ScheduleRepository;
import com.barbatosdev.bdevpersonalschedule.shared.error.ScheduleConflictException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class ReservationConcurrencyIntegrationTest {

    private static Path databaseFile;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ProfessionalRepository professionalRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeAll
    static void beforeAll() throws IOException {
        databaseFile = Files.createTempFile("rehab-scheduler-reservation-concurrency-test-", ".db");
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
    void cleanData() {
        jdbcClient.sql("DELETE FROM reservation_professional").update();
        jdbcClient.sql("DELETE FROM reservation").update();
        jdbcClient.sql("DELETE FROM calendar_event").update();
        jdbcClient.sql("DELETE FROM schedule_exception").update();
        jdbcClient.sql("DELETE FROM working_window").update();
        jdbcClient.sql("DELETE FROM professional").update();
    }

    @Test
    void concurrentCreateForSameSlotShouldAllowOnlyOneSuccess() throws Exception {
        Professional professional = professionalRepository.save(new Professional(
            UUID.randomUUID(),
            new CreateProfessionalRequest("Profesional Concurrencia", "KINE").displayName(),
            "KINE",
            true,
            LocalDateTime.parse("2026-10-12T10:00:00"),
            LocalDateTime.parse("2026-10-12T10:00:00")
        ));

        scheduleRepository.replaceWorkingWindows(professional.id(), List.of(
            new WorkingWindow(UUID.randomUUID(), professional.id(), 1, LocalTime.parse("08:00"), LocalTime.parse("17:00"))
        ));

        CreateReservationCommand command = new CreateReservationCommand(
            List.of(professional.id()),
            LocalDateTime.parse("2026-10-19T10:00:00"),
            LocalDateTime.parse("2026-10-19T10:45:00"),
            "CASO-CONC",
            "Reserva concurrente"
        );

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> task = () -> {
            latch.await(5, TimeUnit.SECONDS);
            try {
                reservationService.create(command);
                return true;
            } catch (ScheduleConflictException ex) {
                return false;
            }
        };

        Future<Boolean> f1 = executor.submit(task);
        Future<Boolean> f2 = executor.submit(task);

        latch.countDown();
        boolean r1 = f1.get(10, TimeUnit.SECONDS);
        boolean r2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        int successCount = (r1 ? 1 : 0) + (r2 ? 1 : 0);
        Assertions.assertEquals(1, successCount);

        Integer reservationCount = jdbcClient.sql("SELECT COUNT(1) FROM reservation").query(Integer.class).single();
        Assertions.assertEquals(1, reservationCount);
    }
}

