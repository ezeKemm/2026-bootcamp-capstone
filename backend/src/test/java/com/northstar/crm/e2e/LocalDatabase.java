package com.northstar.crm.e2e;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Lets e2e tests put the LOCAL dev database (the one the running backend uses) into a known state.
 * Connection details come from the repo's .env file (DB_URL, DB_USERNAME, DB_PASSWORD), the same file the backend reads.
 * Environment variables with the same names win over .env.
 */
final class LocalDatabase {

    private LocalDatabase() {
    }

    /** Sets a seeded customer's status, e.g. put Ravi Singh back to PROSPECT so the activate test can run again. */
    static void setCustomerStatus(String fullName, String status) {
        Map<String, String> config = loadConfig();
        try (Connection connection = DriverManager.getConnection(
                     config.get("DB_URL"), config.get("DB_USERNAME"), config.get("DB_PASSWORD"));
             PreparedStatement update = connection.prepareStatement(
                     "update customers set status = ? where full_name = ?")) {
            update.setString(1, status);
            update.setString(2, fullName);
            if (update.executeUpdate() != 1) {
                throw new IllegalStateException("Expected exactly one customer named " + fullName);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not reach the local database. Is `docker compose up -d` running?", e);
        }
    }

    private static Map<String, String> loadConfig() {
        Map<String, String> config = new HashMap<>();
        // mvnw runs from backend/, so the repo-root .env is one level up; also accept a .env in backend/.
        for (Path envFile : new Path[] {Path.of("../.env"), Path.of(".env")}) {
            if (Files.exists(envFile)) {
                readEnvFile(envFile, config);
            }
        }
        for (String key : new String[] {"DB_URL", "DB_USERNAME", "DB_PASSWORD"}) {
            String fromEnvironment = System.getenv(key);
            if (fromEnvironment != null && !fromEnvironment.isBlank()) {
                config.put(key, fromEnvironment);
            }
            if (config.get(key) == null) {
                throw new IllegalStateException(key + " not found in .env or environment variables");
            }
        }
        return config;
    }

    private static void readEnvFile(Path envFile, Map<String, String> config) {
        try {
            for (String line : Files.readAllLines(envFile)) {
                String trimmed = line.trim();
                int equals = trimmed.indexOf('=');
                if (trimmed.isEmpty() || trimmed.startsWith("#") || equals < 1) {
                    continue;
                }
                config.putIfAbsent(trimmed.substring(0, equals).trim(), trimmed.substring(equals + 1).trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + envFile, e);
        }
    }
}
