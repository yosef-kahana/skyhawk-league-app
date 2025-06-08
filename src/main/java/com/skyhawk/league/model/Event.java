package com.skyhawk.league.model;

import java.time.LocalTime;

public class Event {

	public enum EventType {
		GAME_START, GAME_END, TIMEOUT_START, TIMEOUT_END, INTERMISSION_START, INTERMISSION_END, PLAYER_START,
		PLAYER_END, PLAYER_ACTION
	}

	private long id;
	private long leagueId;
	private long gameId;
	private Long playerId; // Nullable
	private Long statisticTypeId; // Only set for PLAYER_ACTION
	private EventType type;
	private LocalTime eventTime;

	// Constructors
	public Event(long leagueId, long gameId, EventType type, LocalTime eventTime) {
		this.leagueId = leagueId;
		this.gameId = gameId;
		this.type = type;
		this.eventTime = eventTime;
	}

	public Event(long leagueId, long gameId, long playerId, EventType type, LocalTime eventTime) {
		this(leagueId, gameId, type, eventTime);
		this.playerId = playerId;
	}

	public Event(long leagueId, long gameId, long playerId, long statisticTypeId, LocalTime eventTime) {
		this(leagueId, gameId, playerId, EventType.PLAYER_ACTION, eventTime);
		this.statisticTypeId = statisticTypeId;
	}

	// Getters and setters
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getLeagueId() {
		return leagueId;
	}

	public long getGameId() {
		return gameId;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public Long getStatisticTypeId() {
		return statisticTypeId;
	}

	public EventType getType() {
		return type;
	}

	public LocalTime getEventTime() {
		return eventTime;
	}

	@Override
	public String toString() {
		return "Event [id=" + id + ", gameId=" + gameId + ", playerId=" + playerId + ", statisticTypeId="
				+ statisticTypeId + ", type=" + type + ", eventTime=" + eventTime + "]";
	}
}
