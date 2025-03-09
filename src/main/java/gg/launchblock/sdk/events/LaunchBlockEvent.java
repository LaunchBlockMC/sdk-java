package gg.launchblock.sdk.events;

import com.fasterxml.jackson.databind.JsonNode;
import gg.launchblock.sdk.events.handling.LaunchBlockEventManager;

import java.util.UUID;

public class LaunchBlockEvent {

	private final String name;
	private final JsonNode rawContent;

	private final UUID workspaceId;

	public LaunchBlockEvent(String name, JsonNode rawContent) {
		this.name = name;
		this.rawContent = rawContent;

		// todo lb-exceptions (and more detailed exceptions for each)
		RuntimeException workspaceException = new RuntimeException("Attempted to create a LaunchBlockEvent without a valid workspace id attached. (%s)".formatted(name));

		if (!rawContent.hasNonNull("workspaceIdentifier")) {
			throw workspaceException;
		}

		JsonNode workspaceIdNode = rawContent.get("workspaceIdentifier");
		if (!workspaceIdNode.isTextual()) {
			throw workspaceException;
		}

		try {
			this.workspaceId = UUID.fromString(workspaceIdNode.asText());
		} catch (IllegalArgumentException u) {
			throw workspaceException;
		}

	}

	public String getName() {
		return name;
	}

	public JsonNode getRawContent() {
		return rawContent;
	}

	public UUID getWorkspaceId() {
		return workspaceId;
	}

	public void callEvent(LaunchBlockEventManager manager) {
		manager.dispatchEvent(this);
	}

	public static <T extends LaunchBlockEvent> T fromRawContent(JsonNode rawContent) {
		throw new UnsupportedOperationException();
	}


}
