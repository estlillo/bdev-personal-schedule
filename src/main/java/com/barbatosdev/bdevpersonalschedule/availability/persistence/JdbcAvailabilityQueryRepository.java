package com.barbatosdev.bdevpersonalschedule.availability.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAvailabilityQueryRepository implements AvailabilityQueryRepository {

    private final JdbcClient jdbcClient;

    public JdbcAvailabilityQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<WorkingWindowRow> findWorkingWindows(List<UUID> professionalIds) {
        if (professionalIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("""
                SELECT professional_id, day_of_week, start_time, end_time
                FROM working_window
                WHERE professional_id IN (:professionalIds)
                ORDER BY professional_id ASC, day_of_week ASC, start_time ASC
            """)
            .param("professionalIds", stringifyIds(professionalIds))
            .query(this::mapWorkingWindow)
            .list();
    }

    @Override
    public List<ScheduleExceptionRow> findExceptions(List<UUID> professionalIds, LocalDate from, LocalDate to) {
        if (professionalIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("""
                SELECT professional_id, date, start_time, end_time, mode
                FROM schedule_exception
                WHERE professional_id IN (:professionalIds)
                  AND date >= :fromDate
                  AND date <= :toDate
                ORDER BY professional_id ASC, date ASC, start_time ASC
            """)
            .param("professionalIds", stringifyIds(professionalIds))
            .param("fromDate", from.toString())
            .param("toDate", to.toString())
            .query(this::mapException)
            .list();
    }

    @Override
    public List<CalendarEventRow> findCalendarEvents(List<UUID> professionalIds, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        if (professionalIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("""
                SELECT professional_id, starts_at, ends_at
                FROM calendar_event
                WHERE professional_id IN (:professionalIds)
                  AND starts_at < :toExclusive
                  AND ends_at > :fromInclusive
                ORDER BY professional_id ASC, starts_at ASC
            """)
            .param("professionalIds", stringifyIds(professionalIds))
            .param("fromInclusive", fromInclusive.toString())
            .param("toExclusive", toExclusive.toString())
            .query(this::mapEvent)
            .list();
    }

    @Override
    public List<ReservationRow> findActiveReservations(List<UUID> professionalIds, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        if (professionalIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("""
                SELECT rp.professional_id, r.starts_at, r.ends_at
                FROM reservation r
                JOIN reservation_professional rp ON rp.reservation_id = r.id
                WHERE rp.professional_id IN (:professionalIds)
                  AND r.status IN ('HOLD', 'CONFIRMED')
                  AND r.starts_at < :toExclusive
                  AND r.ends_at > :fromInclusive
                ORDER BY rp.professional_id ASC, r.starts_at ASC
            """)
            .param("professionalIds", stringifyIds(professionalIds))
            .param("fromInclusive", fromInclusive.toString())
            .param("toExclusive", toExclusive.toString())
            .query(this::mapReservation)
            .list();
    }

    private List<String> stringifyIds(List<UUID> professionalIds) {
        return professionalIds.stream().map(UUID::toString).toList();
    }

    private WorkingWindowRow mapWorkingWindow(ResultSet rs, int rowNum) throws SQLException {
        return new WorkingWindowRow(
            UUID.fromString(rs.getString("professional_id")),
            rs.getInt("day_of_week"),
            LocalTime.parse(rs.getString("start_time")),
            LocalTime.parse(rs.getString("end_time"))
        );
    }

    private ScheduleExceptionRow mapException(ResultSet rs, int rowNum) throws SQLException {
        return new ScheduleExceptionRow(
            UUID.fromString(rs.getString("professional_id")),
            LocalDate.parse(rs.getString("date")),
            LocalTime.parse(rs.getString("start_time")),
            LocalTime.parse(rs.getString("end_time")),
            rs.getString("mode")
        );
    }

    private CalendarEventRow mapEvent(ResultSet rs, int rowNum) throws SQLException {
        return new CalendarEventRow(
            UUID.fromString(rs.getString("professional_id")),
            LocalDateTime.parse(rs.getString("starts_at")),
            LocalDateTime.parse(rs.getString("ends_at"))
        );
    }

    private ReservationRow mapReservation(ResultSet rs, int rowNum) throws SQLException {
        return new ReservationRow(
            UUID.fromString(rs.getString("professional_id")),
            LocalDateTime.parse(rs.getString("starts_at")),
            LocalDateTime.parse(rs.getString("ends_at"))
        );
    }
}
