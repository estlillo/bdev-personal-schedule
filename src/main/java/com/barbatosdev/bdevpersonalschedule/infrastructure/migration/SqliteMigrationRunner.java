package com.barbatosdev.bdevpersonalschedule.infrastructure.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class SqliteMigrationRunner implements ApplicationRunner {

    private static final String MIGRATION_LOCATION = "classpath:db/migration/V*.sql";

    private final DataSource dataSource;

    public SqliteMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(MIGRATION_LOCATION);
        Arrays.sort(resources, Comparator.comparing(resource -> {
            String filename = resource.getFilename();
            return filename == null ? "" : filename;
        }));

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            ensureHistoryTable(connection);

            for (Resource resource : resources) {
                String version = extractVersion(resource.getFilename());
                if (!isApplied(connection, version)) {
                    executeStatements(connection, readResource(resource));
                    recordMigration(connection, version, resource.getFilename());
                }
            }

            connection.commit();
        }
    }

    private void ensureHistoryTable(Connection connection) throws Exception {
        String sql = """
            CREATE TABLE IF NOT EXISTS schema_migration (
                version TEXT PRIMARY KEY,
                description TEXT NOT NULL,
                applied_at TEXT NOT NULL
            )
            """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private boolean isApplied(Connection connection, String version) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT COUNT(1) FROM schema_migration WHERE version = ?")) {
            statement.setString(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private void recordMigration(Connection connection, String version, String filename) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO schema_migration(version, description, applied_at) VALUES (?, ?, ?)")) {
            statement.setString(1, version);
            statement.setString(2, filename);
            statement.setString(3, LocalDateTime.now().toString());
            statement.executeUpdate();
        }
    }

    private String readResource(Resource resource) throws Exception {
        try (Scanner scanner = new Scanner(resource.getInputStream()).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private void executeStatements(Connection connection, String scriptContent) throws Exception {
        String[] statements = scriptContent.split(";");
        for (String rawStatement : statements) {
            String statement = rawStatement.trim();
            if (!statement.isEmpty()) {
                try (Statement sqlStatement = connection.createStatement()) {
                    sqlStatement.execute(statement);
                }
            }
        }
    }

    private String extractVersion(String filename) {
        if (filename == null || !filename.startsWith("V") || !filename.contains("__")) {
            throw new IllegalStateException("Invalid migration filename: " + filename);
        }
        return filename.substring(1, filename.indexOf("__"));
    }
}

