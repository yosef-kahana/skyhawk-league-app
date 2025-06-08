package com.skyhawk.league.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhawk.league.model.Game;
import com.skyhawk.league.model.Player;
import com.skyhawk.league.model.Statistic;
import com.skyhawk.league.model.Team;
import com.skyhawk.league.repository.GameRepository;
import com.skyhawk.league.repository.PlayerRepository;
import com.skyhawk.league.repository.StatisticRepository;
import com.skyhawk.league.repository.TeamRepository;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerController {
	private static final Logger log = LoggerFactory.getLogger(PlayerController.class);

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final StatisticRepository statisticRepository;
    private final GameRepository gameRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlayerController(Connection connection) {
        this.playerRepository = new PlayerRepository(connection);
        this.teamRepository = new TeamRepository(connection);
        this.statisticRepository = new StatisticRepository(connection);
        this.gameRepository = new GameRepository(connection);
    }

    public void handle(HttpExchange exchange) {
		log.info("Start handle: exchange={}", exchange);
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");

            if (segments.length >= 6 && "league".equals(segments[1])) {
                String leagueName = segments[2];
                String teamName = segments[4];

                if ("players".equals(segments[5])) {
                    if ("GET".equals(method) && segments.length == 6) {
                        getAllTeamPlayers(exchange, teamName);
                    } else if ("POST".equals(method) && segments.length == 6) {
                        createPlayer(exchange, teamName);
                    } else if ("GET".equals(method) && segments.length == 7) {
                        getPlayerByName(exchange, teamName, segments[6]);
                    } else if ("GET".equals(method) && segments.length == 8 && "season-statistics".equals(segments[7])) {
                        getPlayerSeasonStats(exchange, segments[6]);
                    } else if (segments.length == 10 && "games".equals(segments[7]) && "statistics".equals(segments[9])) {
                        getPlayerGameStatistics(exchange, segments[6], segments[8]);
                    } else if (segments.length == 9 && "games".equals(segments[7]) && "current".equals(segments[8])) {
                        getPlayerCurrentGameStatistics(exchange, segments[6]);
                    }
                }
            }
            else {
            	exchange.sendResponseHeaders(404, -1);
            }
        } catch (Exception e) {
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {}
        }
		log.info("End handle: exchange={}", exchange);
    }

    private void createPlayer(HttpExchange exchange, String teamName) throws Exception {
		log.debug("Start createPlayer: exchange={}, teamName={}", exchange, teamName);
        Team team = teamRepository.getByName(teamName);
        if (team == null) {
            exchange.sendResponseHeaders(404, -1);
        }
        else {
	        try (InputStream is = exchange.getRequestBody()) {
	            Player player = objectMapper.readValue(is, Player.class);
	            player.setId(0);
	            player.setTeamId(team.getId());
	            long id = playerRepository.saveIfNotExists(player);
	            String response = "{\"id\": " + id + "}";
	            exchange.getResponseHeaders().add("Content-Type", "application/json");
	            exchange.sendResponseHeaders(201, response.getBytes(StandardCharsets.UTF_8).length);
	            try (OutputStream os = exchange.getResponseBody()) {
	                os.write(response.getBytes(StandardCharsets.UTF_8));
	            }
	        }
        }
		log.debug("End createPlayer: exchange={}, teamName={}", exchange, teamName);
    }

    private void getAllTeamPlayers(HttpExchange exchange, String teamName) throws Exception {
		log.debug("Start getAllTeamPlayers: exchange={}, teamName={}", exchange, teamName);
        List<Player> players = playerRepository.getByTeamName(teamName);
        String response = objectMapper.writeValueAsString(players);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
		log.debug("End getAllTeamPlayers: exchange={}, teamName={}", exchange, teamName);
    }

    private void getPlayerByName(HttpExchange exchange, String teamName, String playerName) throws Exception {
		log.debug("Start getPlayerByName: exchange={}, teamName={}, playerName={}", exchange, teamName, playerName);
        Player player = playerRepository.getByName(playerName);
        Team team = teamRepository.getByName(teamName);
        if (player == null || team == null || player.getTeamId() != team.getId()) {
            exchange.sendResponseHeaders(404, -1);
        }
        else {
	        String response = objectMapper.writeValueAsString(player);
	        exchange.getResponseHeaders().add("Content-Type", "application/json");
	        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
	        try (OutputStream os = exchange.getResponseBody()) {
	            os.write(response.getBytes(StandardCharsets.UTF_8));
	        }
        }
		log.debug("End getPlayerByName: exchange={}, teamName={}, playerName={}", exchange, teamName, playerName);
    }

    private void getPlayerSeasonStats(HttpExchange exchange, String playerName) throws Exception {
		log.debug("Start getPlayerSeasonStats: exchange={}, playerName={}", exchange, playerName);
        List<Statistic> stats = statisticRepository.getByPlayerName(playerName);
        Map<String, Double> results = new HashMap<>();
        for (Statistic stat : stats) {
            String key = String.valueOf(stat.getStatisticTypeId());
            double value = stat.getNumberValue() + stat.getFloatValue();
            results.put(key, results.getOrDefault(key, 0.0) + value);
        }
        String response = objectMapper.writeValueAsString(results);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
		log.debug("End getPlayerSeasonStats: exchange={}, playerName={}", exchange, playerName);
    }

    private void getPlayerGameStatistics(HttpExchange exchange, String playerName, String gameName) throws Exception {
		log.debug("Start getPlayerGameStatistics: exchange={}, playerName={}, gameName={}", exchange, playerName, gameName);
        List<Statistic> stats = statisticRepository.getByPlayerNameAndGameName(playerName, gameName);
        String response = objectMapper.writeValueAsString(stats);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
		log.debug("End getPlayerGameStatistics: exchange={}, playerName={}, gameName={}", exchange, playerName, gameName);
    }

    private void getPlayerCurrentGameStatistics(HttpExchange exchange, String playerName) throws Exception {
		log.debug("Start getPlayerCurrentGameStatistics: exchange={}, playerName={}", exchange, playerName);
        List<Statistic> allStats = statisticRepository.getByPlayerName(playerName);
        List<Statistic> currentStats = new ArrayList<>();
        for (Statistic stat : allStats) {
            Game game = gameRepository.getById(stat.getGameId());
            if (game != null && game.getGameState() != null && !game.getGameState().equals(Game.GameStateEnum.End)) {
                currentStats.add(stat);
            }
        }
        String response = objectMapper.writeValueAsString(currentStats);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
		log.debug("End getPlayerCurrentGameStatistics: exchange={}, playerName={}", exchange, playerName);
    }
}  
