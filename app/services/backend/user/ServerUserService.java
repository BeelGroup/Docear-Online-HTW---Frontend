package services.backend.user;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.sendResult.SendResultException;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.Logger;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.mvc.Controller;
import controllers.Session;

@Profile("DocearWebserviceUserService")
@Component
public class ServerUserService extends UserService {

	@Override
	public Promise<String> authenticate(String username, String password) {
		try {
			final String user = URLEncoder.encode(username, "UTF-8");
			final String pwd = URLEncoder.encode(password, "UTF-8");
			final Promise<Response> wsResponsePromise = WS.url("https://api.docear.org/authenticate/" + user).setHeader("Content-Type", "application/x-www-form-urlencoded").post("password=" + pwd);
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
		} catch (UnsupportedEncodingException e) {
			throw new SendResultException("Used unsupported encding", Controller.INTERNAL_SERVER_ERROR, e);
		}
	}

	@Override
	public Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException {
		if (user == null) {
			throw new NullPointerException("user cannot be null");
		}
		Logger.debug("ServerUserService.getListOfMindMapsFromUser => user: " + user.getUsername());

		String docearServerAPIURL = "https://api.docear.org/user";
		final Promise<WS.Response> mindmapInfoPromise = WS.url(docearServerAPIURL + "/" + user.getUsername() + "/mindmaps/").setHeader("accessToken", user.getAccessToken()).get();
		try {
			final Response response = mindmapInfoPromise.get();
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
		} catch (Exception e) {
			throw new SendResultException("Docear server not reachable", Controller.SERVICE_UNAVAILABLE, e);
		}
	}

	@Override
	public Boolean isValid(User user) {
		// check if cache has info
		Boolean valid = Session.isValid(user);
		if (valid != null)
			return valid;

		/**
		 * TODO use route that is for user validation At the moment the method
		 * tries to get a not existent map from docear server Every code instead
		 * of 401 means that the user exists. Ticket:
		 * https://sourceforge.net/apps/trac/docear/ticket/830
		 */
		final String docearServerAPIURL = "https://api.docear.org/user";
		final String url = docearServerAPIURL + "/" + user.getUsername() + "/mindmaps/-1";
		final Promise<WS.Response> mindmapInfoPromise = WS.url(url).setHeader("accessToken", user.getAccessToken()).get();
		try {
			final Response response = mindmapInfoPromise.get();

			final int status = response.getStatus();
			valid = (status != 401);
			Session.setValid(user, valid);
			return valid;
		} catch (Exception e) {
			throw new SendResultException("Docear server not reachable", Controller.SERVICE_UNAVAILABLE, e);
		}
	}
}
