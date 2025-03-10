package gg.launchblock.sdk.event;

import com.fasterxml.jackson.databind.JsonNode;
import gg.launchblock.sdk.exception.LaunchBlockSDKException;
import gg.launchblock.sdk.exception.LaunchBlockSDKExceptionType;

import java.util.UUID;

public class LaunchBlockProjectDeletedEvent extends LaunchBlockEvent {

	private final String projectName;

	private final UUID projectId;

	private final UUID environmentId;

	public LaunchBlockProjectDeletedEvent(final String topic, final JsonNode rawContent) {
		super(topic, rawContent);

		try {
			projectName = rawContent.get("projectName").asText();
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a %s with topic '%s' with an invalid project name".formatted(getClass().getSimpleName(), topic));
		}

		try {
			environmentId = UUID.fromString(rawContent.get("environmentIdentifier").asText());
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a %s with topic '%s' with an invalid environment identifier".formatted(getClass().getSimpleName(), topic));
		}

		try {
			projectId = UUID.fromString(rawContent.get("projectIdentifier").asText());
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a %s with topic '%s' with an invalid project identifier".formatted(getClass().getSimpleName(), topic));
		}


	}

	public String getProjectName() {
		return projectName;
	}

	public UUID getProjectId() {
		return projectId;
	}

	public UUID getEnvironmentId() {
		return environmentId;
	}


}
