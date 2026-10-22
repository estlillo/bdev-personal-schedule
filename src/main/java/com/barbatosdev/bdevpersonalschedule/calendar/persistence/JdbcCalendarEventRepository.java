package com.barbatosdev.bdevpersonalschedule.calendar.persistence;

import com.barbatosdev.bdevpersonalschedule.calendar.domain.CalendarEvent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCalendarEventRepository implements CalendarEventRepository {

    private final JdbcClient jdbcClient;

    public JdbcCalendarEventRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<CalendarEvent> findByProfessionalAndRange(UUID professionalId, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        return jdbcClient.sql("""
                SELECT id, professional_id, starts_at, ends_at, category, note
                FROM calendar_event
                WHERE professional_id = :professionalId
                  AND starts_at < :toExclusive
                  AND ends_at > :fromInclusive
                ORDER BY starts_at ASC
            """)
            .param("professionalId", professionalId.toString())
            .param("fromInclusive", fromInclusive.toString())
            .param("toExclusive", toExclusive.toString())
            .query(this::mapRow)
            .list();
    }

    @Override
    public Optional<CalendarEvent> findById(UUID id) {
        return jdbcClient.sql("""
                SELECT id, professional_id, starts_at, ends_at, category, note
                FROM calendar_event
                WHERE id = :id
            """)
            .param("id", id.toString())
            .query(this::mapRow)
            .optional();
    }

    @Override
    public CalendarEvent save(CalendarEvent calendarEvent) {
        jdbcClient.sql("""
                INSERT INTO calendar_event (id, professional_id, starts_at, ends_at, category, note)
                VALUES (:id, :professionalId, :startsAt, :endsAt, :category, :note)
            """)
            .param("id", calendarEvent.id().toString())
            .param("professionalId", calendarEvent.professionalId().toString())
            .param("startsAt", calendarEvent.startsAt().toString())
            .param("endsAt", calendarEvent.endsAt().toString())
            .param("category", calendarEvent.category())
            .param("note", calendarEvent.note())
            .update();
        return calendarEvent;
    }

    @Override
    public CalendarEvent update(CalendarEvent calendarEvent) {
        jdbcClient.sql("""
                UPDATE calendar_event
                SET professional_id = :professionalId,
                    starts_at = :startsAt,
                    ends_at = :endsAt,
                    category = :category,
                    note = :note
                WHERE id = :id
            """)
            .param("id", calendarEvent.id().toString())
            .param("professionalId", calendarEvent.professionalId().toString())
            .param("startsAt", calendarEvent.startsAt().toString())
            .param("endsAt", calendarEvent.endsAt().toString())
            .param("category", calendarEvent.category())
            .param("note", calendarEvent.note())
            .update();
        return calendarEvent;
    }

    @Override
    public boolean deleteById(UUID id) {
        int affectedRows = jdbcClient.sql("DELETE FROM calendar_event WHERE id = :id")
            .param("id", id.toString())
            .update();
        return affectedRows > 0;
    }

    @Override
    public boolean hasOverlap(UUID professionalId, LocalDateTime startsAt, LocalDateTime endsAt, UUID excludedEventId) {
        Integer count;
        if (excludedEventId == null) {
            count = jdbcClient.sql("""
                    SELECT COUNT(1)
                    FROM calendar_event
                    WHERE professional_id = :professionalId
                      AND starts_at < :endsAt
                      AND :startsAt < ends_at
                """)
                .param("professionalId", professionalId.toString())
                .param("startsAt", startsAt.toString())
                .param("endsAt", endsAt.toString())
                .query(Integer.class)
                .single();
        } else {
            count = jdbcClient.sql("""
                    SELECT COUNT(1)
                    FROM calendar_event
                    WHERE professional_id = :professionalId
                      AND starts_at < :endsAt
                      AND :startsAt < ends_at
                      AND id <> :excludedEventId
                """)
                .param("professionalId", professionalId.toString())
                .param("startsAt", startsAt.toString())
                .param("endsAt", endsAt.toString())
                .param("excludedEventId", excludedEventId.toString())
                .query(Integer.class)
                .single();
        }

        return count > 0;
    }

    private CalendarEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new CalendarEvent(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("professional_id")),
            LocalDateTime.parse(rs.getString("starts_at")),
            LocalDateTime.parse(rs.getString("ends_at")),
            rs.getString("category"),
            rs.getString("note")
        );
    }
}

