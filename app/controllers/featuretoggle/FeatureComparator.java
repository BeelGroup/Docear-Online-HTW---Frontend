package controllers.featuretoggle;

import java.util.Comparator;

public class FeatureComparator implements Comparator<Feature> {
    @Override
    public int compare(Feature feature, Feature feature2) {
        return feature.toString().compareTo(feature2.toString());
    }
}