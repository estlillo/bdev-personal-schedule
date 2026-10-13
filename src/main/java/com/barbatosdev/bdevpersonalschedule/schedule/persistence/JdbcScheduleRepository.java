package com.barbatosdev.bdevpersonalschedule.schedule.persistence;

import com.barbatosdev.bdevpersonalschedule.schedule.domain.ScheduleExceptionEntry;
import com.barbatosdev.bdevpersonalschedule.schedule.domain.WorkingWindow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcScheduleRepository implements ScheduleRepository {

    private final JdbcClient jdbcClient;

    public JdbcScheduleRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<WorkingWindow> findWorkingWindows(UUID professionalId) {
        return jdbcClient.sql("""
                SELECT id, professional_id, day_of_week, start_time, end_time
                FROM working_window
                WHERE professional_id = :professionalId
                ORDER BY day_of_week ASC, start_time ASC
            """)
            .param("professionalId", professionalId.toString())
            .query(this::mapWorkingWindow)
            .list();
    }

    @Override
    public void replaceWorkingWindows(UUID professionalId, List<WorkingWindow> windows) {
        jdbcClient.sql("DELETE FROM working_window WHERE professional_id = :professionalId")
            .param("professionalId", professionalId.toString())
            .update();

        for (WorkingWindow window : windows) {
            jdbcClient.sql("""
                    INSERT INTO working_window (id, professional_id, day_of_week, start_time, end_time)
                    VALUES (:id, :professionalId, :dayOfWeek, :startTime, :endTime)
                """)
                .param("id", window.id().toString())
                .param("professionalId", professionalId.toString())
                .param("dayOfWeek", window.dayOfWeek())
                .param("startTime", window.startTime().toString())
                .param("endTime", window.endTime().toString())
                .update();
        }
    }

    @Override
    public List<ScheduleExceptionEntry> findExceptions(UUID professionalId, LocalDate from, LocalDate to) {
        return jdbcClient.sql("""
                SELECT id, professional_id, date, start_time, end_time, mode, reason
                FROM schedule_exception
                WHERE professional_id = :professionalId
                  AND date >= :fromDate
                  AND date <= :toDate
                ORDER BY date ASC, start_time ASC
            """)
            .param("professionalId", professionalId.toString())
            .param("fromDate", from.toString())
            .param("toDate", to.toString())
            .query(this::mapScheduleException)
            .list();
    }

    @Override
    public ScheduleExceptionEntry saveException(ScheduleExceptionEntry scheduleException) {
        jdbcClient.sql("""
                INSERT INTO schedule_exception (id, professional_id, date, start_time, end_time, mode, reason)
                VALUES (:id, :professionalId, :date, :startTime, :endTime, :mode, :reason)
            """)
            .param("id", scheduleException.id().toString())
            .param("professionalId", scheduleException.professionalId().toString())
            .param("date", scheduleException.date().toString())
            .param("startTime", scheduleException.startTime().toString())
            .param("endTime", scheduleException.endTime().toString())
            .param("mode", scheduleException.mode())
            .param("reason", scheduleException.reason())
            .update();
        return scheduleException;
    }

    @Override
    public boolean deleteException(UUID professionalId, UUID exceptionId) {
        int affectedRows = jdbcClient.sql("""
                DELETE FROM schedule_exception
                WHERE id = :id AND professional_id = :professionalId
            """)
            .param("id", exceptionId.toString())
            .param("professionalId", professionalId.toString())
            .update();
        return affectedRows > 0;
    }

    private WorkingWindow mapWorkingWindow(ResultSet rs, int rowNum) throws SQLException {
        return new WorkingWindow(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("professional_id")),
            rs.getInt("day_of_week"),
            LocalTime.parse(rs.getString("start_time")),
            LocalTime.parse(rs.getString("end_time"))
        );
    }

    private ScheduleExceptionEntry mapScheduleException(ResultSet rs, int rowNum) throws SQLException {
        return new ScheduleExceptionEntry(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("professional_id")),
            LocalDate.parse(rs.getString("date")),
            LocalTime.parse(rs.getString("start_time")),
            LocalTime.parse(rs.getString("end_time")),
            rs.getString("mode"),
            rs.getString("reason")
        );
    }
}

