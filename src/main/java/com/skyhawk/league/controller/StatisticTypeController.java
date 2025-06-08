package com.skyhawk.league.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhawk.league.model.League;
import com.skyhawk.league.model.StatisticType;
import com.skyhawk.league.repository.LeagueRepository;
import com.skyhawk.league.repository.StatisticTypeRepository;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticTypeController {
	private static final Logger log = LoggerFactory.getLogger(StatisticTypeController.class);

    private final StatisticTypeRepository statisticTypeRepository;
    private final LeagueRepository leagueRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatisticTypeController(Connection connection) {
        this.statisticTypeRepository = new StatisticTypeRepository(connection);
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

                if ("statistic-types".equals(segments[3])) {
                    if ("GET".equals(method) && segments.length == 4) {
                        getStatisticTypesByLeague(exchange, leagueName);
                        return;
                    } else if ("POST".equals(method) && segments.length == 4) {
                        createStatisticType(exchange, leagueName);
                        return;
                    } else if ("GET".equals(method) && segments.length == 5) {
                        String typeName = segments[4];
                        getStatisticTypeByName(exchange, leagueName, typeName);
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

    private void getStatisticTypesByLeague(HttpExchange exchange, String leagueName) throws Exception {
		log.debug("Start getStatisticTypesByLeague: exchange={}, leagueName={}", exchange, leagueName);
        League league = leagueRepository.getByName(leagueName);
        List<StatisticType> types = statisticTypeRepository.getAll().stream()
                .filter(type -> type.getLeagueId() == league.getId())
                .collect(Collectors.toList());
        String response = objectMapper.writeValueAsString(types);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
		log.debug("End getStatisticTypesByLeague: exchange={}, leagueName={}", exchange, leagueName);
    }

    private void getStatisticTypeByName(HttpExchange exchange, String leagueName, String typeName) throws Exception {
		log.debug("Start getStatisticTypeByName: exchange={}, leagueName={}, typeName={}", exchange, leagueName, typeName);
        League league = leagueRepository.getByName(leagueName);
        StatisticType type = statisticTypeRepository.getByName(typeName);
        if (type == null || type.getLeagueId() != league.getId()) {
            exchange.sendResponseHeaders(404, -1);
        }
        else {
	        String response = objectMapper.writeValueAsString(type);
	        exchange.getResponseHeaders().add("Content-Type", "application/json");
	        exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
	        try (OutputStream os = exchange.getResponseBody()) {
	            os.write(response.getBytes(StandardCharsets.UTF_8));
	        }
        }
		log.debug("End getStatisticTypeByName: exchange={}, leagueName={}, typeName={}", exchange, leagueName, typeName);
    }

    private void createStatisticType(HttpExchange exchange, String leagueName) throws Exception {
		log.debug("Start createStatisticType: exchange={}, leagueName={}", exchange, leagueName);
        League league = leagueRepository.getByName(leagueName);
        try (InputStream is = exchange.getRequestBody()) {
            StatisticType type = objectMapper.readValue(is, StatisticType.class);
            type.setId(0);
            type.setLeagueId(league.getId());
            long id = statisticTypeRepository.saveIfNotExists(type);
            String response = "{\"id\": " + id + "}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
		log.debug("End createStatisticType: exchange={}, leagueName={}", exchange, leagueName);
    }
}  
