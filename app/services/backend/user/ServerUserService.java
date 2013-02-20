package services.backend.user;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.libs.F;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;



@Profile("DocearWebserviceUserService")
@Component
public class ServerUserService implements UserService {

	@Override
	public Promise<String> authenticate(String username, String password) {
        final Promise<Response> wsResponsePromise = WS.url("https://api.docear.org/authenticate/" + username).post("password=" + password);
        return wsResponsePromise.map(new F.Function<Response, String>() {
            @Override
            public String apply(Response response) throws Throwable {
                final boolean authenticationSuccessful = response.getStatus() == 200;
                if(authenticationSuccessful) {
                    String accessToken = response.getHeader("accessToken");
                    return accessToken;
                } else {
                    return null;
                }
            }
        });
	}

}
