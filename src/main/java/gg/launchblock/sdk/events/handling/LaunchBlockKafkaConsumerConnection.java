package gg.launchblock.sdk.events.handling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class LaunchBlockKafkaConsumerConnection {

//		public static final String HOSTNAME = "host.docker.internal"; // todo DEV -> remove
	public static final String HOSTNAME = "kafka";
	public static final int PORT = 9092;

	/// Amount of ms that we try to establish a connection to kafka for before failing
	public static final int CONNECTION_TIMEOUT = 100;

	private KafkaConsumer<String, String> kafkaConsumer;

	/// Specifies consumers to call when a kafka message is received
	private List<Consumer<ConsumerRecord<String, String>>> consumerPassthrough;


	private volatile boolean running = false;

	/// Internal kafka consumer group id; if two connections share a group id, kafka events are distributed across them.
	private final String groupId;

	public LaunchBlockKafkaConsumerConnection(String groupId, Consumer<ConsumerRecord<String, String>> consumerPassthrough) {
		this.groupId = groupId;
		this.consumerPassthrough = Collections.singletonList(consumerPassthrough);
	}

	public void close() {
		running = false;
		kafkaConsumer.wakeup();
	}

	// runs in consumerThread
	private void run() {
		kafkaConsumer = createConsumer();

		// dynamically includes all new topics as opposed to Consumer#listTopics#keySet
		kafkaConsumer.subscribe(Pattern.compile(".*"));

		try {
			while (running) {
				ConsumerRecords<String, String> records = kafkaConsumer.poll(Long.MAX_VALUE);

				if (records.count() == 0) continue;

				records.forEach(record -> {
					consumerPassthrough.forEach(pass -> {
						pass.accept(record);
					});
				});

				kafkaConsumer.commitSync(); // advances offset to not receive old events
			}
		} catch (WakeupException e) { // when an indefinitely running poll tries to wake up through close(), we want to close.
			kafkaConsumer.close();
			running = false;
			return;
		}

		kafkaConsumer.close();
		running = false;
	}

	private boolean isKafkaRunning() {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(HOSTNAME, PORT), CONNECTION_TIMEOUT);
			return true;
		} catch (IOException unused) {
			return false;
		}
	}

	private KafkaConsumer<String, String> createConsumer() {
		// https://kafka.apache.org/10/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html
		Properties props = new Properties();
		props.put("bootstrap.servers", HOSTNAME + ":" + PORT);
		props.put("group.id", getGroupId()); // group ids are used for load distribution, irrelevant here
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		// don't include previous messages
		props.put("auto.offset.reset", "latest"); // Options: latest, earliest, none

		return new KafkaConsumer<>(props);
	}

	protected void start() {
		if (running) {
			return;
		}

		if (!isKafkaRunning()) {
			throw new RuntimeException("Could not connect to kafka. Make sure your kafka instance is enabled before listening to it.");
			// todo proper handling and logging
		}
		running = true;
		Executors.newSingleThreadExecutor().submit(this::run);

	}

	public KafkaConsumer<String, String> getKafkaConsumer() {
		return kafkaConsumer;
	}

	public String getGroupId() {
		return groupId;
	}
}
