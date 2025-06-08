package com.skyhawk.league.model;

public class Statistic {

	private long playerId;
	private long gameId;
	private long statisticTypeId;
	private long  numberValue;
	private float floatValue;
	private long id;
	
	public Statistic(long playerId, long gameId, long statisticTypeId) {
		this.playerId = playerId;
		this.gameId = gameId;
		this.statisticTypeId = statisticTypeId;
	}

	// Getters
	public long getPlayerId() {
		return playerId;
	}

	public long getGameId() {
		return gameId;
	}

	public long getStatisticTypeId() {
		return statisticTypeId;
	}

	public long getNumberValue() {
		return numberValue;
	}

	public float getFloatValue() {
		return floatValue;
	}

	public long getId() {
		return id;
	}

	// Setters
	public void setNumberValue(long numberValue) {
		this.numberValue = numberValue;
	}

	public void setFloatValue(float floatValue) {
		this.floatValue = floatValue;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Statistic [playerId=" + playerId + ", gameId=" + gameId + ", statisticTypeId=" + statisticTypeId
				+ ", numberValue=" + numberValue + ", floatValue=" + floatValue + ", id=" + id + "]";
	}

}
