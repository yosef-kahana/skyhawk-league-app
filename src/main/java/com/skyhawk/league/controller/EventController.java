package com.skyhawk.league.controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyhawk.league.model.Event;
import com.skyhawk.league.model.Event.EventType;
import com.skyhawk.league.model.Game;
import com.skyhawk.league.model.Game.GameStateEnum;
import com.skyhawk.league.model.Player;
import com.skyhawk.league.model.Statistic;
import com.skyhawk.league.model.StatisticType;
import com.skyhawk.league.repository.GameRepository;
import com.skyhawk.league.repository.PlayerRepository;
import com.skyhawk.league.repository.StatisticRepository;
import com.skyhawk.league.repository.StatisticTypeRepository;
import com.sun.net.httpserver.HttpExchange;

@RestController
@RequestMapping("/events")
public class EventController {

	private static final Logger log = LoggerFactory.getLogger(EventController.class);
	private final GameRepository gameRepository;
	private final PlayerRepository playerRepository;
	private final StatisticRepository statisticRepository;
	private final StatisticTypeRepository statisiticTypeRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public EventController(Connection connection) {
		this.gameRepository = new GameRepository(connection);
		this.statisticRepository = new StatisticRepository(connection);
		this.statisiticTypeRepository = new StatisticTypeRepository(connection);
		this.playerRepository = new PlayerRepository(connection);
	}

	public void handle(HttpExchange exchange) {
		log.info("Start handle: exchange={}", exchange);
		try {
			if ("POST".equals(exchange.getRequestMethod()) && exchange.getRequestURI().getPath().equals("/events")) {
				processEvent(exchange);
			} else {
				exchange.sendResponseHeaders(405, -1);
			}
		} catch (Exception e) {
			try {
				exchange.sendResponseHeaders(500, -1);
			} catch (Exception ignored) {
			}
		}
		log.info("End handle: exchange={}", exchange);
	}

	private void processEvent(HttpExchange exchange) throws Exception {
		log.debug("Start processEvent: exchange={}", exchange);
		try (InputStream is = exchange.getRequestBody()) {
			Event event = objectMapper.readValue(is, Event.class);
			Game game = gameRepository.getById(event.getGameId());
			if (game == null)
				throw new IllegalArgumentException("Game not found");

			handleEvents(event);
			if (event.getType() == Event.EventType.PLAYER_ACTION && event.getPlayerId() != null
					&& event.getStatisticTypeId() != null) {
				Statistic stat = new Statistic(event.getPlayerId(), game.getId(), event.getStatisticTypeId());
				statisticRepository.saveIfNotExists(stat);
			}

			String response = "{\"id\": " + event.getId() + "}";
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(response.getBytes(StandardCharsets.UTF_8));
			}
		}
		log.debug("End processEvent: exchange={}", exchange);
	}

	private void handleEvents(Event event) throws SQLException {
		log.debug("Start handleEvents: event={}", event);
		switch (event.getType()) {
		case GAME_START:
		case GAME_END:
		case INTERMISSION_START:
		case INTERMISSION_END:
		case TIMEOUT_START:
		case TIMEOUT_END:
			handleGameEvents(event);
			break;
		case PLAYER_ACTION:
		case PLAYER_START:
		case PLAYER_END:
			handlePlayerEvent(event);
			break;
		default:
			throw new IllegalArgumentException("Event action not supported: " + event);
		}
		log.debug("End handleEvents: event={}", event);
	}

	private void handleGameEvents(Event event) throws SQLException {
		log.debug("Start handleGameEvents: event={}", event);
		switch (event.getType()) {
		case GAME_START:
		case INTERMISSION_END:
		case TIMEOUT_END:
			handleGameStart(event);
			break;
		case GAME_END:
		case INTERMISSION_START:
		case TIMEOUT_START:
			handleGameEnd(event);
			break;
		default:
		}
		log.debug("Start handleGameEvents: event={}", event);
	}

	private void handleGameEnd(Event event) throws SQLException {
		log.debug("Start handleGameEnd: event={}", event);
		stopPlayers(event);
		String gameState = null;
		switch (event.getType()) {
		case GAME_END:
			gameState = GameStateEnum.End.name();
			gameRepository.updateEndTime(event.getGameId(), event.getEventTime());
			break;
		case INTERMISSION_START:
			gameState = GameStateEnum.Intermission.name();
			break;
		case TIMEOUT_START:
			gameState = GameStateEnum.Timeout.name();
			break;
		default:
		}
		if (gameState != null) {
			gameRepository.updateGameState(event.getGameId(), gameState);
		}
		log.debug("End handleGameEnd: event={}", event);
	}

	private void stopPlayers(Event event) throws SQLException {
		log.debug("Start stopPlayers: event={}", event);
		Game game = gameRepository.getById(event.getGameId());
		if (game != null) {
			StatisticType startTimeStatisticType = statisiticTypeRepository.getByName("startTime");
			long homeTeamId = game.getHomeTeamId();
			long visitorTeamId = game.getVisitorTeamId();
			List<Player> players = playerRepository.getByTeamId(homeTeamId);
			players.addAll(playerRepository.getByTeamId(visitorTeamId));
			for (Player player : players) {
				List<Statistic> statistics = statisticRepository.getByPlayerIdAndGameId(player.getId(), game.getId());
				for (Statistic statistic : statistics) {
					if (statistic.getStatisticTypeId() == startTimeStatisticType.getId()) {
						handlePlayerEnd(event);
					}
				}
			}
		}
		log.debug("End stopPlayers: event={}", event);
	}

	private void handleGameStart(Event event) throws SQLException {
		log.debug("Start handleGameStart: event={}", event);
		if (event.getType().equals(EventType.GAME_START) == true) {
			gameRepository.updateStartTime(event.getGameId(), event.getEventTime());
		}
		gameRepository.updateGameState(event.getGameId(), GameStateEnum.Running.name());
		log.debug("End handleGameStart: event={}", event);
	}

	private void handlePlayerEvent(Event event) throws SQLException {
		log.debug("Start handlePlayerEvent: event={}", event);
		if (event.getPlayerId() != null) {
			switch (event.getType()) {
			case PLAYER_START:
				handlePlayerStart(event);
				break;
			case PLAYER_END:
				handlePlayerEnd(event);
				break;
			case PLAYER_ACTION:
				handlePlayerAction(event);
				break;
			default:
			}
		} else {
			throw new IllegalArgumentException("Event does not contain playerId");
		}
		log.debug("End handlePlayerEvent: event={}", event);
	}

	private void handlePlayerAction(Event event) throws SQLException {
		log.debug("Start handlePlayerAction: event={}", event);
		Statistic currentStatistic = null;
		List<Statistic> statistics = statisticRepository.getByPlayerIdAndGameId(event.getPlayerId(), event.getGameId());
		for (Statistic statistic : statistics) {
			if (statistic.getStatisticTypeId() == event.getStatisticTypeId()) {
				currentStatistic = statistic;
			}
		}
		if (currentStatistic == null) {
			Statistic statistic = new Statistic(event.getPlayerId(), event.getGameId(), event.getStatisticTypeId());
			statistic.setNumberValue(1);
			statisticRepository.saveIfNotExists(statistic);
		} else {
			StatisticType statisticType = statisiticTypeRepository.getById(event.getStatisticTypeId());
			Integer maxRange = statisticType.getMaxValue();
			Integer minRange = statisticType.getMinValue();
			currentStatistic.setNumberValue(currentStatistic.getNumberValue() + 1);
			statisticRepository.updateNumberValue(event.getPlayerId(), event.getGameId(), event.getStatisticTypeId(),
					currentStatistic.getNumberValue());
			if (maxRange != null && maxRange <= currentStatistic.getNumberValue()) {
				handlePlayerEnd(event);
			}
			if (minRange != null && minRange >= currentStatistic.getNumberValue()) {
				handlePlayerEnd(event);
			}
		}
		log.debug("End handlePlayerAction: event={}", event);
	}

	private void handlePlayerEnd(Event event) throws SQLException {
		log.debug("Start handlePlayerStart: event={}", event);
		Long playTimeStart = null;
		Float playTime = 0.0f;
		StatisticType startTimeStatisticType = statisiticTypeRepository.getByName("startTime");
		StatisticType playTimeStatisticType = statisiticTypeRepository.getByName("playTime");
		List<Statistic> statistics = statisticRepository.getByPlayerIdAndGameId(event.getPlayerId(), event.getGameId());
		for (Statistic statistic : statistics) {
			if (statistic.getStatisticTypeId() == startTimeStatisticType.getId()) {
				playTimeStart = statistic.getNumberValue();
			} else if (statistic.getStatisticTypeId() == playTimeStatisticType.getId()) {
				playTime = statistic.getFloatValue();
			}
		}
		if (playTimeStart != null) {
			long playInSeconds = event.getEventTime().toSecondOfDay() - playTimeStart;
			float currentPlayTime = mergeFloatTime(playTime, playInSeconds);
			if (playTime == 0.0f) {
				// player first statistic
				Statistic statistic = new Statistic(event.getPlayerId(), event.getGameId(),
						playTimeStatisticType.getId());
				statistic.setFloatValue(currentPlayTime);
				statisticRepository.saveIfNotExists(statistic);
			} else {
				statisticRepository.updateFloatValue(event.getPlayerId(), event.getGameId(),
						playTimeStatisticType.getId(), currentPlayTime);
			}
			statisticRepository.removeIfExist(event.getPlayerId(), event.getGameId(), startTimeStatisticType.getId());
		} else {
			log.warn("Player not playing");
		}
		log.debug("End handlePlayerStart: event={}", event);
	}

	private float mergeFloatTime(float playTime, long playInSeconds) {
		log.debug("Start mergeFloatTime: playTime={}, playInSeconds={}", playTime, playInSeconds);
		// Split the float into minutes and seconds
		int minutes = (int) playTime;
		float fractional = playTime - minutes;
		int seconds = Math.round(fractional * 100); // e.g., 0.30 -> 30 seconds

		// Convert total to seconds and add
		int totalSeconds = minutes * 60 + seconds + (int) playInSeconds;

		// Convert back to minutes and seconds
		int resultMinutes = totalSeconds / 60;
		int resultSeconds = totalSeconds % 60;

		// Reconstruct float: e.g., 1.30 = 1 min 30 sec
		float newPlayTime = resultMinutes + resultSeconds / 100f;
		log.debug("End mergeFloatTime: playTime={}, playInSeconds={}, return={}", playTime, playInSeconds, newPlayTime);
		return newPlayTime;
	}

	private void handlePlayerStart(Event event) throws SQLException {
		log.debug("Start handlePlayerStart: event={}", event);
		Long playTimeStart = null;
		StatisticType startTimeStatisticType = statisiticTypeRepository.getByName("startTime");
		List<Statistic> statistics = statisticRepository.getByPlayerIdAndGameId(event.getPlayerId(), event.getGameId());
		for (Statistic statistic : statistics) {
			if (statistic.getStatisticTypeId() == startTimeStatisticType.getId()) {
				playTimeStart = statistic.getNumberValue();
				log.warn("Player already playing");
			}
		}
		if (playTimeStart == null) {
			statisticRepository.updateNumberValue(event.getPlayerId(), event.getGameId(),
					startTimeStatisticType.getId(), event.getEventTime().toSecondOfDay());
		}
		log.debug("End handlePlayerStart: event={}", event);
	}
}
