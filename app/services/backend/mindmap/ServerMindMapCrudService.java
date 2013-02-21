package services.backend.mindmap;

import static akka.pattern.Patterns.ask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.NoUserLoggedInException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.docear.messages.Messages.MindmapAsJsonReponse;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import play.Configuration;
import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;
import scala.concurrent.Future;
import util.backend.ZipUtils;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import com.typesafe.config.ConfigFactory;

@Profile("backendProd")
@Component
public class ServerMindMapCrudService extends MindMapCrudServiceBase implements MindMapCrudService {
	private static Map<String, String> serverIdToMapIdMap;
	private static String freeplaneActorUrl;
	private static ObjectMapper objectMapper;
	private static ActorSystem system;
	
	static {
		final Configuration conf = Play.application().configuration();
		freeplaneActorUrl = conf.getString("backend.singleInstance.host");
		serverIdToMapIdMap = new HashMap<String, String>();
		objectMapper = new ObjectMapper();
	}
	
	@Override
	public Promise<JsonNode> mindMapAsJson(String id) throws DocearServiceException, IOException {
		//hack, because we use 'wrong' ids at the moment because of docear server ids
		String mindmapId = serverIdToMapIdMap.get(id);
		if(mindmapId == null) { //if not hosted, send to a server
			Logger.debug("Map for server id " + id + " not open. Sending to freeplane...");
			sendMapToDocearInstance(id);
			mindmapId = serverIdToMapIdMap.get(id);
		} else {
			Logger.debug("ServerId: " + id + "; MapId: " + mindmapId);
		}

		ActorRef remoteActor = getRemoteActor();
		Future<Object> future = ask(remoteActor, new MindmapAsJsonRequest(mindmapId), 20000);

		Promise<JsonNode> promise = Akka.asPromise(future).map(
				new Function<Object, JsonNode>() {
					@Override
					public JsonNode apply(Object message) throws Throwable {
						final MindmapAsJsonReponse response = (MindmapAsJsonReponse)message;
						final String jsonString = response.getJsonString();
						return objectMapper.readTree(jsonString);
					}
				});

		return promise;
	}


	@Override
	public Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException {
		if(user == null) {
			throw new NullPointerException("user cannot be null");
		}

		String docearServerAPIURL = "https://api.docear.org/user";
		WS.Response response =  WS.url(docearServerAPIURL + "/" + user.getUsername() + "/mindmaps/")
				.setHeader("accessToken", user.getAccessToken()).get().get();

		BufferedReader br = new BufferedReader (new StringReader(response.getBody().toString()));

		List<UserMindmapInfo> infos = new LinkedList<UserMindmapInfo>();
		for ( String line; (line = br.readLine()) != null; ){
			String[] strings = line.split("\\|#\\|");
			Logger.debug(line);
			UserMindmapInfo info = new UserMindmapInfo(strings[0], strings[1], strings[2], strings[3], strings[4]);
			infos.add(info);
		}

		return Promise.pure(Arrays.asList(infos.toArray(new UserMindmapInfo[0])));
	}

	private void sendMapToDocearInstance(String mapId) throws NoUserLoggedInException {
		
		File file = null;
		String mmId = null;
		try {
			if(mapId.length() == 1) { //test map
				mmId = mapId;
				file = new File(Play.application().resource("mindmaps/"+mapId+".mm").toURI());
			} else { //map from user account
				User user = controllers.User.getCurrentUser();
				if(user == null)
					throw new NoUserLoggedInException();

				file = getMindMapFileFromDocearServer(user, mapId);
				if(file == null)
					throw new FileNotFoundException();

				//just a hack, because we are currently using different ids for retrieval then supposed
				mmId = getMapIdFromFile(file);

			}
		} catch (FileNotFoundException e) {
			Logger.error("can't find mindmap file", e);
		} catch (IOException e) {
			Logger.error("can't open mindmap file", e);
		} catch (URISyntaxException e) {
			Logger.error("can't open mindmap file", e);
		}

		serverIdToMapIdMap.put(mapId, mmId);

		//send file to server and put in map
		ActorRef remoteActor = getRemoteActor();
		remoteActor.tell(new OpenMindMapRequest(file),remoteActor);	
	}

	private static File getMindMapFileFromDocearServer(final User user, final String mmIdOnServer) throws IOException {
		String docearServerAPIURL = "https://api.docear.org/user";

		WS.Response response =  WS.url(docearServerAPIURL + "/" + user.getUsername() + "/mindmaps/" + mmIdOnServer)
				.setHeader("accessToken", user.getAccessToken())
				.get().get();

		if(response.getStatus() == 200) {
			return ZipUtils.extractMindmap(response.getBodyAsStream());
		} else {
			return null;
		}
	}

	private static String getMapIdFromFile(File mindmapFile) {
		try {
			DocumentBuilderFactory dbFactory =  DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(mindmapFile);

			doc.getDocumentElement().normalize();

			return doc.getDocumentElement().getAttribute("dcr_id");

		} catch (Exception e) {}


		return null;
	}

	private ActorRef getRemoteActor() {
		if(system == null) {
			system = ActorSystem.create("freeplaneSystem",ConfigFactory.load().getConfig("local"));
		}
		ActorRef remoteActor = system.actorFor(freeplaneActorUrl);
		Logger.debug("Connection to "+remoteActor.path()+" established");
		return remoteActor;
	}
}
