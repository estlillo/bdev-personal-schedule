package com.barbatosdev.bdevpersonalschedule.professional.persistence;

import com.barbatosdev.bdevpersonalschedule.professional.domain.Professional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProfessionalRepository implements ProfessionalRepository {

    private final JdbcClient jdbcClient;

    public JdbcProfessionalRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<Professional> findAll(Boolean active) {
        if (active == null) {
            return jdbcClient.sql("""
                    SELECT id, display_name, specialty, active, created_at, updated_at
                    FROM professional
                    ORDER BY created_at ASC
                """)
                .query(this::mapRow)
                .list();
        }

        return jdbcClient.sql("""
                SELECT id, display_name, specialty, active, created_at, updated_at
                FROM professional
                WHERE active = :active
                ORDER BY created_at ASC
            """)
            .param("active", active)
            .query(this::mapRow)
            .list();
    }

    @Override
    public Optional<Professional> findById(UUID id) {
        return jdbcClient.sql("""
                SELECT id, display_name, specialty, active, created_at, updated_at
                FROM professional
                WHERE id = :id
            """)
            .param("id", id.toString())
            .query(this::mapRow)
            .optional();
    }

    @Override
    public Professional save(Professional professional) {
        jdbcClient.sql("""
                INSERT INTO professional (id, display_name, specialty, active, created_at, updated_at)
                VALUES (:id, :displayName, :specialty, :active, :createdAt, :updatedAt)
            """)
            .param("id", professional.id().toString())
            .param("displayName", professional.displayName())
            .param("specialty", professional.specialty())
            .param("active", professional.active())
            .param("createdAt", professional.createdAt().toString())
            .param("updatedAt", professional.updatedAt().toString())
            .update();
        return professional;
    }

    @Override
    public Professional update(Professional professional) {
        jdbcClient.sql("""
                UPDATE professional
                SET display_name = :displayName,
                    specialty = :specialty,
                    active = :active,
                    updated_at = :updatedAt
                WHERE id = :id
            """)
            .param("id", professional.id().toString())
            .param("displayName", professional.displayName())
            .param("specialty", professional.specialty())
            .param("active", professional.active())
            .param("updatedAt", professional.updatedAt().toString())
            .update();
        return professional;
    }

    private Professional mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Professional(
            UUID.fromString(rs.getString("id")),
            rs.getString("display_name"),
            rs.getString("specialty"),
            rs.getBoolean("active"),
            LocalDateTime.parse(rs.getString("created_at")),
            LocalDateTime.parse(rs.getString("updated_at"))
        );
    }
}

