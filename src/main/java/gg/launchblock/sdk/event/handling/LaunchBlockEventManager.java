package gg.launchblock.sdk.event.handling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.launchblock.sdk.event.*;
import gg.launchblock.sdk.exception.LaunchBlockSDKException;
import gg.launchblock.sdk.exception.LaunchBlockSDKExceptionType;
import gg.launchblock.sdk.util.ImmutablePair;
import gg.launchblock.sdk.util.LaunchBlockSDKConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class LaunchBlockEventManager {

	/// bindings of kafka topics to event classes to identify which events to create and call after a kafka message
	private final HashMap<String, Class<? extends LaunchBlockEvent>> topicBindings = new HashMap<>();

	private final LaunchBlockKafkaConsumerConnection consumerConnection;

	private final List<LaunchBlockEventListener> listeners;

	/// whether the kafka connection hasn't been closed
	private boolean active;

	@SafeVarargs
	public final <T extends LaunchBlockEventListener> void registerEvents(final T... newListeners) {
		listeners.addAll(Arrays.asList(newListeners));
	}

	public <T extends LaunchBlockEvent> void dispatchEvent(final T event) {
		// two passes to respect priority
		List<ImmutablePair<Method, LaunchBlockEventListener>> eventMethods = new ArrayList<>();

		getListeners().forEach(listener -> {
			for (Method declaredMethod : listener.getClass().getDeclaredMethods()) {
				if (!declaredMethod.isAnnotationPresent(LaunchBlockEventHandler.class)) {
					continue;
				}

				if (declaredMethod.getParameterTypes().length != 1 ||
						!(LaunchBlockEvent.class.isAssignableFrom(declaredMethod.getParameterTypes()[0]))) {
					LaunchBlockSDKConstants.JAVA_SDK_LOGGER.warn("Event Handler '{}' of listener '{}' has invalid parameters to dispatch to, ignoring",
							declaredMethod.getName(), listener.getClass().getSimpleName());
				}

				// validate handler parameter, only accepting (T event)
				if (!(declaredMethod.getParameterTypes()[0].isInstance(event))) {
					continue;
				}

				declaredMethod.setAccessible(true);

				eventMethods.add(new ImmutablePair<>(declaredMethod, listener));
			}

		});

		// consider priority
		eventMethods.stream().sorted((a, b) -> {
					// in order of decreasing priority
					return Integer.compare(b.getLeft().getAnnotation(LaunchBlockEventHandler.class).priority().getLevel(),
							a.getLeft().getAnnotation(LaunchBlockEventHandler.class).priority().getLevel());
				})
				.forEach(pair -> {
					try {
						pair.getLeft().invoke(pair.getRight(), event);
					} catch (IllegalAccessException | InvocationTargetException e) {
						throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
								"Failed to invoke event handler %s in listener %s"
										.formatted(pair.getLeft().getName(), pair.getRight().getClass().getSimpleName()));
					}
				});


	}

	/// Creates kafka topic bindings for all default LaunchBlock events
	private void registerDefaultBindings() {
		topicBindings.put(LaunchBlockEventType.PROJECT_CREATED, LaunchBlockProjectCreatedEvent.class);
		topicBindings.put(LaunchBlockEventType.PROJECT_DELETED, LaunchBlockProjectDeletedEvent.class);
		topicBindings.put(LaunchBlockEventType.LIFECYCLE_CREATED, LaunchBlockLifecycleCreatedEvent.class);
		topicBindings.put(LaunchBlockEventType.LIFECYCLE_STATUS, LaunchBlockLifecycleStatusEvent.class);
	}

	/**
	 * @param groupId Internal group id of kafka consumer; if two managers share a group id, kafka events are distributed across them.
	 */
	public LaunchBlockEventManager(final String groupId) {
		this.listeners = new ArrayList<>();
		this.registerDefaultBindings();

		this.consumerConnection = new LaunchBlockKafkaConsumerConnection(groupId, this::handleKafkaMessage);
		this.active = true;

		this.consumerConnection.start();

	}

	public LaunchBlockEventManager() {
		this("lb-" + UUID.randomUUID());
	}

	/// Creates and dispatches required events when a `message` is received from kafka
	private void handleKafkaMessage(final ConsumerRecord<String, String> message) {
		// need to create and call all events corresponding to this topic
		String kafkaTopic = message.topic();

		JsonNode rawContent;
		try {
			rawContent = new ObjectMapper().readTree(message.value());
		} catch (JsonProcessingException e) {
			throw new LaunchBlockSDKException(LaunchBlockSDKExceptionType.INVALID_JSON,
					"Could not process kafka message of topic '%s' into a json node: \"%s\"".formatted(kafkaTopic, message.value()));
		}

		// send out a plain LaunchBlockEvent if this topic isn't bound
		Class<? extends LaunchBlockEvent> eventClazz = topicBindings.getOrDefault(kafkaTopic, LaunchBlockEvent.class);

		final LaunchBlockEvent eventObject;

		try {

			eventObject = eventClazz.getDeclaredConstructor(String.class, JsonNode.class).newInstance(kafkaTopic, rawContent);

		} catch (NoSuchMethodException e) {
			throw new LaunchBlockSDKException(LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Failed to construct event object for '%s'. Make sure this event has a constructor accepting parameters (String, JsonNode)"
							.formatted(eventClazz.getSimpleName()));
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.EVENT_HANDLING,
					"Failed to construct event object for %s".formatted(eventClazz.getSimpleName()));
		}

		this.dispatchEvent(eventObject);
	}

	protected LaunchBlockKafkaConsumerConnection getConsumerConnection() {
		return consumerConnection;
	}

	public List<LaunchBlockEventListener> getListeners() {
		return listeners;
	}

	/**
	 * Binds requested kafka topic to the given event class only if the topic has no previous binding
	 *
	 * @return true if the topic is not already bound
	 */
	public boolean createTopicBinding(final String topic, Class<? extends LaunchBlockEvent> eventType) {
		if (topicBindings.containsKey(topic)) return false;
		topicBindings.put(topic, eventType);
		return true;
	}

	/// @return true if the topic was previously bound
	public boolean removeTopicBinding(final String topic) {
		return topicBindings.remove(topic) != null;
	}

	public Map<String, Class<? extends LaunchBlockEvent>> getTopicBindings() {
		return Collections.unmodifiableMap(topicBindings);
	}

	/**
	 * 	Permanently closes the held consumer connection and makes this event manager object defunct.
	 * 	This method should be called when the manager's operation is no longer required as the kafka connection is not otherwise severed
 	 */
	public void close() {
		consumerConnection.close();
		active = false;
	}

	public boolean isClosed() {
		return !active;
	}
}
