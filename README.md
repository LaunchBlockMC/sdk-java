![Java SDK](https://github.com/user-attachments/assets/c535c4bb-6d0a-4a7d-8742-94d273dbe295)


# Listener System
## Listening to events
Create a class implementing `LaunchBlockEventListener`,
then use methods annotated with `@LaunchBlockEventHandler`
with one parameter representing a class which extends `LaunchBlockEvent`.

To register events in this class, create a `LaunchBlockEventManager` and 
call its `registerEvents` method on your listener class. 

Remember to `close()` your LaunchBlockEventManager when it is no longer required.

**Example** 
```
public class TestClass implements LaunchBlockEventListener {

	@LaunchBlockEventHandler(priority = LaunchBlockEventPriority.HIGH)
	public void onProjectCreated(LaunchBlockProjectCreatedEvent e) {
		// Someone just created a project!
	}

}
```
```
public class Main {

	public static void main(String[] args) {
		LaunchBlockEventManager eventManager = new LaunchBlockEventManager();
		eventManager.registerEvents(new TestClass());
		...
		eventManager.close();
	}
	
}
```

---
## Creating events
Create a class extending `LaunchBlockEvent` with a constructor of `(String, JsonNode)`, representing the topic and raw json message sent through kafka.

To call this event through this constructor when a kafka message is received within a specific topic, use the `createTopicBinding` method on your event manager.

**Example**
```
public class SomeEvent extends LaunchBlockEvent {

	public SomeEvent(String topic, JsonNode rawContent) {
		super(topic, rawContent);
		// Process raw content
	}
    
}
```
```
public class Main {

	public static void main(String[] args) {
		LaunchBlockEventManager eventManager = new LaunchBlockEventManager();
		eventManager.createTopicBinding("kafka_topic", SomeEvent.class);
		...
		eventManager.close();
	}
	
}
```