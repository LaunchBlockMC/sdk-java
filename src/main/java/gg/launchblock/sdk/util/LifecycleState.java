package gg.launchblock.sdk.util;

public enum LifecycleState {
	// name() corresponds to kafka event json

	QUEUE,
	BUILDING,
	BUILD_FAILED,
	DEPLOYING,
	DEPLOY_FAILED,
	DEPLOYED,
	ROLLED_BACK,
	REPLACED;
}
