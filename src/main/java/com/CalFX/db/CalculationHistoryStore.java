package com.CalFX.db;

import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Saves calculator and currency results to a local SQLite database file and keeps only the
 * most recent 100 rows. One instance is shared for the app's lifetime (created once in
 * Navigator), so every screen writes to and reads from the same table.
 *
 * All database access happens on one background thread; callers get their answer back on
 * the JavaFX thread, the same pattern GraphPane uses for sampling a function.
 */
public class CalculationHistoryStore {

    private static final int MAX_RECORDS = 100;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path databaseFile = Path.of(System.getProperty("user.home"), ".CalFX", "CalFX-history.db");
    private final String jdbcUrl = "jdbc:sqlite:" + databaseFile;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "history-db");
        thread.setDaemon(true);
        return thread;
    });

    public CalculationHistoryStore() {
        worker.execute(this::createTableIfNeeded);
    }

    /** Fire-and-forget: adds one row, then trims the table back down to the newest 100. */
    public void insertAsync(String type, String expression, String result) {
        worker.execute(() -> {
            try (Connection connection = connect()) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO calculations(type, expression, result, created_at) VALUES (?, ?, ?, ?)")) {
                    insert.setString(1, type);
                    insert.setString(2, expression);
                    insert.setString(3, result);
                    insert.setString(4, LocalDateTime.now().format(TIMESTAMP_FORMAT));
                    insert.executeUpdate();
                }
                try (Statement trim = connection.createStatement()) {
                    trim.executeUpdate(
                            "DELETE FROM calculations WHERE id NOT IN "
                                    + "(SELECT id FROM calculations ORDER BY id DESC LIMIT " + MAX_RECORDS + ")");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /** Loads the most recent rows (newest first) and reports them on the JavaFX thread. */
    public void loadRecentAsync(Consumer<List<CalculationRecord>> onLoaded, Consumer<Throwable> onError) {
        Task<List<CalculationRecord>> task = new Task<>() {
            @Override
            protected List<CalculationRecord> call() throws SQLException {
                List<CalculationRecord> records = new ArrayList<>();
                String sql = "SELECT id, type, expression, result, created_at "
                        + "FROM calculations ORDER BY id DESC LIMIT " + MAX_RECORDS;
                try (Connection connection = connect();
                     Statement statement = connection.createStatement();
                     ResultSet rows = statement.executeQuery(sql)) {
                    while (rows.next()) {
                        records.add(new CalculationRecord(
                                rows.getLong("id"),
                                rows.getString("type"),
                                rows.getString("expression"),
                                rows.getString("result"),
                                rows.getString("created_at")));
                    }
                }
                return records;
            }
        };
        task.setOnSucceeded(event -> onLoaded.accept(task.getValue()));
        task.setOnFailed(event -> onError.accept(task.getException()));
        worker.execute(task);
    }

    public void shutdown() {
        worker.shutdownNow();
    }

    // ---------------------------------------------------------------- background-thread work

    private void createTableIfNeeded() {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS calculations ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "type TEXT NOT NULL, "
                            + "expression TEXT NOT NULL, "
                            + "result TEXT NOT NULL, "
                            + "created_at TEXT NOT NULL)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Connection connect() throws SQLException {
        try {
            Files.createDirectories(databaseFile.getParent());
        } catch (IOException e) {
            throw new SQLException("Cannot create " + databaseFile.getParent(), e);
        }
        return DriverManager.getConnection(jdbcUrl);
    }
}
