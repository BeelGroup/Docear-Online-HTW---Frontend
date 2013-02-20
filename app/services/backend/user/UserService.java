package services.backend.user;

import play.libs.F.Promise;

public interface UserService {

	/**
	 * authenticates user credentials
	 * @param username
	 * @param password
	 * @return user access token or null on failure
	 */
    Promise<String> authenticate(String username, String password);
	
}
