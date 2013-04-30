package services.backend.user;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import models.backend.User;
import models.backend.UserMindmapInfo;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.Logger;
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
				if (authenticationSuccessful) {
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
		if (user == null) {
			throw new NullPointerException("user cannot be null");
		}
		Logger.debug("ServerUserService.getListOfMindMapsFromUser => user: " + user.getUsername());

		String docearServerAPIURL = "https://api.docear.org/user";
		final Promise<WS.Response> mindmapInfoPromise = WS.url(docearServerAPIURL + "/" + user.getUsername() + "/mindmaps/").setHeader("accessToken", user.getAccessToken()).get();
		WS.Response response = mindmapInfoPromise.get();

		Logger.debug("ServerUserService.getListOfMindMapsFromUser => response received");
		BufferedReader br = new BufferedReader(new StringReader(response.getBody().toString()));
		List<UserMindmapInfo> infos = new LinkedList<UserMindmapInfo>();
		for (String line; (line = br.readLine()) != null;) {
			String[] strings = line.split("\\|#\\|");
			final String mmIdOnServer = strings[0];
			final String mmIdInternal = strings[1];
			final String revision = strings[2];
			final String filePath = strings[3];
			final String fileName = strings[4];

			UserMindmapInfo info = new UserMindmapInfo(mmIdOnServer, mmIdInternal, revision, filePath, fileName);
			infos.add(info);
		}
		return Promise.pure(Arrays.asList(infos.toArray(new UserMindmapInfo[0])));
	}
	
	@Override
	public Promise<List<Long>> getListOfProjectIdsFromUser(User user) {
		throw new NotImplementedException("https://github.com/Docear/HTW-Frontend/issues/305");
	}

}
