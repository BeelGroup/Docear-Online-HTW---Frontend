package controllers.featuretoggle;

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