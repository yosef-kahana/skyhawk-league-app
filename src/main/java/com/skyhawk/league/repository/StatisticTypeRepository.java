package com.skyhawk.league.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skyhawk.league.model.StatisticType;
import com.skyhawk.league.model.StatisticType.StatTypeEnum;

public class StatisticTypeRepository implements BaseRepository<StatisticType> {

    private static final Logger log = LoggerFactory.getLogger(StatisticTypeRepository.class);
    private final Connection connection;

    public StatisticTypeRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTable() throws SQLException {
        log.info("Start createTable for statistic_type");
        String sql = """
            CREATE TABLE IF NOT EXISTS statistic_type (
                id SERIAL PRIMARY KEY,
                league_id BIGINT NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                type TEXT NOT NULL DEFAULT 'NUMBER',
                min_value INTEGER,
                max_value INTEGER,
                UNIQUE (league_id, name),
                FOREIGN KEY (league_id) REFERENCES league(id) ON DELETE CASCADE
            );
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }

        log.info("End createTable for statistic_type");
    }

    @Override
    public long saveIfNotExists(StatisticType type) throws SQLException {
        log.info("Start saveIfNotExists: leagueId={}, name={}", type.getLeagueId(), type.getName());
        Long generatedId = null;

        String selectSql = "SELECT id FROM statistic_type WHERE league_id = ? AND name = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setLong(1, type.getLeagueId());
            selectStmt.setString(2, type.getName());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    generatedId = rs.getLong("id");
                    log.trace("StatisticType already exists with ID: {}", generatedId);
                }
            }
        }

        if (generatedId == null) {
            String insertSql = """
                INSERT INTO statistic_type (league_id, name, description, type, min_value, max_value)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
            """;

            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setLong(1, type.getLeagueId());
                insertStmt.setString(2, type.getName());
                insertStmt.setString(3, type.getDescription());
                insertStmt.setString(4, type.getType().name());
                if (type.getMinValue() != null) {
                    insertStmt.setInt(5, type.getMinValue());
                } else {
                    insertStmt.setNull(5, Types.INTEGER);
                }
                if (type.getMaxValue() != null) {
                    insertStmt.setInt(6, type.getMaxValue());
                } else {
                    insertStmt.setNull(6, Types.INTEGER);
                }

                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        generatedId = rs.getLong(1);
                        type.setId(generatedId);
                    } else {
                        throw new SQLException("Failed to insert statistic_type, no ID returned.");
                    }
                }
            }
        }

        log.info("End saveIfNotExists: name={}, return={}", type.getName(), generatedId);
        return generatedId;
    }

    @Override
    public List<StatisticType> getAll() throws SQLException {
        log.info("Start getAll for statistic_type");
        List<StatisticType> types = new ArrayList<>();
        String sql = "SELECT * FROM statistic_type";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                StatisticType type = new StatisticType(
                        rs.getLong("league_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        StatTypeEnum.valueOf(rs.getString("type")),
                        rs.getObject("min_value", Integer.class),
                        rs.getObject("max_value", Integer.class)
                );
                type.setId(rs.getLong("id"));
                types.add(type);
                log.trace("Add statisticType: {}", type);
            }
        }

        log.info("End getAll: count={}", types.size());
        return types;
    }

    @Override
    public StatisticType getById(long id) throws SQLException {
        log.info("Start getById: id={}", id);
        StatisticType type = null;
        String sql = "SELECT * FROM statistic_type WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    type = new StatisticType(
                            rs.getLong("league_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            StatTypeEnum.valueOf(rs.getString("type")),
                            rs.getObject("min_value", Integer.class),
                            rs.getObject("max_value", Integer.class)
                    );
                    type.setId(rs.getLong("id"));
                }
            }
        }

        log.info("End getById: id={}, return={}", id, type);
        return type;
    }

    @Override
    public StatisticType getByName(String name) throws SQLException {
        log.info("Start getByName: name={}", name);
        StatisticType type = null;
        String sql = "SELECT * FROM statistic_type WHERE name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    type = new StatisticType(
                            rs.getLong("league_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            StatTypeEnum.valueOf(rs.getString("type")),
                            rs.getObject("min_value", Integer.class),
                            rs.getObject("max_value", Integer.class)
                    );
                    type.setId(rs.getLong("id"));
                }
            }
        }

        log.info("End getByName: name={}, return={}", name, type);
        return type;
    }
}
