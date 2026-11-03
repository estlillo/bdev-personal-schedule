package com.barbatosdev.bdevpersonalschedule.shared.schedule;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInstitutionalScheduleRepository implements InstitutionalScheduleRepository {

    private static final DateTimeFormatter SQLITE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcClient jdbcClient;

    public JdbcInstitutionalScheduleRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<InstitutionalScheduleConfig> findScheduleConfig() {
        return jdbcClient.sql("""
                SELECT day_start, day_end, slot_minutes, updated_at
                FROM institutional_schedule_config
                WHERE id = 1
            """)
            .query(this::mapScheduleConfig)
            .optional();
    }

    @Override
    public InstitutionalScheduleConfig saveScheduleConfig(InstitutionalScheduleConfig config) {
        jdbcClient.sql("""
                INSERT INTO institutional_schedule_config (id, day_start, day_end, slot_minutes, updated_at)
                VALUES (1, :dayStart, :dayEnd, :slotMinutes, :updatedAt)
                ON CONFLICT(id) DO UPDATE SET
                    day_start = excluded.day_start,
                    day_end = excluded.day_end,
                    slot_minutes = excluded.slot_minutes,
                    updated_at = excluded.updated_at
            """)
            .param("dayStart", config.dayStart().toString())
            .param("dayEnd", config.dayEnd().toString())
            .param("slotMinutes", config.slotMinutes())
            .param("updatedAt", config.updatedAt().toString())
            .update();

        return findScheduleConfig().orElseThrow(() -> new IllegalStateException("Schedule config not found after save"));
    }

    @Override
    public List<InstitutionalBlockRule> findBlockRules(Boolean active) {
        if (active == null) {
            return jdbcClient.sql("""
                    SELECT id, name, category, rule_type, day_of_week, start_time, end_time,
                           start_date, end_date, active, created_at, updated_at
                    FROM institutional_block_rule
                    ORDER BY rule_type ASC, day_of_week ASC, start_time ASC, name ASC
                """)
                .query(this::mapBlockRule)
                .list();
        }

        return jdbcClient.sql("""
                SELECT id, name, category, rule_type, day_of_week, start_time, end_time,
                       start_date, end_date, active, created_at, updated_at
                FROM institutional_block_rule
                WHERE active = :active
                ORDER BY rule_type ASC, day_of_week ASC, start_time ASC, name ASC
            """)
            .param("active", active ? 1 : 0)
            .query(this::mapBlockRule)
            .list();
    }

    @Override
    public Optional<InstitutionalBlockRule> findBlockRuleById(UUID ruleId) {
        return jdbcClient.sql("""
                SELECT id, name, category, rule_type, day_of_week, start_time, end_time,
                       start_date, end_date, active, created_at, updated_at
                FROM institutional_block_rule
                WHERE id = :id
            """)
            .param("id", ruleId.toString())
            .query(this::mapBlockRule)
            .optional();
    }

    @Override
    public InstitutionalBlockRule saveBlockRule(InstitutionalBlockRule rule) {
        jdbcClient.sql("""
                INSERT INTO institutional_block_rule (
                    id, name, category, rule_type, day_of_week, start_time, end_time,
                    start_date, end_date, active, created_at, updated_at
                )
                VALUES (
                    :id, :name, :category, :ruleType, :dayOfWeek, :startTime, :endTime,
                    :startDate, :endDate, :active, :createdAt, :updatedAt
                )
            """)
            .param("id", rule.id().toString())
            .param("name", rule.name())
            .param("category", rule.category())
            .param("ruleType", rule.ruleType().name())
            .param("dayOfWeek", rule.dayOfWeek())
            .param("startTime", rule.startTime().toString())
            .param("endTime", rule.endTime().toString())
            .param("startDate", rule.startDate() != null ? rule.startDate().toString() : null)
            .param("endDate", rule.endDate() != null ? rule.endDate().toString() : null)
            .param("active", rule.active() ? 1 : 0)
            .param("createdAt", rule.createdAt().toString())
            .param("updatedAt", rule.updatedAt().toString())
            .update();

        return findBlockRuleById(rule.id()).orElseThrow(() -> new IllegalStateException("Block rule not found after save"));
    }

    @Override
    public InstitutionalBlockRule updateBlockRule(InstitutionalBlockRule rule) {
        int updatedRows = jdbcClient.sql("""
                UPDATE institutional_block_rule
                SET name = :name,
                    category = :category,
                    rule_type = :ruleType,
                    day_of_week = :dayOfWeek,
                    start_time = :startTime,
                    end_time = :endTime,
                    start_date = :startDate,
                    end_date = :endDate,
                    active = :active,
                    updated_at = :updatedAt
                WHERE id = :id
            """)
            .param("id", rule.id().toString())
            .param("name", rule.name())
            .param("category", rule.category())
            .param("ruleType", rule.ruleType().name())
            .param("dayOfWeek", rule.dayOfWeek())
            .param("startTime", rule.startTime().toString())
            .param("endTime", rule.endTime().toString())
            .param("startDate", rule.startDate() != null ? rule.startDate().toString() : null)
            .param("endDate", rule.endDate() != null ? rule.endDate().toString() : null)
            .param("active", rule.active() ? 1 : 0)
            .param("updatedAt", rule.updatedAt().toString())
            .update();

        if (updatedRows == 0) {
            throw new com.barbatosdev.bdevpersonalschedule.shared.error.NotFoundException("Institutional block rule not found: " + rule.id());
        }

        return findBlockRuleById(rule.id()).orElseThrow(() -> new IllegalStateException("Block rule not found after update"));
    }

    @Override
    public boolean deleteBlockRule(UUID ruleId) {
        int affectedRows = jdbcClient.sql("DELETE FROM institutional_block_rule WHERE id = :id")
            .param("id", ruleId.toString())
            .update();
        return affectedRows > 0;
    }

    private InstitutionalScheduleConfig mapScheduleConfig(ResultSet rs, int rowNum) throws SQLException {
        return new InstitutionalScheduleConfig(
            LocalTime.parse(rs.getString("day_start")),
            LocalTime.parse(rs.getString("day_end")),
            rs.getInt("slot_minutes"),
            parseDateTime(rs.getString("updated_at"))
        );
    }

    private InstitutionalBlockRule mapBlockRule(ResultSet rs, int rowNum) throws SQLException {
        String startDateRaw = rs.getString("start_date");
        String endDateRaw = rs.getString("end_date");

        return new InstitutionalBlockRule(
            UUID.fromString(rs.getString("id")),
            rs.getString("name"),
            rs.getString("category"),
            InstitutionalRuleType.fromValue(rs.getString("rule_type")),
            (Integer) rs.getObject("day_of_week"),
            LocalTime.parse(rs.getString("start_time")),
            LocalTime.parse(rs.getString("end_time")),
            startDateRaw != null ? LocalDate.parse(startDateRaw) : null,
            endDateRaw != null ? LocalDate.parse(endDateRaw) : null,
            rs.getInt("active") == 1,
            parseDateTime(rs.getString("created_at")),
            parseDateTime(rs.getString("updated_at"))
        );
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null) {
            throw new IllegalStateException("DateTime value is required");
        }
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }
        return LocalDateTime.parse(value, SQLITE_TIMESTAMP);
    }
}

