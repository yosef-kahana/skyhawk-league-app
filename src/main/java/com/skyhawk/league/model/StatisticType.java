package com.skyhawk.league.model;

public class StatisticType {
	private long leagueId;
	private String name;
	private String description;
	private StatTypeEnum type;
	private Integer minValue;
	private Integer maxValue;
	private long id;
	
	public enum StatTypeEnum {
		NUMBER, 
		FLOAT
	};
	
	public StatisticType(long leagueId, String name, String description, StatTypeEnum type, Integer minValue, Integer maxValue) {
		this.leagueId = leagueId;
		this.name = name;
		this.description = description;
		this.type = type;
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	
	public StatisticType(long leagueId, String name, String description, StatTypeEnum type) {
		this(leagueId, name, description, type, null, null);
	}

	public StatisticType(long leagueId, String name, String description) {
		this(leagueId, name, description, StatTypeEnum.NUMBER, null, null);
	}

	// Getters
	public long getLeagueId() {
		return leagueId;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public StatTypeEnum getType() {
		return type;
	}

	public Integer getMinValue() {
		return minValue;
	}

	public Integer getMaxValue() {
		return maxValue;
	}

	public long getId() {
		return id;
	}

	// Setters
	public void setLeagueId(long leagueId) {
		this.leagueId = leagueId;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "StatisiticType [leagueId=" + leagueId + ", name=" + name + ", description=" + description + ", type="
				+ type + ", minValue=" + minValue + ", maxValue=" + maxValue + ", id=" + id + "]";
	}
	
	
}
