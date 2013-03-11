package controllers;

import java.io.IOException;
import java.util.List;

import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;
import models.frontend.Credentials;

import org.codehaus.jackson.JsonNode;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import play.Logger;
import play.Play;
import play.data.Form;
import play.libs.F;
import play.libs.Json;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import services.backend.user.UserService;

import static controllers.Secured.SESSION_KEY_TIMEOUT;
import static controllers.Secured.SESSION_KEY_USERNAME;
import static controllers.Secured.createTimeoutTimestamp;
import static controllers.User.getCurrentUser;
import static play.data.Form.form;

@Component
public class User extends Controller {

    public static final Form<Credentials> credentialsForm = form(Credentials.class);

	@Autowired
    private UserService userService;
	
	public Result login() {
        final Form<Credentials> filledForm = credentialsForm.bindFromRequest();
        Result result;

        if (filledForm.hasErrors()) {
            result = badRequest(views.html.user.loginForm.render(filledForm));
        } else {
            final Credentials credentials = filledForm.get();
            final F.Promise<String> tokenPromise = userService.authenticate(credentials.getUsername(), credentials.getPassword());
            result = async(tokenPromise.map(new F.Function<String, Result>() {
                @Override
                public Result apply(String accessToken) throws Throwable {
                    final boolean authenticationSuccessful = accessToken != null;
                    if (authenticationSuccessful) {
                        setAuthenticatedSession(credentials, accessToken);
                        return redirect(routes.Application.index());
                    } else {
                        filledForm.reject("The credentials doesn't match any user.");
                        Logger.debug(credentials.getUsername() + " is unauthorized");
                        return unauthorized(views.html.user.loginForm.render(filledForm));
                    }
                }
            }));
        }
        return result;
    }
	
    @Security.Authenticated(Secured.class)
	public Result mapListFromDB() throws IOException, DocearServiceException {
        final Promise<List<UserMindmapInfo>> listOfMindMapsFromUser = userService.getListOfMindMapsFromUser(getCurrentUser());
        return async(listOfMindMapsFromUser.map(new F.Function<List<UserMindmapInfo>, Result>() {
            @Override
            public Result apply(List<UserMindmapInfo> maps) throws Throwable {
                return ok(Json.toJson(maps));
            }
        }));
    }

    private void setAuthenticatedSession(Credentials credentials, String accessToken) {
        Session.createSession(credentials.getUsername(), accessToken);
        session(SESSION_KEY_USERNAME, credentials.getUsername());
        session(SESSION_KEY_TIMEOUT, createTimeoutTimestamp().toString());
    }

    @Security.Authenticated(Secured.class)
    public Result logout() {
        session().clear();
        return redirect(routes.Application.index());
    }

    @Security.Authenticated(Secured.class)
    public Result profile() {
        return redirect(routes.User.mapListFromDB());
    }
	
    public static boolean isAuthenticated() {
    	return getCurrentUser() != null;
    }
    
	/**
	 * 
	 * @return User or null if non is logged-in
	 */
    public static models.backend.User getCurrentUser() {
        return Session.getUser(session(SESSION_KEY_USERNAME));
    }
	
}
