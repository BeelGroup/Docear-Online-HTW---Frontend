package services.backend.user;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.libs.F.Promise;

import java.util.Arrays;
import java.util.UUID;


@Profile("userServiceMock")
@Component
public class MockUserService implements UserService{

	@Override
	public Promise<String> authenticate(String username, String password) {
        final boolean usernameCorrect = Arrays.asList("Jöran", "Julius", "Michael", "Florian", "Alex", "Paul", "Marcel", "Dimitri", "Volker").contains(username);
        final boolean authenticated = usernameCorrect && "secret".equals(password);
        return Promise.pure(authenticated ? username + "-token-" + UUID.randomUUID().toString() :null);
    }

}
