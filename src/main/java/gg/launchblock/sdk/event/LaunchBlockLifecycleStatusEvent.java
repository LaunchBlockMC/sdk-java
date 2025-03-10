package gg.launchblock.sdk.event;

import com.fasterxml.jackson.databind.JsonNode;
import gg.launchblock.sdk.exception.LaunchBlockSDKException;
import gg.launchblock.sdk.exception.LaunchBlockSDKExceptionType;
import gg.launchblock.sdk.util.LifecycleState;

import java.util.UUID;

public class LaunchBlockLifecycleStatusEvent extends LaunchBlockEvent {

	private final UUID lifecycleId;

	private final UUID projectIdentifier;

	private final UUID environmentIdentifier;

	private final LifecycleState oldState;

	private final LifecycleState newState;

	public LaunchBlockLifecycleStatusEvent(final String topic, final JsonNode rawContent) {
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

		try {
			oldState = LifecycleState.valueOf(rawContent.get("oldState").asText().toUpperCase());
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a %s with topic '%s' with an invalid old state".formatted(getClass().getSimpleName(), topic));
		}

		try {
			newState = LifecycleState.valueOf(rawContent.get("newState").asText().toUpperCase());
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a %s with topic '%s' with an invalid new state".formatted(getClass().getSimpleName(), topic));
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

	public LifecycleState getOldState() {
		return oldState;
	}

	public LifecycleState getNewState() {
		return newState;
	}
}
