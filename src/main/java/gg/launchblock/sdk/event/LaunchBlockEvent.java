package gg.launchblock.sdk.event;

import com.fasterxml.jackson.databind.JsonNode;
import gg.launchblock.sdk.event.handling.LaunchBlockEventManager;
import gg.launchblock.sdk.exception.LaunchBlockSDKException;
import gg.launchblock.sdk.exception.LaunchBlockSDKExceptionType;

import java.util.UUID;

public class LaunchBlockEvent {

	private final JsonNode rawContent;

	private final String topic;

	private final UUID workspaceId;

	/**
	 * @param topic      kafka topic associated with this event
	 * @param rawContent kafka value associated with this event, as json
	 */
	public LaunchBlockEvent(final String topic, final JsonNode rawContent) {
		this.topic = topic;
		this.rawContent = rawContent;

		try {
			this.workspaceId = UUID.fromString(rawContent.get("workspaceIdentifier").asText());
		} catch (Exception e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Attempted to create a LaunchBlockEvent with topic '%s' without a valid workspace identifier attached".formatted(topic));
		}
	}

	public String getTopic() {
		return topic;
	}

	public JsonNode getRawContent() {
		return rawContent;
	}

	public UUID getWorkspaceId() {
		return workspaceId;
	}

	public void callEvent(final LaunchBlockEventManager manager) {
		manager.dispatchEvent(this);
	}

}
