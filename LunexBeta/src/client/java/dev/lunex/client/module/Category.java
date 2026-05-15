package dev.lunex.client.module;

public enum Category {
	COMBAT("Combat", "C"),
	MOVEMENT("Movement", "M"),
	RENDER("Render", "R"),
	PLAYER("Player", "P"),
	MISC("Misc", "X"),
	THEMES("Themes", "T"),
	CONFIGS("Configs", "S");

	private final String title;
	private final String icon;

	Category(String title, String icon) {
		this.title = title;
		this.icon = icon;
	}

	public String getTitle() {
		return title;
	}

	public String getIcon() {
		return icon;
	}
}
