package com.skyhawk.league.repository;

import com.skyhawk.league.model.Game;
import com.skyhawk.league.model.Player;
import com.skyhawk.league.model.Statistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StatisticRepository implements BaseRepository<Statistic> {

    private static final Logger log = LoggerFactory.getLogger(StatisticRepository.class);
    private final Connection connection;

    public StatisticRepository(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void createTable() throws SQLException {
        log.info("Start createTable for statistic");
        String sql = """
            CREATE TABLE IF NOT EXISTS statistic (
                id SERIAL PRIMARY KEY,
                player_id BIGINT NOT NULL,
                game_id BIGINT NOT NULL,
                statistic_type_id BIGINT NOT NULL,
                number_value BIGINT DEFAULT 0,
                float_value REAL DEFAULT 0.0,
                FOREIGN KEY (player_id) REFERENCES player(id) ON DELETE CASCADE,
                FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE,
                FOREIGN KEY (statistic_type_id) REFERENCES statistic_type(id) ON DELETE CASCADE
            );
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            stmt.execute("CREATE UNIQUE INDEX idx_statistic_composite ON statistic (player_id, game_id, statistic_type_id);");
            stmt.execute("CREATE INDEX idx_statistic_player_id ON statistic (player_id);");
            stmt.execute("CREATE INDEX idx_statistic_type_id ON statistic (statistic_type_id);");        
        }

        log.info("End createTable for statistic");
    }

    @Override
    public long saveIfNotExists(Statistic stat) throws SQLException {
        log.info("Start saveIfNotExists: playerId={}, gameId={}, typeId={}", stat.getPlayerId(), stat.getGameId(), stat.getStatisticTypeId());
        Long generatedId = null;

        String selectSql = "SELECT id FROM statistic WHERE player_id = ? AND game_id = ? AND statistic_type_id = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setLong(1, stat.getPlayerId());
            selectStmt.setLong(2, stat.getGameId());
            selectStmt.setLong(3, stat.getStatisticTypeId());
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    generatedId = rs.getLong("id");
                    log.trace("Statistic already exists with ID: {}", generatedId);
                }
            }
        }

        if (generatedId == null) {
            String insertSql = """
                INSERT INTO statistic (player_id, game_id, statistic_type_id, number_value, float_value)
                VALUES (?, ?, ?, ?, ?) RETURNING id
            """;
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setLong(1, stat.getPlayerId());
                insertStmt.setLong(2, stat.getGameId());
                insertStmt.setLong(3, stat.getStatisticTypeId());
                insertStmt.setLong(4, stat.getNumberValue());
                insertStmt.setFloat(5, stat.getFloatValue());

                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        generatedId = rs.getLong(1);
                        stat.setId(generatedId);
                    } else {
                        throw new SQLException("Failed to insert statistic, no ID returned.");
                    }
                }
            }
        }

        log.info("End saveIfNotExists: return={}", generatedId);
        return generatedId;
    }

    @Override
    public Statistic getById(long id) throws SQLException {
        log.info("Start getById: id={}", id);
        Statistic stat = null;
        String sql = "SELECT * FROM statistic WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    stat = new Statistic(
                            rs.getLong("player_id"),
                            rs.getLong("game_id"),
                            rs.getLong("statistic_type_id")
                    );
                    stat.setId(rs.getLong("id"));
                    stat.setNumberValue(rs.getLong("number_value"));
                    stat.setFloatValue(rs.getFloat("float_value"));
                }
            }
        }

        log.info("End getById: return={}", stat);
        return stat;
    }

    @Override
    public List<Statistic> getAll() throws SQLException {
        log.info("Start getAll");
        List<Statistic> result = new ArrayList<>();
        String sql = "SELECT * FROM statistic";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Statistic stat = new Statistic(
                        rs.getLong("player_id"),
                        rs.getLong("game_id"),
                        rs.getLong("statistic_type_id")
                );
                stat.setId(rs.getLong("id"));
                stat.setNumberValue(rs.getInt("number_value"));
                stat.setFloatValue(rs.getFloat("float_value"));
                result.add(stat);
            }
        }

        log.info("End getAll: count={}", result.size());
        return result;
    }

    @Override
    public Statistic getByName(String name) throws SQLException {
        log.info("getByName is not supported for Statistic");
        throw new UnsupportedOperationException("Statistic does not support getByName()");
    }

     public List<Statistic> getByPlayerId(long playerId) throws SQLException {
        log.info("Start getByPlayerId: playerId={}", playerId);
        List<Statistic> result = new ArrayList<>();
        String sql = "SELECT * FROM statistic WHERE player_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, playerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Statistic stat = new Statistic(
                            rs.getLong("player_id"),
                            rs.getLong("game_id"),
                            rs.getLong("statistic_type_id")
                    );
                    stat.setId(rs.getLong("id"));
                    stat.setNumberValue(rs.getInt("number_value"));
                    stat.setFloatValue(rs.getFloat("float_value"));
                    result.add(stat);
                }
            }
        }

        log.info("End getByPlayerId: count={}", result.size());
        return result;
    }

    public List<Statistic> getByPlayerName(String playerName) throws SQLException {
        log.info("Start getByPlayerName: name={}", playerName);
        PlayerRepository playerRepo = new PlayerRepository(connection);
        Player player = playerRepo.getByName(playerName);
        return player != null ? getByPlayerId(player.getId()) : new ArrayList<>();
    }

    public List<Statistic> getByPlayerIdAndGameId(long playerId, long gameId) throws SQLException {
        log.info("Start getByPlayerIdAndGameId: playerId={}, gameId={}", playerId, gameId);
        List<Statistic> result = new ArrayList<>();
        String sql = "SELECT * FROM statistic WHERE player_id = ? AND game_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, playerId);
            stmt.setLong(2, gameId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Statistic stat = new Statistic(
                            rs.getLong("player_id"),
                            rs.getLong("game_id"),
                            rs.getLong("statistic_type_id")
                    );
                    stat.setId(rs.getLong("id"));
                    stat.setNumberValue(rs.getInt("number_value"));
                    stat.setFloatValue(rs.getFloat("float_value"));
                    result.add(stat);
                }
            }
        }

        log.info("End getByPlayerIdAndGameId: count={}", result.size());
        return result;
    }

    public List<Statistic> getByPlayerNameAndGameId(String playerName, long gameId) throws SQLException {
        log.info("Start getByPlayerNameAndGameId: playerName={}, gameId={}", playerName, gameId);
        PlayerRepository playerRepo = new PlayerRepository(connection);
        Player player = playerRepo.getByName(playerName);
        return player != null ? getByPlayerIdAndGameId(player.getId(), gameId) : new ArrayList<>();
    }

    public List<Statistic> getByPlayerIdAndGameName(long playerId, String gameName) throws SQLException {
        log.info("Start getByPlayerIdAndGameName: playerId={}, gameName={}", playerId, gameName);
        GameRepository gameRepo = new GameRepository(connection);
        Game game = gameRepo.getByName(gameName);
        return game != null ? getByPlayerIdAndGameId(playerId, game.getId()) : new ArrayList<>();
    }

    public List<Statistic> getByPlayerNameAndGameName(String playerName, String gameName) throws SQLException {
        log.info("Start getByPlayerNameAndGameName: playerName={}, gameName={}", playerName, gameName);
        PlayerRepository playerRepo = new PlayerRepository(connection);
        GameRepository gameRepo = new GameRepository(connection);
        Player player = playerRepo.getByName(playerName);
        Game game = gameRepo.getByName(gameName);
        return (player != null && game != null)
                ? getByPlayerIdAndGameId(player.getId(), game.getId())
                : new ArrayList<>();
    }
    
    public void updateNumberValue(long playerId, long gameId, long statisticTypeId, long newValue) throws SQLException {
        log.info("Start updateNumberValue: playerId={}, gameId={}, statTypeId={}, newValue={}", playerId, gameId, statisticTypeId, newValue);
        String sql = "UPDATE statistic SET number_value = ? WHERE player_id = ? AND game_id = ? AND statistic_type_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, newValue);
            stmt.setLong(2, playerId);
            stmt.setLong(3, gameId);
            stmt.setLong(4, statisticTypeId);
            stmt.executeUpdate();
        }
        log.info("End updateNumberValue");
    }

    public void updateFloatValue(long playerId, long gameId, long statisticTypeId, float newValue) throws SQLException {
        log.info("Start updateFloatValue: playerId={}, gameId={}, statTypeId={}, newValue={}", playerId, gameId, statisticTypeId, newValue);
        String sql = "UPDATE statistic SET float_value = ? WHERE player_id = ? AND game_id = ? AND statistic_type_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setFloat(1, newValue);
            stmt.setLong(2, playerId);
            stmt.setLong(3, gameId);
            stmt.setLong(4, statisticTypeId);
            stmt.executeUpdate();
        }
        log.info("End updateFloatValue");
    }

    public void removeIfExist(long playerId, long gameId, long statisticTypeId) throws SQLException {
        log.info("Start deleteByPlayerGameAndType: playerId={}, gameId={}, typeId={}", playerId, gameId, statisticTypeId);
        String sql = "DELETE FROM statistic WHERE player_id = ? AND game_id = ? AND statistic_type_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, playerId);
            stmt.setLong(2, gameId);
            stmt.setLong(3, statisticTypeId);
            stmt.executeUpdate();
        }

        log.info("End deleteByPlayerGameAndType");
    }}
