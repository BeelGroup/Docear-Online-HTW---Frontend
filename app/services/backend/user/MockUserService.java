package services.backend.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.UserNotFoundException;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.libs.F.Promise;

@Profile("userServiceMock")
@Component
public class MockUserService extends UserService {

	@Override
	public Promise<String> authenticate(String username, String password) {
		final boolean usernameCorrect = Arrays.asList("Jöran", "Julius", "Michael", "Florian", "Alex", "Paul", "Marcel", "Dimitri", "Volker").contains(username);
		final boolean authenticated = usernameCorrect && "secret".equals(password);
		return Promise.pure(authenticated ? username + "-token-" + UUID.randomUUID().toString() : null);
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
	public Promise<List<Long>> getListOfProjectIdsFromUser(User user) {
		List<Long> list = new ArrayList<Long>();
		list.add(1L);
		return Promise.pure(list);
	}

	@Override
	public Boolean isValid(User user) {
		if (user.getAccessToken().equals("1") && user.getUsername().equals("User1")) {
			return true;
		} else if (user.getAccessToken().equals("2") && user.getUsername().equals("User2")) {
			return true;
		} else {
			return false;
		}
	}


}
