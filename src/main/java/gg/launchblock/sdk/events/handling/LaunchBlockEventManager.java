package gg.launchblock.sdk.events.handling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.launchblock.sdk.events.LaunchBlockEvent;
import gg.launchblock.sdk.events.LaunchBlockEventType;
import gg.launchblock.sdk.events.LaunchBlockProjectCreatedEvent;
import gg.launchblock.sdk.util.ImmutablePair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class LaunchBlockEventManager {

	private final HashMap<String, Class<? extends LaunchBlockEvent>> topicBindings = new HashMap<>();

	private final LaunchBlockKafkaConsumerConnection consumerConnection;

	private final List<LaunchBlockEventListener> listeners;

	@SafeVarargs
	public final <T extends LaunchBlockEventListener> void registerEvents(final T... newListeners) {
		listeners.addAll(Arrays.asList(newListeners));
	}

	public <T extends LaunchBlockEvent> void dispatchEvent(final T event) {
		// save all first to consider priority
		List<ImmutablePair<Method, LaunchBlockEventListener>> eventMethods = new ArrayList<>();

		getListeners().forEach(listener -> {

			for (Method declaredMethod : listener.getClass().getDeclaredMethods()) {
				if (!declaredMethod.isAnnotationPresent(LaunchBlockEventHandler.class)) {
					continue;
				}

				// validate parameter, only accepting (T event)
				if (declaredMethod.getParameterTypes().length != 1 || !(declaredMethod.getParameterTypes()[0].isInstance(event))) {
					continue;
				}

				declaredMethod.setAccessible(true);

				eventMethods.add(new ImmutablePair<>(declaredMethod, listener));
			}

		});

		eventMethods.stream().sorted((a,b) -> {
					// in order of decreasing priority
					return Integer.compare(b.getLeft().getAnnotation(LaunchBlockEventHandler.class).priority().level, a.getLeft().getAnnotation(LaunchBlockEventHandler.class).priority().level);
				})
				.forEach(pair -> {
					try {
						pair.getLeft().invoke(pair.getRight(), event);
					} catch (IllegalAccessException | InvocationTargetException e) {
						throw new RuntimeException(e); // todo handling
					}
				});


	}

	private void registerDefaultBindings() {
		topicBindings.put(LaunchBlockEventType.PROJECT_CREATED, LaunchBlockProjectCreatedEvent.class);
	}

	public LaunchBlockEventManager(String groupId) {
		this.listeners = new ArrayList<>();
		this.registerDefaultBindings();

		this.consumerConnection = new LaunchBlockKafkaConsumerConnection(groupId, this::handleKafkaMessage);

		this.consumerConnection.start();

	}

	public LaunchBlockEventManager() {
		this("lb-" + UUID.randomUUID());
	}

	private void handleKafkaMessage(final ConsumerRecord<String, String> message) {
		Logger log = LoggerFactory.getLogger(getClass());

		// find all event classes corresponding to this topic
		String topic = message.topic();
		log.info(topic);

		JsonNode rawContent;
		try {
			rawContent = new ObjectMapper().readTree(message.value());
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e); // todo exception handling
		}

		boolean definedTopic = topicBindings.containsKey(topic);
		Class<? extends LaunchBlockEvent> eventClazz = topicBindings.getOrDefault(topic, LaunchBlockEvent.class);


		final LaunchBlockEvent eventObject;
		if (definedTopic) {
			try {
				// each event class needs a public constructor accepting raw message json.
				eventObject = eventClazz.getDeclaredConstructor(JsonNode.class).newInstance(rawContent);
			} catch (NoSuchMethodException e) {
				// todo handle
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		} else {
			eventObject = new LaunchBlockEvent(topic, rawContent);
		}
		dispatchEvent(eventObject);
	}

	public LaunchBlockKafkaConsumerConnection getConsumerConnection() {
		return consumerConnection;
	}

	public List<LaunchBlockEventListener> getListeners() {
		return listeners;
	}

	/**
	 * Binds requested kafka topic to the given event class only if the topic has no previous binding.
	 * @return true if the topic is not already bound
	 */
	public boolean createTopicBinding(String topic, Class<? extends LaunchBlockEvent> eventType) {
		if(topicBindings.containsKey(topic)) return false;
		topicBindings.put(topic, eventType);
		return true;
	}

	/// @return True if the topic was previously bound
	public boolean removeTopicBinding(String topic) {
		return topicBindings.remove(topic) != null;
	}

	public Map<String, Class<? extends LaunchBlockEvent>> getTopicBindings() {
		return Collections.unmodifiableMap(topicBindings);
	}
}
