package gg.launchblock.sdk.emitter;

import com.fasterxml.jackson.databind.JsonNode;
import gg.launchblock.sdk.exception.LaunchBlockSDKException;
import gg.launchblock.sdk.exception.LaunchBlockSDKExceptionType;
import gg.launchblock.sdk.util.ImmutablePair;
import gg.launchblock.sdk.util.KafkaUtil;
import gg.launchblock.sdk.util.LaunchBlockSDKConstants;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.*;

public class LaunchBlockMessageEmitter {

	/**
	 * Creates a new emitter, sends the requested message, and closes the emitter. <br>
	 * Only recommended when sending a very low volume of messages due to
	 * performance impact of creating a new producer for every message. <br>
	 *  Consider storing an instance of {@link LaunchBlockMessageEmitter} to use instead.
	 * @see	#quickSendAll(Collection)
	 */
	public static void quickSend(final String topic, final JsonNode value) {
		quickSendAll(Collections.singletonList(new ImmutablePair<>(topic, value)));
	}

	/**
	 * Creates a new emitter, sends the requested messages, and closes the emitter. <br>
	 * Only recommended when sending a very low volume of messages due to
	 * performance impact of creating a new producer for every message collection. <br>
	 * Consider storing an instance of {@link LaunchBlockMessageEmitter} to use instead.
	 */
	public static void quickSendAll(final Collection<ImmutablePair<String, JsonNode>> messages) {
		LaunchBlockMessageEmitter emitter = new LaunchBlockMessageEmitter();
		emitter.sendAll(false, messages); // closing flushes regardless
		emitter.close();
	}

	private final KafkaProducer<String, String> kafkaProducer;

	private boolean active;

	public LaunchBlockMessageEmitter() {
		this.active = true;
		this.kafkaProducer = createProducer();
	}

	private KafkaProducer<String, String> createProducer() {
		if(!KafkaUtil.isKafkaRunning()) {
			throw new LaunchBlockSDKException(LaunchBlockSDKExceptionType.KAFKA,
					"Could not connect to kafka. Make sure your kafka instance is enabled before attempting to emit messages");
		}

		// for further reading, consult
		// https://kafka.apache.org/23/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html

		final Properties props = new Properties();
		props.put("bootstrap.servers", LaunchBlockSDKConstants.KAFKA_HOSTNAME+":"+LaunchBlockSDKConstants.KAFKA_PORT);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("retries", "1");

		// all acknowledgments -> generally guarantee writes are successful (slightly worse latency than acks=1 or 0)
		props.put("acks", "all");

		return new KafkaProducer<>(props);
	}

	/**
	 * Uses {@link LaunchBlockMessageEmitter#sendAll(boolean, Collection)} with a one element collection
	 */
	public void send(final String topic, final JsonNode value, final boolean flush) {
		this.sendAll(flush, Collections.singletonList(new ImmutablePair<>(topic, value)));
	}


	/**
	 * Uses {@link LaunchBlockMessageEmitter#send(String, JsonNode, boolean)} with flushing enabled.
	 */
	public void send(final String topic, final JsonNode value) {
		send(topic, value, true);
	}

	/**
	 * Emits kafka messages for each of the given topic-value pairs
	 * @param flush whether to send messages immediately
	 * @param messages each pair contains a String representing kafka topic and JsonNode representing value
	 */
	public void sendAll(final boolean flush, final Collection<ImmutablePair<String, JsonNode>> messages) {
		if(isClosed()) {
			throw new LaunchBlockSDKException(LaunchBlockSDKExceptionType.KAFKA, "Attempted to use a closed message emitter object");
		}

		for (ImmutablePair<String, JsonNode> entry : messages) {
			// we use KafkaProducer's send() and flush() instead of its transaction system as atomicity is not crucial,
			// and the slightly higher performance of this method is a nice bonus

			getKafkaProducer().send(new ProducerRecord<>(entry.getLeft(), entry.getRight().toString()),
					// callback when the send action is eventually executed
					(recordMetadata, e) -> {
						if(e == null) return;
						throw new LaunchBlockSDKException(e, LaunchBlockSDKExceptionType.KAFKA, "Failed to emit a message in topic '%s'; '%s'"
								.formatted(entry.getLeft(), entry.getRight().toString()));
					});

		}

		if(flush) {
			getKafkaProducer().flush();
		}
	}

	/**
	 * Flushes buffer and marks this emitter object as inactive,
	 * so that it may not be used to send any further messages
	 */
	public void close() {
		this.active = false;
		getKafkaProducer().close(); // flushes and closes
	}

	/// Flushes buffer of the underlying producer
	public void flush() {
		getKafkaProducer().flush();
	}

	public boolean isClosed() {
		return !active;
	}

	protected KafkaProducer<String, String> getKafkaProducer() {
		return this.kafkaProducer;
	}

}
