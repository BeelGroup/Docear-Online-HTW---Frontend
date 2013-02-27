package controllers.featuretoggle;

/**
 * Feature toggle mechanism
 *
 * Usage in controller or action:
 * annotate it with:
 * {@code  @ImplementedFeature({EDIT_NODE_TEXT, KEYBOARD_NAVIGATION})}
 *
 * Usage in Scala templates: (don't forget the imports)
 * {@code
 * if(Feature.FEATURE_NAME.isEnabled()= {feature is available }
 * }
 *
 * Configure available features in conf files: application.features = [KEYBOARD_NAVIGATION, FEATURE_XYZ]
 */
public enum Feature {
    NEVER_IMPLEMENTED("dummy for testing"),
    KEYBOARD_NAVIGATION("enables the user to move with the arrow buttons on the mind map"),
    EDIT_NODE_TEXT("enables the user to click on a node and change the text in it");

    String description;
    boolean isEnabled = false;

    private Feature(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public static void enableFeature(String featureName, boolean isEnabled) {
        try {
            Feature.valueOf(featureName).isEnabled = isEnabled;
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Feature not found: " + featureName, e);
        }
    }
}