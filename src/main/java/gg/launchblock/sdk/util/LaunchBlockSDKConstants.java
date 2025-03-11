package gg.launchblock.sdk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaunchBlockSDKConstants {
	public static final Logger JAVA_SDK_LOGGER = LoggerFactory.getLogger("gg.launchblock.sdk");

	// may be different in a development environment if kafka is running inside of docker while sdk is not (check docker settings)
	public static final String KAFKA_HOSTNAME = "kafka";
	public static final int KAFKA_PORT = 9092;

}
