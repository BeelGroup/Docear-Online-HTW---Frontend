package controllers.featuretoggle;

import play.Logger;
import play.Play;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

public class FeatureInterceptor extends Action<Feature> {

    public Result call(Http.Context ctx) throws Throwable {

        Feature configuration1 = configuration;

//        //if is def/conf...
//        Play.application().configuration();
        Logger.info("Calling action for " + ctx);
        Logger.info("value " + configuration.getClass() );
        Logger.info("value " + configuration);

        return notFound("not there");
    }
}