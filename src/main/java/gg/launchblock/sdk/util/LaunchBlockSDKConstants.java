package gg.launchblock.sdk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class LaunchBlockSDKConstants {
	
	static {
		// default hostname 'kafka' if not otherwise defined by environment variable
		KAFKA_HOSTNAME = Objects.requireNonNullElse(System.getenv("KAFKA_HOSTNAME"), "kafka");

		// default port 9092 if not otherwise defined by environment variable
		KAFKA_PORT = Objects.requireNonNullElse(Integer.valueOf(System.getenv("KAFKA_PORT")), 9092);
	}

	public static final Logger JAVA_SDK_LOGGER = LoggerFactory.getLogger("gg.launchblock.sdk");

	/// Amount of ms that we try to establish a connection to kafka for before failing
	public static final int KAFKA_CONNECTION_TIMEOUT = 100;
	public static final String KAFKA_HOSTNAME;
	public static final int KAFKA_PORT;

}
