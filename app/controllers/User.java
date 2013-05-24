package controllers;

import static controllers.Secured.SESSION_KEY_ACCESS_TOKEN;
import static controllers.Secured.SESSION_KEY_TIMEOUT;
import static controllers.Secured.SESSION_KEY_USERNAME;
import static controllers.Secured.createTimeoutTimestamp;
import static play.data.Form.form;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;
import models.frontend.Credentials;

import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import play.Logger;
import play.data.Form;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import services.backend.project.ProjectService;
import services.backend.user.UserService;

@Component
public class User extends Controller {

	public static final Form<Credentials> credentialsForm = form(Credentials.class);

	@Autowired
	private UserService userService;
	
	@Autowired
	private ProjectService projectService;

	public Result login() {
		final Form<Credentials> filledForm = credentialsForm.bindFromRequest();
		Result result;

		if (filledForm.hasErrors()) {
			result = badRequest(views.html.index.render(filledForm));
		} else {
			final Credentials credentials = filledForm.get();
			final F.Promise<String> tokenPromise = userService.authenticate(credentials.getUsername(), credentials.getPassword());
			result = async(tokenPromise.map(new F.Function<String, Result>() {
				@Override
				public Result apply(String accessToken) throws Throwable {
					final boolean authenticationSuccessful = accessToken != null;
					if (authenticationSuccessful) {
						setAuthenticatedSession(credentials, accessToken);
						Logger.info("User '" + credentials.getUsername() + "' logged in succesfully.");
						return redirect(routes.Application.index());
					} else {
						filledForm.reject("The credentials don't match any user.");
						Logger.debug(credentials.getUsername() + " is unauthorized");
						return unauthorized(views.html.index.render(filledForm));
					}
				}
			}));
		}
		return result;
	}

	public Result loginRest() {
		final Form<Credentials> filledForm = credentialsForm.bindFromRequest();
		Result result;

		if (filledForm.hasErrors()) {
			result = badRequest(views.html.index.render(filledForm));
		} else {
			final Credentials credentials = filledForm.get();
			final F.Promise<String> tokenPromise = userService.authenticate(credentials.getUsername(), credentials.getPassword());
			result = async(tokenPromise.map(new F.Function<String, Result>() {
				@Override
				public Result apply(String accessToken) throws Throwable {
					final boolean authenticationSuccessful = accessToken != null;
					if (authenticationSuccessful) {
						setAuthenticatedSession(credentials, accessToken);
						Logger.info("User '" + credentials.getUsername() + "' logged in succesfully.");
						return ok(accessToken);
					} else {
						filledForm.reject("The credentials don't match any user.");
						Logger.debug(credentials.getUsername() + " is unauthorized");
						return unauthorized();
					}
				}
			}));
		}
		return result;
	}

	@Security.Authenticated(Secured.class)
	public Result mapListFromDB() throws IOException, DocearServiceException {
		final Promise<List<UserMindmapInfo>> listOfMindMapsFromUser = userService.getListOfMindMapsFromUser(user());
		return async(listOfMindMapsFromUser.map(new F.Function<List<UserMindmapInfo>, Result>() {
			@Override
			public Result apply(List<UserMindmapInfo> maps) throws Throwable {
				return ok(Json.toJson(maps));
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result projectListFromDB() throws IOException {
		final Promise<JsonNode> projectListPromise = projectService.getProjectsFromUser(user().getUsername());
		return async(projectListPromise.map(new F.Function<JsonNode, Result>() {
			@Override
			public Result apply(JsonNode projects) throws Throwable {
				return ok(Json.toJson(projects));
			}
		}));
	}

	public List<UserMindmapInfo> getMindmapInfosOfLoggedInUser() throws IOException {
		final models.backend.User user = user();
		if (user == null)
			return new ArrayList<UserMindmapInfo>();

		final Promise<List<UserMindmapInfo>> listOfMindMapsFromUser = userService.getListOfMindMapsFromUser(user());
		return listOfMindMapsFromUser.get();
	}

	private void setAuthenticatedSession(Credentials credentials, String accessToken) {
		session(SESSION_KEY_USERNAME, credentials.getUsername());
		session(SESSION_KEY_ACCESS_TOKEN, accessToken);
		session(SESSION_KEY_TIMEOUT, createTimeoutTimestamp().toString());
	}

	@Security.Authenticated(Secured.class)
	public Result logout() {
		session().clear();
		return redirect(routes.Application.index());
	}

	@Security.Authenticated(Secured.class)
	public Result profile() {
		return TODO;
	}
	
	private models.backend.User user() {
		return userService.getCurrentUser();
	}

}
