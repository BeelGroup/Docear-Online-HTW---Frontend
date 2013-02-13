package controllers;

import models.frontend.Credentials;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import play.Logger;
import play.Play;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import services.backend.user.UserService;

import static controllers.Secured.SESSION_KEY_TIMEOUT;
import static controllers.Secured.SESSION_KEY_USERNAME;
import static controllers.Secured.createTimeoutTimestamp;
import static play.data.Form.form;

@Component
public class User extends Controller {

    private static final Form<Credentials> credentialsForm = form(Credentials.class);

	@Autowired
    private UserService userService;
	
	public Result login() {
        final Form<Credentials> filledForm = credentialsForm.bindFromRequest();
        Result result;

        if (filledForm.hasErrors()) {
            result = badRequest(views.html.user.loginForm.render(filledForm));
        } else {
            final Credentials credentials = filledForm.get();
            final String accessToken = userService.authenticate(credentials.getUsername(), credentials.getPassword());
            final boolean authenticationSuccessful = accessToken != null;
            if (authenticationSuccessful) {
                setAuthenticatedSession(credentials, accessToken);
                result = redirect(routes.Application.index());
            } else {
                filledForm.reject("The credentials doesn't match any user.");
                Logger.debug(credentials.getUsername() + " is unauthorized");
                result = unauthorized(views.html.user.loginForm.render(filledForm));
            }
        }
        return result;
    }

    private void setAuthenticatedSession(Credentials credentials, String accessToken) {
        Session.createSession(credentials.getUsername(), accessToken);
        session(SESSION_KEY_USERNAME, credentials.getUsername());
        session(SESSION_KEY_TIMEOUT, createTimeoutTimestamp().toString());
    }

    public Result loginForm() {
        return ok(views.html.user.loginForm.render(credentialsForm));
    }

    public Result logout() {
        session().clear();
        return redirect(routes.Application.index());
    }

    public Result profile() {
        return redirect(routes.ControllerFactory.mindMap.mapListFromDB());
    }
	
	/**
	 * 
	 * @return User or null if non is logged-in
	 */
    public static models.backend.User getCurrentUser() {
        return Session.getUser(session(SESSION_KEY_USERNAME));
    }
	
}
