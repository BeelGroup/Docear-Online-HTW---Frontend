package services.backend.user;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.UserNotFoundException;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.Play;
import play.libs.F.Promise;

@Profile("userServiceMock")
@Component
public class MockUserService extends UserService {
	
	private final List<String> allowedUsernames = Play.application().configuration().getStringList("application.users.mockNames");
	private final String allowedPasswort = Play.application().configuration().getString("application.users.mockPassword");
	
	@Override
	public Promise<String> authenticate(String username, String password) {
		final boolean usernameCorrect = allowedUsernames.contains(username);
		final boolean authenticated = usernameCorrect && allowedPasswort.equals(password);
		return Promise.pure(authenticated ? generateMockToken(username) : null);
	}

	@Override
	public Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException {
		if (isValid(user))
			return Promise.pure(Arrays.asList(new UserMindmapInfo("1", "1", "Dec 01, 2012 6:38:31 PM", "/not/available", "1.mm"), new UserMindmapInfo("1", "1", "Dec 01, 2012 7:38:31 PM",
					"/not/available", "1.mm"), new UserMindmapInfo("1", "1", "Dec 01, 2012 8:38:31 PM", "/not/available", "1.mm"), new UserMindmapInfo("2", "2", "Dec 02, 2012 6:38:31 PM",
					"/not/available", "2.mm"), new UserMindmapInfo("3", "3", "Dec 03, 2012 6:38:31 PM", "/not/available", "3.mm"), new UserMindmapInfo("4", "4", "Dec 19, 2012 6:38:31 PM",
					"/not/available", "4.mm"), new UserMindmapInfo("5", "5", "Dec 05, 2012 6:38:31 PM", "/not/available", "5.mm")));
		else
			throw new UserNotFoundException();
	}

	@Override
	public Boolean isValid(User user) {
		if (user.getAccessToken().equals(generateMockToken(user.getUsername()))) {
			return true;
		} else if (user.getAccessToken().equals("1") && user.getUsername().equals("User1")) {
			return true;
		} else if (user.getAccessToken().equals("2") && user.getUsername().equals("User2")) {
			return true;
		} else {
			return false;
		}
	}

	private String generateMockToken(String username) {
		return username + "-token";
	}

}
