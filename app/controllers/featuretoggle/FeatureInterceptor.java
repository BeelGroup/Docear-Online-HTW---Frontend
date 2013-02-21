package controllers.featuretoggle;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Arrays;

public class FeatureInterceptor extends Action<ImplementedFeature> {
    public Result call(Http.Context ctx) throws Throwable {
        final Feature[] features = configuration.value();
        final boolean allFeaturesEnabled = Iterables.all(Arrays.asList(features), new Predicate<Feature>() {
            @Override
            public boolean apply(Feature feature) {
                return feature.isEnabled();
            }
        });
        Result result = notFound();
        if (allFeaturesEnabled) {
            result = delegate.call(ctx);
        }
        return result;
    }
}