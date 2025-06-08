package com.skyhawk.league.controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhawk.league.model.Game;
import com.skyhawk.league.model.League;
import com.skyhawk.league.repository.GameRepository;
import com.skyhawk.league.repository.LeagueRepository;
import com.sun.net.httpserver.HttpExchange;

public class GameController {
	private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final GameRepository gameRepository;
    private final LeagueRepository leagueRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GameController(Connection connection) {
        this.gameRepository = new GameRepository(connection);
        this.leagueRepository = new LeagueRepository(connection);
    }

    public void handle(HttpExchange exchange) {
		log.info("Start handle: exchange={}", exchange);
    try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");

            if (segments.length >= 4 && "league".equals(segments[1])) {
                String leagueName = segments[2];

                if ("games".equals(segments[3])) {
                    if ("GET".equals(method) && segments.length == 4) {
                        getGamesByLeague(exchange, leagueName);
                        return;
                    } else if ("POST".equals(method) && segments.length == 4) {
                        createGame(exchange, leagueName);
                        return;
                    } else if ("GET".equals(method) && segments.length == 5) {
                        String gameName = segments[4];
                        getGameByName(exchange, leagueName, gameName);
                        return;
                    }
                }
            }
            exchange.sendResponseHeaders(404, -1);
        } catch (Exception e) {
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {}
        }
	log.info("End handle: exchange={}", exchange);
    }

    private void getGamesByLeague(HttpExchange exchange, String leagueName) throws Exception {
		log.debug("Start getGamesByLeague: exchange={}, leagueName={}", exchange, leagueName);
        League league = leagueRepository.getByName(leagueName);
        List<Game> games = gameRepository.getAll().stream()
                .filter(game -> game.getLeagueId() == league.getId())
                .collect(Collectors.toList());
        String response = objectMapper.writeValueAsString(games);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
		log.debug("End getGamesByLeague: exchange={}, leagueName={}", exchange, leagueName);
    }

    private void getGameByName(HttpExchange exchange, String leagueName, String gameName) throws Exception {
		log.debug("Start getGameByName: exchange={}, leagueName={}, gameName={}", exchange, leagueName, gameName);
        League league = leagueRepository.getByName(leagueName);
        Game game = gameRepository.getByName(gameName);
        if (game == null || game.getLeagueId() != league.getId()) {
            exchange.sendResponseHeaders(404, -1);
        }
        else {
	        String response = objectMapper.writeValueAsString(game);
	        exchange.getResponseHeaders().add("Content-Type", "application/json");
	        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
	        try (OutputStream os = exchange.getResponseBody()) {
	            os.write(response.getBytes(StandardCharsets.UTF_8));
	        }
        }
		log.debug("End getGameByName: exchange={}, leagueName={}, gameName={}", exchange, leagueName, gameName);
    }

    private void createGame(HttpExchange exchange, String leagueName) throws Exception {
		log.debug("Start createGame: exchange={}, leagueName={}", exchange, leagueName);
        League league = leagueRepository.getByName(leagueName);
        try (InputStream is = exchange.getRequestBody()) {
            Game game = objectMapper.readValue(is, Game.class);
            game.setId(0);
            game.setLeagueId(league.getId());
            long id = gameRepository.saveIfNotExists(game);
            String response = "{\"id\": " + id + "}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
		log.debug("End createGame: exchange={}, leagueName={}", exchange, leagueName);
    }
}  
