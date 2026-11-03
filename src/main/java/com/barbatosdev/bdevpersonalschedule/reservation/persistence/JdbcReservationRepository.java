package com.barbatosdev.bdevpersonalschedule.reservation.persistence;

import com.barbatosdev.bdevpersonalschedule.reservation.domain.Reservation;
import com.barbatosdev.bdevpersonalschedule.reservation.domain.ReservationStatus;
import com.barbatosdev.bdevpersonalschedule.shared.error.NotFoundException;
import com.barbatosdev.bdevpersonalschedule.shared.error.ScheduleConflictException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcReservationRepository implements ReservationRepository {

    private final JdbcClient jdbcClient;
    private final DataSource dataSource;

    public JdbcReservationRepository(JdbcClient jdbcClient, DataSource dataSource) {
        this.jdbcClient = jdbcClient;
        this.dataSource = dataSource;
    }

    @Override
    public List<Reservation> findByRange(LocalDate from, LocalDate to, ReservationStatus status) {
        LocalDateTime fromInclusive = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        List<ReservationRow> rows;
        if (status == null) {
            rows = jdbcClient.sql("""
                    SELECT id, starts_at, ends_at, status, reference_code, description, created_at, updated_at
                    FROM reservation
                    WHERE starts_at < :toExclusive
                      AND ends_at > :fromInclusive
                    ORDER BY starts_at ASC
                """)
                .param("fromInclusive", fromInclusive.toString())
                .param("toExclusive", toExclusive.toString())
                .query(this::mapReservationRow)
                .list();
        } else {
            rows = jdbcClient.sql("""
                    SELECT id, starts_at, ends_at, status, reference_code, description, created_at, updated_at
                    FROM reservation
                    WHERE starts_at < :toExclusive
                      AND ends_at > :fromInclusive
                      AND status = :status
                    ORDER BY starts_at ASC
                """)
                .param("fromInclusive", fromInclusive.toString())
                .param("toExclusive", toExclusive.toString())
                .param("status", status.name())
                .query(this::mapReservationRow)
                .list();
        }

        return toReservations(rows);
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        Optional<ReservationRow> row = jdbcClient.sql("""
                SELECT id, starts_at, ends_at, status, reference_code, description, created_at, updated_at
                FROM reservation
                WHERE id = :id
            """)
            .param("id", id.toString())
            .query(this::mapReservationRow)
            .optional();

        if (row.isEmpty()) {
            return Optional.empty();
        }

        Map<UUID, List<UUID>> professionalsByReservation = loadProfessionalIds(List.of(row.get().id()));
        return Optional.of(toReservation(row.get(), professionalsByReservation.getOrDefault(row.get().id(), List.of())));
    }

    @Override
    public Reservation createAtomic(Reservation reservation) {
        try (Connection connection = dataSource.getConnection()) {
            beginImmediate(connection);
            try {
                for (UUID professionalId : reservation.professionalIds()) {
                    if (hasCalendarEventConflict(connection, professionalId, reservation.startsAt(), reservation.endsAt())
                        || hasActiveReservationConflict(connection, professionalId, reservation.startsAt(), reservation.endsAt())) {
                        executeSql(connection, "ROLLBACK");
                        throw new ScheduleConflictException("Reservation interval conflicts with existing schedule");
                    }
                }

                insertReservation(connection, reservation);
                insertReservationProfessionals(connection, reservation);
                executeSql(connection, "COMMIT");
                return reservation;
            } catch (RuntimeException ex) {
                rollbackQuietly(connection);
                throw ex;
            } catch (SQLException ex) {
                rollbackQuietly(connection);
                throw new IllegalStateException("Failed to create reservation", ex);
            }
        } catch (SQLException ex) {
            if (isDatabaseLocked(ex)) {
                throw new ScheduleConflictException("Reservation interval conflicts with concurrent write operation");
            }
            throw new IllegalStateException("Failed to create reservation", ex);
        }
    }

    @Override
    public Reservation updateStatus(UUID id, ReservationStatus status, LocalDateTime updatedAt) {
        int updatedRows = jdbcClient.sql("""
                UPDATE reservation
                SET status = :status,
                    updated_at = :updatedAt
                WHERE id = :id
            """)
            .param("id", id.toString())
            .param("status", status.name())
            .param("updatedAt", updatedAt.toString())
            .update();

        if (updatedRows == 0) {
            throw new NotFoundException("Reservation not found: " + id);
        }

        return findById(id).orElseThrow(() -> new NotFoundException("Reservation not found: " + id));
    }

    @Override
    public Reservation updateMetadata(UUID id, String referenceCode, String description, LocalDateTime updatedAt) {
        int updatedRows = jdbcClient.sql("""
                UPDATE reservation
                SET reference_code = :referenceCode,
                    description = :description,
                    updated_at = :updatedAt
                WHERE id = :id
            """)
            .param("id", id.toString())
            .param("referenceCode", referenceCode)
            .param("description", description)
            .param("updatedAt", updatedAt.toString())
            .update();

        if (updatedRows == 0) {
            throw new NotFoundException("Reservation not found: " + id);
        }

        return findById(id).orElseThrow(() -> new NotFoundException("Reservation not found: " + id));
    }

    private List<Reservation> toReservations(List<ReservationRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = rows.stream().map(ReservationRow::id).toList();
        Map<UUID, List<UUID>> professionalsByReservation = loadProfessionalIds(ids);

        List<Reservation> result = new ArrayList<>();
        for (ReservationRow row : rows) {
            result.add(toReservation(row, professionalsByReservation.getOrDefault(row.id(), List.of())));
        }
        return result;
    }

    private Map<UUID, List<UUID>> loadProfessionalIds(List<UUID> reservationIds) {
        if (reservationIds.isEmpty()) {
            return Map.of();
        }

        List<ReservationProfessionalRow> rows = jdbcClient.sql("""
                SELECT reservation_id, professional_id
                FROM reservation_professional
                WHERE reservation_id IN (:ids)
                ORDER BY reservation_id ASC
            """)
            .param("ids", reservationIds.stream().map(UUID::toString).toList())
            .query((rs, rowNum) -> new ReservationProfessionalRow(
                UUID.fromString(rs.getString("reservation_id")),
                UUID.fromString(rs.getString("professional_id"))
            ))
            .list();

        Map<UUID, List<UUID>> map = new HashMap<>();
        for (ReservationProfessionalRow row : rows) {
            map.computeIfAbsent(row.reservationId(), key -> new ArrayList<>()).add(row.professionalId());
        }
        return map;
    }

    private Reservation toReservation(ReservationRow row, List<UUID> professionalIds) {
        return new Reservation(
            row.id(),
            row.startsAt(),
            row.endsAt(),
            row.status(),
            row.referenceCode(),
            row.description(),
            row.createdAt(),
            row.updatedAt(),
            professionalIds
        );
    }

    private ReservationRow mapReservationRow(ResultSet rs, int rowNum) throws SQLException {
        return new ReservationRow(
            UUID.fromString(rs.getString("id")),
            LocalDateTime.parse(rs.getString("starts_at")),
            LocalDateTime.parse(rs.getString("ends_at")),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getString("reference_code"),
            rs.getString("description"),
            LocalDateTime.parse(rs.getString("created_at")),
            LocalDateTime.parse(rs.getString("updated_at"))
        );
    }

    private void beginImmediate(Connection connection) throws SQLException {
        executeSql(connection, "BEGIN IMMEDIATE");
    }

    private void executeSql(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            executeSql(connection, "ROLLBACK");
        } catch (SQLException ignored) {
            // Best effort rollback.
        }
    }

    private boolean hasCalendarEventConflict(
        Connection connection,
        UUID professionalId,
        LocalDateTime startsAt,
        LocalDateTime endsAt
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(1)
                FROM calendar_event
                WHERE professional_id = ?
                  AND starts_at < ?
                  AND ? < ends_at
            """)) {
            statement.setString(1, professionalId.toString());
            statement.setString(2, endsAt.toString());
            statement.setString(3, startsAt.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean hasActiveReservationConflict(
        Connection connection,
        UUID professionalId,
        LocalDateTime startsAt,
        LocalDateTime endsAt
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(1)
                FROM reservation r
                JOIN reservation_professional rp ON rp.reservation_id = r.id
                WHERE rp.professional_id = ?
                  AND r.status IN ('HOLD', 'CONFIRMED')
                  AND r.starts_at < ?
                  AND ? < r.ends_at
            """)) {
            statement.setString(1, professionalId.toString());
            statement.setString(2, endsAt.toString());
            statement.setString(3, startsAt.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void insertReservation(Connection connection, Reservation reservation) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO reservation (id, starts_at, ends_at, status, reference_code, description, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
            statement.setString(1, reservation.id().toString());
            statement.setString(2, reservation.startsAt().toString());
            statement.setString(3, reservation.endsAt().toString());
            statement.setString(4, reservation.status().name());
            statement.setString(5, reservation.referenceCode());
            statement.setString(6, reservation.description());
            statement.setString(7, reservation.createdAt().toString());
            statement.setString(8, reservation.updatedAt().toString());
            statement.executeUpdate();
        }
    }

    private void insertReservationProfessionals(Connection connection, Reservation reservation) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO reservation_professional (reservation_id, professional_id)
                VALUES (?, ?)
            """)) {
            for (UUID professionalId : reservation.professionalIds()) {
                statement.setString(1, reservation.id().toString());
                statement.setString(2, professionalId.toString());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private boolean isDatabaseLocked(SQLException ex) {
        String message = ex.getMessage();
        return message != null && message.toLowerCase().contains("database is locked");
    }

    private record ReservationRow(
        UUID id,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        ReservationStatus status,
        String referenceCode,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    private record ReservationProfessionalRow(UUID reservationId, UUID professionalId) {
    }
}
