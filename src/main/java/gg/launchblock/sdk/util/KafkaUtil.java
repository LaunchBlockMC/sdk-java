package gg.launchblock.sdk.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class KafkaUtil {

	/**
	 * Attempts to establish a connection to the kafka server specified through {@link LaunchBlockSDKConstants#KAFKA_HOSTNAME} and {@link LaunchBlockSDKConstants#KAFKA_PORT}
	 * @return The result of this attempt
	 */
	public static boolean isKafkaRunning() {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(LaunchBlockSDKConstants.KAFKA_HOSTNAME, LaunchBlockSDKConstants.KAFKA_PORT), LaunchBlockSDKConstants.KAFKA_CONNECTION_TIMEOUT);
			return true;
		} catch (IOException unused) {
			return false;
		}
	}
}
