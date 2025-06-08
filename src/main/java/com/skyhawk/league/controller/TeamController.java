package com.skyhawk.league.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhawk.league.model.*;
import com.skyhawk.league.repository.*;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeamController {
	private static final Logger log = LoggerFactory.getLogger(TeamController.class);

    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;
    private final PlayerRepository playerRepository;
    private final StatisticRepository statisticRepository;
    private final GameRepository gameRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TeamController(Connection connection) {
        this.teamRepository = new TeamRepository(connection);
        this.leagueRepository = new LeagueRepository(connection);
        this.playerRepository = new PlayerRepository(connection);
        this.statisticRepository = new StatisticRepository(connection);
        this.gameRepository = new GameRepository(connection);
    }

    public void handle(HttpExchange exchange) {
		log.info("Start handle: exchange={}", exchange);
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");

            if (segments.length >= 4 && "league".equals(segments[1])) {
                String leagueName = segments[2];

                if ("teams".equals(segments[3])) {
                    if ("GET".equals(method) && segments.length == 4) {
                        getTeamsByLeague(exchange, leagueName);
                    } else if ("POST".equals(method) && segments.length == 4) {
                        createTeam(exchange, leagueName);
                    } else if ("GET".equals(method) && segments.length == 5) {
                        String teamName = segments[4];
                        getTeamByName(exchange, leagueName, teamName);
                    } else if ("GET".equals(method) && segments.length == 6 && "season-statistics".equals(segments[5])) {
                        getTeamSeasonStatistics(exchange, leagueName, segments[4]);
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
            e.printStackTrace();
        }
		log.info("End handle: exchange={}", exchange);
    }

    private void getTeamsByLeague(HttpExchange exchange, String leagueName) throws Exception {
		log.debug("Start getTeamsByLeague: exchange={}, leagueName={}", exchange, leagueName);
        League league = leagueRepository.getByName(leagueName);
        List<Team> teams = teamRepository.getAll().stream()
                .filter(team -> team.getLeagueId() == league.getId())
                .collect(Collectors.toList());
        String response = objectMapper.writeValueAsString(teams);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
		log.debug("End getTeamsByLeague: exchange={}, leagueName={}", exchange, leagueName);
    }

    private void getTeamByName(HttpExchange exchange, String leagueName, String teamName) throws Exception {
		log.debug("Start getTeamByName: exchange={}, leagueName={}, teamName={}", exchange, leagueName, teamName);
        League league = leagueRepository.getByName(leagueName);
        Team team = teamRepository.getByName(teamName);
        if (team == null || team.getLeagueId() != league.getId()) {
            exchange.sendResponseHeaders(404, -1);
        }
        else {
	        String response = objectMapper.writeValueAsString(team);
	        exchange.getResponseHeaders().add("Content-Type", "application/json");
	        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
	        try (OutputStream os = exchange.getResponseBody()) {
	            os.write(response.getBytes(StandardCharsets.UTF_8));
	        }
        }
		log.debug("End getTeamByName: exchange={}, leagueName={}, teamName={}", exchange, leagueName, teamName);
    }

    private void createTeam(HttpExchange exchange, String leagueName) throws Exception {
		log.debug("Start createTeam: exchange={}, leagueName={}", exchange, leagueName);
        League league = leagueRepository.getByName(leagueName);
        try (InputStream is = exchange.getRequestBody()) {
            Team team = objectMapper.readValue(is, Team.class);
            team.setId(0);
            team.setLeagueId(league.getId());
            long id = teamRepository.saveIfNotExists(team);
            String response = "{\"id\": " + id + "}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
		log.debug("End createTeam: exchange={}, leagueName={}", exchange, leagueName);
    }

    private void getTeamSeasonStatistics(HttpExchange exchange, String leagueName, String teamName) throws Exception {
		log.debug("Start getTeamSeasonStatistics: exchange={}, leagueName={}, teamName={}", exchange, leagueName, teamName);
        League league = leagueRepository.getByName(leagueName);
        Team team = teamRepository.getByName(teamName);

        if (team == null || team.getLeagueId() != league.getId()) {
            exchange.sendResponseHeaders(404, -1);
        }
        else {
	        List<Game> games = gameRepository.getByTeamId(team.getId());
	        Map<Object, Map<Object, Number>> statisticsMap = new HashMap<>();
	
	        List<Player> players = playerRepository.getByTeamId(team.getId());
	        for (Player player : players) {
	            int gamesPlayed = 0;
	            Map<Object, Number> playerStatistics = new HashMap<>();
	            statisticsMap.put(player.getName(), playerStatistics);
	            for (Game game : games) {
	                List<Statistic> statistics = statisticRepository.getByPlayerIdAndGameId(player.getId(), game.getId());
	                if (!statistics.isEmpty()) {
	                    gamesPlayed++;
	                    for (Statistic stat : statistics) {
	                        String key = String.valueOf(stat.getStatisticTypeId());
	                        Number playerStatistic = playerStatistics.get(key);
	                        if (stat.getFloatValue() != 0.0f) {
	                            playerStatistic = playerStatistic == null ? 0.0f : playerStatistic.floatValue();
	                            playerStatistic = playerStatistic.floatValue() + stat.getFloatValue();
	                        }
	                        if (stat.getNumberValue() != 0) {
	                            playerStatistic = playerStatistic == null ? 0 : playerStatistic.longValue();
	                            playerStatistic = playerStatistic.longValue() + stat.getNumberValue();
	                        }
	                        playerStatistics.put(key, playerStatistic);
	                    }
	                }
	            }
	            if (gamesPlayed > 0) {
	                for (Map.Entry<Object, Number> entry : playerStatistics.entrySet()) {
	                    float value = entry.getValue().floatValue();
	                    entry.setValue(value / gamesPlayed);
	                }
	            }
	        }
	
	        String response = objectMapper.writeValueAsString(statisticsMap);
	        exchange.getResponseHeaders().add("Content-Type", "application/json");
	        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
	        try (OutputStream os = exchange.getResponseBody()) {
	            os.write(response.getBytes(StandardCharsets.UTF_8));
	        }
        }
		log.debug("End getTeamSeasonStatistics: exchange={}, leagueName={}, teamName={}", exchange, leagueName, teamName);
    }
}  
