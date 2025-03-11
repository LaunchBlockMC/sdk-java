# LaunchBlock Java SDK
![Java SDK](https://github.com/user-attachments/assets/c535c4bb-6d0a-4a7d-8742-94d273dbe295)

![GitHub Release](https://img.shields.io/github/v/release/LaunchBlockMC/sdk-java)

## Installing the SDK

Get the current latest version from [the releases page](https://github.com/LaunchBlockMC/sdk-java/releases)

### Maven
Add the dependency to your dependencies section in `pom.xml`
```
<dependency>
  <groupId>gg.launchblock</groupId>
  <artifactId>sdk-java</artifactId>
  <version>version</version>
</dependency>
```

### Gradle
Add the dependency to your `build.gradle` file
```
dependencies {
    implementation 'gg.launchblock:sdk-java:version'
}
```

## Listener System
### Listening to events
Create a class implementing `LaunchBlockEventListener`,
then use methods annotated with `@LaunchBlockEventHandler`
with one parameter representing a class which extends `LaunchBlockEvent`.

To register events on an object of this type, create a `LaunchBlockEventManager` and 
call its `registerEvents` method on your listener object. 

Remember to `close()` your LaunchBlockEventManager when it is no longer required.

**Load Distribution** <br>
the `groupId` parameter in `LaunchBlockEventManager`'s constructor may be used to distribute events across all event managers with this group id. (usually across multiple running instances of an application)

**Example** 
```java
public class TestClass implements LaunchBlockEventListener {

	@LaunchBlockEventHandler(priority = LaunchBlockEventPriority.HIGH)
	public void onProjectCreated(final LaunchBlockProjectCreatedEvent e) {
		// Someone just created a project!
	}
	
}
```
```java
public class Main {

	public static void main(final String[] args) {
		final LaunchBlockEventManager eventManager = new LaunchBlockEventManager();
		eventManager.registerEvents(new TestClass());
		//...
		eventManager.close();
	}
	
}
```

### Creating events
Create a class extending `LaunchBlockEvent` with a constructor of `(String, JsonNode)`, representing the topic and raw json message sent through kafka.

To call this event through this constructor when a kafka message is received within a specific topic, use the `createTopicBinding` method on your event manager.

**Example**
```java
public class SomeEvent extends LaunchBlockEvent {

	public SomeEvent(final String topic, final JsonNode rawContent) {
		super(topic, rawContent);
		// Process raw content
	}
    
}
```
```java
public class Main {

	public static void main(final String[] args) {
		final LaunchBlockEventManager eventManager = new LaunchBlockEventManager();
		eventManager.createTopicBinding("kafka_topic", SomeEvent.class);
		//...
		eventManager.close();
	}
	
}
```

## Emitter System
To send messages to kafka topics, which may be picked up by the listener system, you may use the emitter system.

To use, create a `LaunchBlockMessageEmitter` object and call its `send` method.

**Example**
```java
public class Main {

	public static void main(final String[] args) {
		final LaunchBlockEventEmitter emitter = new LaunchBlockEventEmitter();

		final ObjectNode message = new ObjectMapper().createObjectNode();
		message.put("host", "launchblock.gg");
		
		emitter.send("kafka_topic", message);
		
		//...
		emitter.close();
	}
	
}
```