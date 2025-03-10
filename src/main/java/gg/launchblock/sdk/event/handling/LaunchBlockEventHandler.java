package gg.launchblock.sdk.event.handling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LaunchBlockEventHandler {

	LaunchBlockEventPriority priority() default LaunchBlockEventPriority.DEFAULT;

}
