package com.skyhawk.league.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Game {
	private String name;
	private String description;
	private long leagueId;
	private long homeTeamId;
	private long visitorTeamId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
	private GameStateEnum gameState;
	private long id;
	
	public enum GameStateEnum {
		Running,
		Intermission,
		Timeout,
		End,
		NotStarted,
	}

	public Game(long leagueId, String name, String description, long homeTeamId, long visitorTeamId, LocalDate date) {
		this.leagueId = leagueId;
		this.name = name;
		this.description = description;
		this.homeTeamId = homeTeamId;
		this.visitorTeamId = visitorTeamId;
		this.date= date;
	}

	// Getters
	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public long getLeagueId() {
		return leagueId;
	}

	public long getHomeTeamId() {
		return homeTeamId;
	}

	public long getVisitorTeamId() {
		return visitorTeamId;
	}

	public LocalDate getDate() {
		return date;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public GameStateEnum getGameState() {
		return gameState;
	}

	public long getId() {
		return id;
	}

	// Setters
	public void setLeagueId(long leagueId) {
		this.leagueId = leagueId;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public void setGameState(GameStateEnum gameState) {
		this.gameState = gameState;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Game [name=" + name + ", description=" + description + ", leagueId=" + leagueId + ", homeTeamId="
				+ homeTeamId + ", visitorTeamId=" + visitorTeamId + ", date=" + date + ", startTime=" + startTime
				+ ", endTime=" + endTime + ", gameState=" + gameState + ", id=" + id + "]";
	}

	
	

}
