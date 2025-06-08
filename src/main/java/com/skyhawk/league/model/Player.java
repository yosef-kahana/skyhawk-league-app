package com.skyhawk.league.model;

public class Player {
	private long teamId;
	private String name;
	private String description;
	private long id;

	public Player(long teamId, String name, String description) {
		this.teamId = teamId;
		this.name = name;
		this.description = description;
	}

	// Getter
	public long getTeamId() {
		return teamId;
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
	public void setTeamId(long teamId) {
		this.teamId = teamId;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Player [teamId=" + teamId + ", name=" + name + ", description=" + description + ", id=" + id + "]";
	}

}
