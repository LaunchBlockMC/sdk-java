package gg.launchblock.sdk.event.handling;

import gg.launchblock.sdk.exception.LaunchBlockSDKException;
import gg.launchblock.sdk.exception.LaunchBlockSDKExceptionType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class LaunchBlockKafkaConsumerConnection {

	// may be different in a development environment if kafka is running inside of docker while sdk is not (check docker settings)
	public static final String HOSTNAME = "kafka";
	public static final int PORT = 9092;

	/// Amount of ms that we try to establish a connection to kafka for before failing
	public static final int CONNECTION_TIMEOUT = 100;

	private KafkaConsumer<String, String> kafkaConsumer;

	/// Specifies consumers to call when a kafka message is received
	private final List<Consumer<ConsumerRecord<String, String>>> consumerPassthrough;


	private volatile boolean running = false;

	/// Internal kafka consumer group id
	private final String groupId;

	public LaunchBlockKafkaConsumerConnection(final String groupId, final Consumer<ConsumerRecord<String, String>> consumerPassthrough) {
		this.groupId = groupId;
		this.consumerPassthrough = new ArrayList<>() {{
			add(consumerPassthrough);
		}};
	}

	public void close() {
		running = false;
		kafkaConsumer.wakeup();
	}

	/**
	 * Starts listening for kafka messages to pass to `consumerPassthrough` consumers
	 */
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
		} catch (
				WakeupException e) { // when an indefinitely running poll tries to wake up through close(), we want to close.
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
		// for further information,
		// https://kafka.apache.org/10/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html

		Properties props = new Properties();
		props.put("bootstrap.servers", HOSTNAME + ":" + PORT);
		props.put("group.id", getGroupId()); // used for load distribution
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

		// don't include previous messages
		props.put("auto.offset.reset", "latest");

		return new KafkaConsumer<>(props);
	}

	/**
	 * 	invokes `run()` in a separate thread only if the process is not already running
	 * @see LaunchBlockKafkaConsumerConnection#run()
 	 */
	protected void start() {
		if (running) {
			return;
		}

		if (!isKafkaRunning()) {
			throw new LaunchBlockSDKException(LaunchBlockSDKExceptionType.KAFKA,
					"Could not connect to kafka. Make sure your kafka instance is enabled before listening to it.");
		}

		running = true;
		Executors.newSingleThreadExecutor().submit(this::run);
	}

	public void addPassthroughAction(final Consumer<ConsumerRecord<String, String>> action) {
		consumerPassthrough.add(action);
	}

	public KafkaConsumer<String, String> getKafkaConsumer() {
		return kafkaConsumer;
	}

	public String getGroupId() {
		return groupId;
	}
}
