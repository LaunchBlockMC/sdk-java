package gg.launchblock.sdk.exception;

import gg.launchblock.sdk.util.LaunchBlockSDKConstants;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LaunchBlockSDKException extends RuntimeException {

	private final LaunchBlockSDKExceptionType type;
	private final String message;

	public LaunchBlockSDKException(final Throwable throwable, final LaunchBlockSDKExceptionType type, final String message) {
		super(message);

		this.type = type;
		this.message = message;

		StringBuilder logError = new StringBuilder("SDK error was thrown: [%s], %s".formatted(type.name(), message));

		// make stack trace available if provided; useful for exceptions on other threads which java doesn't normally print in System.err
		if (throwable != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			throwable.printStackTrace(pw);

			logError.append("; %s".formatted(sw.toString()));
		}

		LaunchBlockSDKConstants.JAVA_SDK_LOGGER.error(logError.toString());
	}


	public LaunchBlockSDKException(LaunchBlockSDKExceptionType type, String message) {
		this(null, type, message);
	}

	public LaunchBlockSDKExceptionType getType() {
		return type;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
