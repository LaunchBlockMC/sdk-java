package gg.launchblock.sdk.event;

import com.fasterxml.jackson.databind.JsonNode;
import gg.launchblock.sdk.exception.LaunchBlockSDKException;
import gg.launchblock.sdk.exception.LaunchBlockSDKExceptionType;

import java.util.UUID;

public class LaunchBlockProjectCreatedEvent extends LaunchBlockEvent {

	private final String projectName;

	private final UUID projectIdentifier;

	private final UUID environmentIdentifier;

	public LaunchBlockProjectCreatedEvent(final String topic, final JsonNode rawContent) {
		super(topic, rawContent);

		try {
			projectName = rawContent.get("projectName").asText();
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a LaunchBlockProjectCreatedEvent with topic '%s' with an invalid project name".formatted(topic));
		}

		try {
			environmentIdentifier = UUID.fromString(rawContent.get("environmentIdentifier").asText());
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a LaunchBlockProjectCreatedEvent with topic '%s' with an invalid environment identifier".formatted(topic));
		}

		try {
			projectIdentifier = UUID.fromString(rawContent.get("projectIdentifier").asText());
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a LaunchBlockProjectCreatedEvent with topic '%s' with an invalid project identifier".formatted(topic));
		}


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
