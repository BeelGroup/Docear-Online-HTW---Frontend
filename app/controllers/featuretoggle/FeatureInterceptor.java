package controllers.featuretoggle;

import com.google.common.collect.Iterables;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import static java.util.Arrays.asList;

public class FeatureInterceptor extends Action<ImplementedFeature> {
    public Result call(Http.Context ctx) throws Throwable {
        final Feature[] features = configuration.value();
        final boolean allFeaturesEnabled = Iterables.all(asList(features), new FeatureEnabledPredicate());
        Result result = notFound();
        if (allFeaturesEnabled) {
            result = delegate.call(ctx);
        }
        return result;
    }
}