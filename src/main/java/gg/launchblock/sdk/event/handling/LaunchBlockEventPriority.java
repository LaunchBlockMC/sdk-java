package gg.launchblock.sdk.event.handling;

public enum LaunchBlockEventPriority {
	HIGHEST(2),
	HIGH(1),
	DEFAULT(0),
	LOW(-1),
	LOWEST(-2);

	private final int level; // higher = more priority

	LaunchBlockEventPriority(int level) {
		this.level = level;
	}

	public int getLevel() {
		return level;
	}
}
