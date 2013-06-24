package controllers.featuretoggle;

import com.google.common.collect.Iterables;

import java.util.List;

import static java.util.Arrays.asList;

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
 * Usage in Javascripts:
 * {@code
 * if("KEYBOARD_NAVIGATION" in features)
 *  console.log "drin" }
 *
 * Configure available features in conf files: application.features = [KEYBOARD_NAVIGATION, FEATURE_XYZ]
 */
public enum Feature {
    NEVER_IMPLEMENTED("dummy for testing"),
    KEYBOARD_NAVIGATION("enables the user to move with the arrow buttons on the mind map"),
    EDIT_NODE_TEXT("enables the user to click on a node and change the text in it"),
    NODE_CONTROLS("enables node controls to edit/add/delete nodes "),
    FOLD_NODE("enables to fold/unfold nodes "),
    DELETE_NODE("deletes node from mindmap"),
    MOVE_NODE("enables to move nodes into an other node or to anouter position"),
    ADD_NODE("enables to add nodes"),
    FEEDBACK("enables a feedback dialog for the user"),
    USER_PROFILE("enables the user profile"),
    SERVER_SYNC("synchronizes all changes in the frontend with the backend"),
    WORKSPACE("enables project synchronization with the API-Server"),
    WORKSPACE_UPLOAD("enables file upload to projects"),
    WORKSPACE_JSTREE("enables rendering of project structure using jstree"),
    WORKSPACE_LAZY_LOADING("enables lazi loading for resources of projects"),
    LISTEN_FOR_UPDATES("listen for updates via long polling"),
    LOCK_NODE("lock and unlock nodes when in use"),
    RIBBONS("display ribbons on top of page"),
    RIBBON_GENERAL("display general ribbon"),
    ANIMATE_TREE_RESIZE("enable animation when tree is resized (e.g. new node added)");

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

    public static Iterable<Feature> enabledFeatures() {
        return Iterables.filter(getFeatureList(), new FeatureEnabledPredicate());
    }

    public static List<Feature> getFeatureList() {
        return asList(values());
    }
}