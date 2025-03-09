package gg.launchblock.sdk.events;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public class LaunchBlockProjectCreatedEvent extends LaunchBlockEvent {

	private final String projectName;
	private final UUID projectIdentifier;
	private final UUID environmentIdentifier;

	public LaunchBlockProjectCreatedEvent(JsonNode rawContent) {
		super(LaunchBlockEventType.PROJECT_CREATED, rawContent);

		// todo validation

		projectName = rawContent.get("projectName").asText();
		environmentIdentifier = UUID.fromString(rawContent.get("environmentIdentifier").asText());
		projectIdentifier = UUID.fromString(rawContent.get("projectIdentifier").asText());

	}


	public String getProjectName() {
		return projectName;
	}

	public UUID getProjectIdentifier() {
		return projectIdentifier;
	}

	public UUID getEnvironmentIdentifier() {
		return environmentIdentifier;
	}


}
