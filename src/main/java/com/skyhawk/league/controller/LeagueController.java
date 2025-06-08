package com.skyhawk.league.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhawk.league.model.League;
import com.skyhawk.league.repository.LeagueRepository;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeagueController {
	private static final Logger log = LoggerFactory.getLogger(LeagueController.class);

    private final LeagueRepository leagueRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LeagueController(Connection connection) {
        this.leagueRepository = new LeagueRepository(connection);
    }

    public void handle(HttpExchange exchange) {
		log.info("Start handle: exchange={}", exchange);
       try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method)) {
                if (path.matches("/league/?")) {
                    getAllLeagues(exchange);
                } else if (path.matches("/league/[^/]+/?")) {
                    String name = path.substring(path.lastIndexOf('/') + 1);
                    getLeagueByName(exchange, name);
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            } else if ("POST".equals(method) && path.equals("/league")) {
                createLeague(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }

        } catch (Exception e) {
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {}
        }
		log.info("End handle: exchange={}", exchange);
    }

    private void getAllLeagues(HttpExchange exchange) throws Exception {
		log.debug("Start getAllLeagues: exchange={}", exchange);
        List<League> leagues = leagueRepository.getAll();
        String response = objectMapper.writeValueAsString(leagues);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
		log.debug("End getAllLeagues: exchange={}", exchange);
    }

    private void getLeagueByName(HttpExchange exchange, String name) throws SQLException, IOException {
		log.debug("Start getLeagueByName: exchange={}, name={}", exchange, name);
        League league = leagueRepository.getByName(name);
        if (league == null) {
            exchange.sendResponseHeaders(404, -1);
        }
        else {
	        try {
	            String response = objectMapper.writeValueAsString(league);
	            exchange.getResponseHeaders().add("Content-Type", "application/json");
	            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
	            try (OutputStream os = exchange.getResponseBody()) {
	                os.write(response.getBytes(StandardCharsets.UTF_8));
	            }
	        } catch (Exception e) {
	            exchange.sendResponseHeaders(500, -1);
	        }
        }
		log.debug("End getLeagueByName: exchange={}, name={}", exchange, name);
    }

    private void createLeague(HttpExchange exchange) throws Exception {
		log.debug("Start createLeague: exchange={}", exchange);
        try (InputStream is = exchange.getRequestBody()) {
            League league = objectMapper.readValue(is, League.class);
            long id = leagueRepository.saveIfNotExists(league);
            String response = "{\"id\": " + id + "}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
		log.debug("End createLeague: exchange={}", exchange);
    }
}  
