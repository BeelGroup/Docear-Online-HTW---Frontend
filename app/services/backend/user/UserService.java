package services.backend.user;

import static controllers.Secured.SESSION_KEY_USERNAME;

import java.io.IOException;
import java.util.List;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.UserNotFoundException;
import play.Logger;
import play.libs.F.Promise;
import play.mvc.Controller;
import controllers.Session;

public abstract class UserService {

	/**
	 * authenticates user credentials
	 * @param username
	 * @param password
	 * @return user access token or null on failure
	 */
    public abstract Promise<String> authenticate(String username, String password);
	
    public abstract Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException;
    
    public abstract Promise<List<Long>> getListOfProjectIdsFromUser(User user);
    
    public abstract Boolean isValid(User user);
    
	/** 
	 * @return User or null if non is logged-in
	 */
	public User getCurrentUser() {
		final String accessToken = Controller.request().getQueryString("accessToken");
		final String username = Controller.request().getQueryString("username");
		Logger.debug("accessToken: " + accessToken + "; username: " + username);
		if (accessToken != null && username != null) {
			final User user = new User(username, accessToken);
			if (isValid(user))
				return user;
			else
				throw new UserNotFoundException();
		} else {
			return Session.getUser(Controller.session(SESSION_KEY_USERNAME));
		}
	}

	public boolean isAuthenticated() {
		return getCurrentUser() != null;
	}
}
