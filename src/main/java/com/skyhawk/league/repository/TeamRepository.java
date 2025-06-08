package com.skyhawk.league.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skyhawk.league.model.Team;

public class TeamRepository implements BaseRepository<Team> {
    private static final Logger log = LoggerFactory.getLogger(TeamRepository.class);

    private final Connection connection;

    public TeamRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTable() throws SQLException {
        log.info("Start createTable for team");
        String sql = """
            CREATE TABLE IF NOT EXISTS team (
                id SERIAL PRIMARY KEY,
                league_id BIGINT NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                UNIQUE (league_id, name),
                FOREIGN KEY (league_id) REFERENCES league(id) ON DELETE CASCADE
            );
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }

        log.info("End createTable for team");
    }

    @Override
    public long saveIfNotExists(Team team) throws SQLException {
        log.info("Start saveIfNotExists: leagueId={}, name={}", team.getLeagueId(), team.getName());
        Long generatedId = null;

        String selectSql = "SELECT id FROM team WHERE league_id = ? AND name = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setLong(1, team.getLeagueId());
            selectStmt.setString(2, team.getName());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    generatedId = rs.getLong("id");
                    log.trace("Team already exists with ID: {}", generatedId);
                }
            }
        }

        if (generatedId == null) {
            String insertSql = "INSERT INTO team (league_id, name, description) VALUES (?, ?, ?) RETURNING id";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setLong(1, team.getLeagueId());
                insertStmt.setString(2, team.getName());
                insertStmt.setString(3, team.getDescription());

                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        generatedId = rs.getLong(1);
                        team.setId(generatedId);
                    } else {
                        throw new SQLException("Failed to insert team, no ID returned.");
                    }
                }
            }
        }

        log.info("End saveIfNotExists: name={}, return={}", team.getName(), generatedId);
        return generatedId;
    }

    @Override
    public List<Team> getAll() throws SQLException {
        log.info("Start getAll for team");
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM team";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Team team = new Team(
                        rs.getLong("league_id"),
                        rs.getString("name"),
                        rs.getString("description")
                );
                team.setId(rs.getLong("id"));
                teams.add(team);
                log.trace("Add team: {}", team);
            }
        }

        log.info("End getAll: count={}", teams.size());
        return teams;
    }

    @Override
    public Team getById(long id) throws SQLException {
        log.info("Start getById: id={}", id);
        Team team = null;
        String sql = "SELECT * FROM team WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    team = new Team(
                            rs.getLong("league_id"),
                            rs.getString("name"),
                            rs.getString("description")
                    );
                    team.setId(rs.getLong("id"));
                }
            }
        }

        log.info("End getById: id={}, return={}", id, team);
        return team;
    }

    @Override
    public Team getByName(String name) throws SQLException {
        log.info("Start getByName: name={}", name);
        Team team = null;
        String sql = "SELECT * FROM team WHERE name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    team = new Team(
                            rs.getLong("league_id"),
                            rs.getString("name"),
                            rs.getString("description")
                    );
                    team.setId(rs.getLong("id"));
                }
            }
        }

        log.info("End getByName: name={}, return={}", name, team);
        return team;
    }
}
