package controllers.secured;

import controllers.routes;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;


public class SecuredFrontend extends SecuredBase {

    @Override
    public Result onUnauthorized(Http.Context ctx) {
        ctx.flash().put("error", "You need to authenticate.");
        return Results.redirect(routes.Application.index());
    }
}
