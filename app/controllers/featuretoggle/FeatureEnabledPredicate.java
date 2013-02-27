package controllers.featuretoggle;

import com.google.common.base.Predicate;

class FeatureEnabledPredicate implements Predicate<Feature> {
    @Override
    public boolean apply(Feature feature) {
        return feature.isEnabled();
    }
}
