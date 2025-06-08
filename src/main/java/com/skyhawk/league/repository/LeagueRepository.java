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

import com.skyhawk.league.model.League;

public class LeagueRepository implements BaseRepository<League> {
    private static final Logger log = LoggerFactory.getLogger(LeagueRepository.class);

    private final Connection connection;

    public LeagueRepository(Connection connection) {
        this.connection = connection;
    }

    // Create the league table if it doesn't exist
    public void createTable() throws SQLException {
    	log.info("Start createTable");
        String sql = """
            CREATE TABLE IF NOT EXISTS league (
                id SERIAL PRIMARY KEY,
                name TEXT UNIQUE NOT NULL,
                description TEXT
            );
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    	log.info("End createTable");
    }

    public long saveIfNotExists(League league) throws SQLException {
        log.info("Starting saveIfNotExists: league={}", league.getName());
        Long generatedId = null;
        // First, check if the league already exists by name
        String selectSql = "SELECT id FROM league WHERE name = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setString(1, league.getName());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    // Already exists, return existing id
                	generatedId = rs.getLong("id");
                    log.trace("League already exists with ID: {}", generatedId);
                }
            }
        }

        if (generatedId == null) {
	        // Insert new league if not exists
	        String insertSql = "INSERT INTO league (name, description) VALUES (?, ?) RETURNING id";
	        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
	            insertStmt.setString(1, league.getName());
	            insertStmt.setString(2, league.getDescription());
	            try (ResultSet rs = insertStmt.executeQuery()) {
	                if (rs.next()) {
	                    generatedId = rs.getLong(1);
	                    league.setId(generatedId);
	                } else {
	                    throw new SQLException("Failed to insert league, no ID returned.");
	                }
	            }
	        }
        }
        log.info("End saveIfNotExists: league={}, return={}", league.getName(), generatedId);
        return generatedId;
    }
    
    // Get all
    public List<League> getAll() throws SQLException {
        log.info("Start getAll");

        String sql = "SELECT id, name, description FROM league";
        List<League> leagues = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                League league = new League(rs.getString("name"), rs.getString("description"));
                league.setId(rs.getLong("id"));
                leagues.add(league);
                log.trace("Add league={}", league);
            }

        }
        log.info("End getAll: return={}", leagues);
        return leagues;
    }

    // Get by name
    public League getByName(String name) throws SQLException {
        log.info("Start getByName: name={}", name);
        League league = null;
        String sql = "SELECT id, name, description FROM league WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    league = new League(rs.getString("name"), rs.getString("description"));
                    league.setId(rs.getLong("id"));
                    log.trace("Finished getByName(): found league with ID {}", league.getId());
                }
            }
        }
        log.info("End getByName: name={}, return={}", name, league);
        return league;
    }

    // Get by ID
    public League getById(long id) throws SQLException {
        log.info("Start getById: id={}", id);
        League league = null;
        String sql = "SELECT id, name, description FROM league WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    league = new League(rs.getString("name"), rs.getString("description"));
                    league.setId(rs.getLong("id"));
                }
            }
        }
        log.info("End getById: Id={}, return={}", id, league);
        return league;
    }
}
