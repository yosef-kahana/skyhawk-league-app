package com.skyhawk.league.model;

public class Team {
	private long leagueId;
	private String name;
	private String description;
	private long id;

	public Team(long leagueId, String name, String description) {
		this.leagueId = leagueId;
		this.name = name;
		this.description = description;
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

	public long getId() {
		return id;
	}

	// Setter
	public void setLeagueId(long leagueId) {
		this.leagueId = leagueId;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Team [leagueId=" + leagueId + ", name=" + name + ", description=" + description + ", id=" + id + "]";
	}
}
