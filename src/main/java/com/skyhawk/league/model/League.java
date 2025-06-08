package com.skyhawk.league.model;

public class League {
	private String name;
	private String description;
	private long id;

	public League(String name, String description) {
		this.name = name;
		this.description = description;
	}

	// Getters
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
	public void setId(long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "League [name=" + name + ", description=" + description + ", id=" + id + "]";
	}
}
