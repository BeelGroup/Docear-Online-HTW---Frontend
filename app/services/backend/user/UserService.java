package services.backend.user;

import static controllers.Secured.QUERY_ACCESS_TOKEN;
import static controllers.Secured.QUERY_USERNAME;
import static controllers.Secured.SESSION_KEY_ACCESS_TOKEN;
import static controllers.Secured.SESSION_KEY_USERNAME;

import java.io.IOException;
import java.util.List;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.UserNotFoundException;
import play.libs.F.Promise;
import play.mvc.Controller;

public abstract class UserService {

	/**
	 * authenticates user credentials
	 * @param username
	 * @param password
	 * @return user access token or null on failure
	 */
    public abstract Promise<String> authenticate(String username, String password);
	
    public abstract Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException;
    
    public abstract Boolean isValid(User user);
    
	/** 
	 * @return User or null if non is logged-in
	 */
	public User getCurrentUser() throws UserNotFoundException {
		return getCurrentUser(null);
	}
	
	public User getCurrentUser(final String source) {
		//get aT and username from queryString
		String accessToken = Controller.request().getQueryString(QUERY_ACCESS_TOKEN);
		String username = Controller.request().getQueryString(QUERY_USERNAME);
		
		//check if both present
		if (!(accessToken != null && username != null)) {
			//get from session instead
			accessToken = Controller.session().get(SESSION_KEY_ACCESS_TOKEN);
			username = Controller.session().get(SESSION_KEY_USERNAME);
		}
		
		//if at least one is not set it does count as non logged in
		if(accessToken == null || username == null) {
			return null;
		}
		
		User user = null;
		if (accessToken != null && username != null) {
			user = new User(username, accessToken);
			if (!isValid(user))
				user = null;
		}
		
		if(user!= null)
			return user;
		else
			throw new UserNotFoundException();
	}

	public boolean isAuthenticated() {
		try {
			return getCurrentUser() != null;
		} catch (UserNotFoundException e) {
			return false;
		}
	}
}
