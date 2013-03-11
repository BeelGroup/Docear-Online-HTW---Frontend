package services.backend.user;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import models.backend.User;
import models.backend.UserMindmapInfo;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.libs.F;
import play.libs.F.Function;
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
	
	
	@Override
	public Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException {
		if(user == null) {
			throw new NullPointerException("user cannot be null");
		}

		String docearServerAPIURL = "https://api.docear.org/user";
		final Promise<WS.Response> accessTokenPromise = WS.url(docearServerAPIURL + "/" + user.getUsername() + "/mindmaps/")
				.setHeader("accessToken", user.getAccessToken()).get();
		return accessTokenPromise.map(new Function<WS.Response, List<UserMindmapInfo>>() {
			@Override
			public List<UserMindmapInfo> apply(WS.Response response) throws Throwable {
				BufferedReader br = new BufferedReader (new StringReader(response.getBody().toString()));
				List<UserMindmapInfo> infos = new LinkedList<UserMindmapInfo>();
				for ( String line; (line = br.readLine()) != null; ){
					String[] strings = line.split("\\|#\\|");

					UserMindmapInfo info = new UserMindmapInfo(strings[0], strings[1], strings[2], strings[3], strings[4]);
					infos.add(info);
				}
				return Arrays.asList(infos.toArray(new UserMindmapInfo[0]));
			}
		});
	}

}
