package com.skyhawk.league.repository;

import com.skyhawk.league.model.Player;
import com.skyhawk.league.model.Team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerRepository implements BaseRepository<Player> {

    private static final Logger log = LoggerFactory.getLogger(PlayerRepository.class);
    private final Connection connection;

    public PlayerRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTable() throws SQLException {
        log.info("Start createTable for player");
        String sql = """
            CREATE TABLE IF NOT EXISTS player (
                id SERIAL PRIMARY KEY,
                team_id BIGINT NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                UNIQUE (team_id, name),
                FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE
            );
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute("CREATE UNIQUE INDEX idx_player_team_name ON player (team_id, name);");
        }

        log.info("End createTable for player");
    }

    @Override
    public long saveIfNotExists(Player player) throws SQLException {
        log.info("Start saveIfNotExists: player={}", player.getName());
        Long generatedId = null;

        String selectSql = "SELECT id FROM player WHERE team_id = ? AND name = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setLong(1, player.getTeamId());
            selectStmt.setString(2, player.getName());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    generatedId = rs.getLong("id");
                    log.trace("Player already exists with ID: {}", generatedId);
                }
            }
        }

        if (generatedId == null) {
            String insertSql = "INSERT INTO player (team_id, name, description) VALUES (?, ?, ?) RETURNING id";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setLong(1, player.getTeamId());
                insertStmt.setString(2, player.getName());
                insertStmt.setString(3, player.getDescription());

                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        generatedId = rs.getLong(1);
                        player.setId(generatedId);
                    } else {
                        throw new SQLException("Failed to insert player, no ID returned.");
                    }
                }
            }
        }

        log.info("End saveIfNotExists: player={}, return={}", player.getName(), generatedId);
        return generatedId;
    }

    @Override
    public List<Player> getAll() throws SQLException {
        log.info("Start getAll for players");
        List<Player> players = new ArrayList<>();
        String sql = "SELECT * FROM player";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Player player = new Player(
                        rs.getLong("team_id"),
                        rs.getString("name"),
                        rs.getString("description")
                );
                player.setId(rs.getLong("id"));
                players.add(player);
                log.trace("Add player: {}", player);
            }
        }

        log.info("End getAll: count={}", players.size());
        return players;
    }

    @Override
    public Player getById(long id) throws SQLException {
        log.info("Start getById: id={}", id);
        Player player = null;
        String sql = "SELECT * FROM player WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    player = new Player(
                            rs.getLong("team_id"),
                            rs.getString("name"),
                            rs.getString("description")
                    );
                    player.setId(rs.getLong("id"));
                }
            }
        }

        log.info("End getById: id={}, return={}", id, player);
        return player;
    }

    @Override
    public Player getByName(String name) throws SQLException {
        log.info("Start getByName: name={}", name);
        Player player = null;
        String sql = "SELECT * FROM player WHERE name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    player = new Player(
                            rs.getLong("team_id"),
                            rs.getString("name"),
                            rs.getString("description")
                    );
                    player.setId(rs.getLong("id"));
                    log.trace("Found player with ID: {}", player.getId());
                }
            }
        }

        log.info("End getByName: name={}, return={}", name, player);
        return player;
    }

    public List<Player> getByTeamId(long teamId) throws SQLException {
        log.info("Start getByTeamId: teamId={}", teamId);
        List<Player> players = new ArrayList<>();
        String sql = "SELECT * FROM player WHERE team_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Player player = new Player(
                            rs.getLong("team_id"),
                            rs.getString("name"),
                            rs.getString("description")
                    );
                    player.setId(rs.getLong("id"));
                    players.add(player);
                }
            }
        }

        log.info("End getByTeamId: count={}", players.size());
        return players;
    }

    public List<Player> getByTeamName(String teamName) throws SQLException {
        log.info("Start getByTeamName: teamName={}", teamName);
        List<Player> result = new ArrayList<>();
        TeamRepository teamRepository = new TeamRepository(connection);
        Team team = teamRepository.getByName(teamName);
        if (team != null) {
            result = getByTeamId(team.getId());
        }
        log.info("End getByTeamName: teamName={}, count={}", teamName, result.size());
        return result;
    }
}
