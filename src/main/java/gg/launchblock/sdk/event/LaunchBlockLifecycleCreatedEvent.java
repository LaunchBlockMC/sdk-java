package gg.launchblock.sdk.event;

import com.fasterxml.jackson.databind.JsonNode;
import gg.launchblock.sdk.exception.LaunchBlockSDKException;
import gg.launchblock.sdk.exception.LaunchBlockSDKExceptionType;

import java.util.UUID;

public class LaunchBlockLifecycleCreatedEvent extends LaunchBlockEvent {

	private final UUID lifecycleId;

	private final UUID projectIdentifier;

	private final UUID environmentIdentifier;

	public LaunchBlockLifecycleCreatedEvent(final String topic, final JsonNode rawContent) {
		super(topic, rawContent);

		try {
			environmentIdentifier = UUID.fromString(rawContent.get("environmentIdentifier").asText());
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a %s with topic '%s' with an invalid environment identifier".formatted(getClass().getSimpleName(), topic));
		}

		try {
			projectIdentifier = UUID.fromString(rawContent.get("projectIdentifier").asText());
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a %s with topic '%s' with an invalid project identifier".formatted(getClass().getSimpleName(), topic));
		}

		try {
			lifecycleId = UUID.fromString(rawContent.get("lifecycleId").asText());
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a %s with topic '%s' with an invalid lifecycle identifier".formatted(getClass().getSimpleName(), topic));
		}


	}

	public UUID getLifecycleId() {
		return lifecycleId;
	}

	public UUID getProjectIdentifier() {
		return projectIdentifier;
	}

	public UUID getEnvironmentIdentifier() {
		return environmentIdentifier;
	}


}
