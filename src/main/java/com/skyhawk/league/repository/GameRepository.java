package com.skyhawk.league.repository;

import com.skyhawk.league.model.Game;
import com.skyhawk.league.model.Game.GameStateEnum;
import com.skyhawk.league.model.Team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class GameRepository implements BaseRepository<Game>{

    private static final Logger log = LoggerFactory.getLogger(GameRepository.class);
    private final Connection connection;

    public GameRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTable() throws SQLException {
        log.info("Start createTable");

        String sql = """
            CREATE TABLE IF NOT EXISTS game (
                id SERIAL PRIMARY KEY,
                league_id BIGINT NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                home_team_id BIGINT NOT NULL,
                visitor_team_id BIGINT NOT NULL,
                date DATE NOT NULL,
                start_time TIME,
                end_time TIME,
                game_state TEXT,
                UNIQUE (league_id, name),
                FOREIGN KEY (league_id) REFERENCES league(id) ON DELETE CASCADE,
                FOREIGN KEY (home_team_id) REFERENCES team(id) ON DELETE CASCADE,
                FOREIGN KEY (visitor_team_id) REFERENCES team(id) ON DELETE CASCADE
            );
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute("CREATE UNIQUE INDEX idx_game_id ON game (id);");
        }

        log.info("End createTable");
    }

    @Override
    public long saveIfNotExists(Game game) throws SQLException {
        log.info("Start saveIfNotExists: game={}", game.getName());
        Long generatedId = null;

        String selectSql = "SELECT id FROM game WHERE league_id = ? AND name = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setLong(1, game.getLeagueId());
            selectStmt.setString(2, game.getName());

            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    generatedId = rs.getLong("id");
                    log.trace("Game already exists with ID: {}", generatedId);
                }
            }
        }

        if (generatedId == null) {
            String insertSql = """
                INSERT INTO game (league_id, name, description, home_team_id, visitor_team_id, date, start_time, game_state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id;
            """;

            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setLong(1, game.getLeagueId());
                insertStmt.setString(2, game.getName());
                insertStmt.setString(3, game.getDescription());
                insertStmt.setLong(4, game.getHomeTeamId());
                insertStmt.setLong(5, game.getVisitorTeamId());
                insertStmt.setDate(6, Date.valueOf(game.getDate()));
                insertStmt.setTime(7, game.getStartTime() != null ? Time.valueOf(game.getStartTime()) : null);
                insertStmt.setString(8, game.getGameState() != null ? game.getGameState().name() : null);

                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        generatedId = rs.getLong(1);
                        game.setId(generatedId);
                    } else {
                        throw new SQLException("Failed to insert game, no ID returned.");
                    }
                }
            }
        }

        log.info("End saveIfNotExists: game={}, return={}", game.getName(), generatedId);
        return generatedId;
    }

    @Override
    public List<Game> getAll() throws SQLException {
        log.info("Start getAll");
        String sql = "SELECT * FROM game";
        List<Game> games = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Game game = extractGameFromResultSet(rs);
                games.add(game);
                log.trace("Add game: {}", game);
            }
        }

        log.info("End getAll: count={}", games.size());
        return games;
    }

    @Override
    public Game getById(long id) throws SQLException {
        log.info("Start getById: id={}", id);
        Game game = null;

        String sql = "SELECT * FROM game WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    game = extractGameFromResultSet(rs);
                }
            }
        }

        log.info("End getById: id={}, return={}", id, game);
        return game;
    }

    @Override
    public Game getByName(String name) throws SQLException {
        log.info("Start getByName: name={}", name);
        Game game = null;

        String sql = "SELECT * FROM game WHERE name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    game = extractGameFromResultSet(rs);
                    log.trace("Found game with ID {}", game.getId());
                }
            }
        }

        log.info("End getByName: name={}, return={}", name, game);
        return game;
    }

    public List<Game> getByTeamId(long teamId) throws SQLException {
        log.info("Start getByTeamId: teamId={}", teamId);
        List<Game> result = new ArrayList<>();
        String sql = "SELECT * FROM game WHERE home_team_id = ? OR visitor_team_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, teamId);
            stmt.setLong(2, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Game game = new Game(
                            rs.getLong("league_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getLong("home_team_id"),
                            rs.getLong("visitor_team_id"),
                            rs.getDate("date").toLocalDate()
                    );
                    game.setId(rs.getLong("id"));
                    game.setStartTime(rs.getTime("start_time") != null ? rs.getTime("start_time").toLocalTime() : null);
                    game.setEndTime(rs.getTime("end_time") != null ? rs.getTime("end_time").toLocalTime() : null);
                    game.setGameState(Game.GameStateEnum.valueOf(rs.getString("game_state")));
                    result.add(game);
                }
            }
        }

        log.info("End getByTeamId: count={}", result.size());
        return result;
    }

    public List<Game> getByTeamName(String teamName) throws SQLException {
        log.info("Start getByTeamName: teamName={}", teamName);
        TeamRepository teamRepo = new TeamRepository(connection);
        Team team = teamRepo.getByName(teamName);
        if (team != null) {
            return getByTeamId(team.getId());
        }
        return new ArrayList<>();
    }

    private Game extractGameFromResultSet(ResultSet rs) throws SQLException {
        log.debug("Start getByName: rs={}", rs);
        Game game = new Game(
                rs.getLong("league_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getLong("home_team_id"),
                rs.getLong("visitor_team_id"),
                rs.getDate("date").toLocalDate()
        );
        game.setId(rs.getLong("id"));
        Time startTime = rs.getTime("start_time");
        if (startTime != null) {
            game.setStartTime(startTime.toLocalTime());
        }
        Time endTime = rs.getTime("end_time");
        if (endTime != null) {
            game.setEndTime(endTime.toLocalTime());
        }
        String gameState = rs.getString("game_state");
        if (gameState != null) {
            game.setGameState(GameStateEnum.valueOf(gameState));
        }
        log.debug("End getByName: rs={}, return={}", rs, game);
        return game;
    }

    public void updateGameState(long gameId, String gameState) throws SQLException {
        log.info("Start updateGameState: gameId={},  state={}", gameId, gameState);

        String updateSql = "UPDATE game SET game_state = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setString(1, gameState);
            stmt.setLong(2, gameId);
            stmt.executeUpdate();
        }
        log.info("End updateGameState: gameId={},  state={}", gameId, gameState);
    }

    public void updateEndTime(long gameId, LocalTime endTime) throws SQLException {
        log.info("Start updateEndTime: gameId={}, endTime={}", gameId, endTime);

        String updateSql = "UPDATE game SET end_time = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setTime(1, Time.valueOf(endTime));
            stmt.setLong(2, gameId);
            stmt.executeUpdate();
        }
        log.info("End updateEndTime: gameId={}, endTime={}", gameId, endTime);
    }
    
    public void updateStartTime(long gameId, LocalTime startTime) throws SQLException {
        log.info("Start updateStartTime: gameId={}, startTime={}", gameId, startTime);

        String updateSql = "UPDATE game SET start_time = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            stmt.setTime(1, Time.valueOf(startTime));
            stmt.setLong(2, gameId);
            stmt.executeUpdate();
        }
        log.info("End updateStartTime: gameId={}, startTime={}", gameId, startTime);
    }

}
