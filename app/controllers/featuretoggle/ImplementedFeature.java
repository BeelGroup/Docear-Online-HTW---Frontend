package controllers.featuretoggle;

import play.mvc.With;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Usage in Controller or action:
 * {@code  @ImplementedFeature({EDIT_NODE_TEXT, KEYBOARD_NAVIGATION})}
 */
@With(FeatureInterceptor.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImplementedFeature {
    Feature[] value();
}
