package services.backend.user;

import java.io.IOException;
import java.util.List;

import models.backend.User;
import models.backend.UserMindmapInfo;
import play.libs.F.Promise;

public interface UserService {

	/**
	 * authenticates user credentials
	 * @param username
	 * @param password
	 * @return user access token or null on failure
	 */
    Promise<String> authenticate(String username, String password);
	
    Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException;
    
    Promise<List<Long>> getListOfProjectIdsFromUser(User user);
}
