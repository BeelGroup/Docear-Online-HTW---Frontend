package services.backend.mindmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
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

import com.typesafe.config.ConfigFactory;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;
import scala.concurrent.Future;
import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;
import util.backend.ZipUtils;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;

@Profile("backendProd")
@Component
public class ServerMindMapCrudService extends MindMapCrudServiceBase implements MindMapCrudService {
	private static Map<String, String> serverIdToMapIdMap;
	private static ObjectMapper objectMapper;
	private static ActorSystem system;
	private static ActorRef remoteActor;
	

	public ServerMindMapCrudService() {
		
		serverIdToMapIdMap = new HashMap<String, String>();
		objectMapper = new ObjectMapper();
	}
	
	@Override
	public Promise<JsonNode> mindMapAsJson(String id) throws DocearServiceException, IOException {
		//hack, because we use 'wrong' ids at the moment because of docear server ids
		String mindmapId = serverIdToMapIdMap.get(id);
		String serverUrl = null;
		if(mindmapId == null) { //if not hosted, send to a server
			Logger.debug("No map for server id " + id + ". Sending to server...");
			serverUrl = sendMapToDocearInstance(id);
			mindmapId = serverIdToMapIdMap.get(id);
		} else {
			serverUrl = ServerMindmapMap.getInstance().getServerURLForMap(mindmapId);
			Logger.debug("ServerId: " + id + "; MapId: " + mindmapId + "; url: " +serverUrl);
		}

		String remoteUrl = serverUrl.toString();
		ActorRef remoteActor = getRemoteActor(remoteUrl);

		Future<Object> future = akka.pattern.Patterns.ask(remoteActor, new MindmapAsJsonRequest(mindmapId), 20000);

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

	private String sendMapToDocearInstance(String mapId) throws NoUserLoggedInException {
		//find server with capacity
		String serverUrl = ServerMindmapMap.getInstance().getServerWithFreeCapacity();

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
					return null;

				//TODO just a hack, because we are currently using different ids for retrieval then supposed
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
		ActorRef remoteActor = getRemoteActor(serverUrl);
		remoteActor.tell(new OpenMindMapRequest(file),remoteActor);	
		//akka.pattern.Patterns.ask(remoteActor, new OpenMindMapRequest(file), 20000);

		return serverUrl;
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

	private ActorRef getRemoteActor(String urlString) {
		if(system == null) {
			system = ActorSystem.create("freeplaneSystem",ConfigFactory.load().getConfig("local"));
		}
		ActorRef remoteActor = system.actorFor(urlString);
		Logger.debug("Connection to "+remoteActor.path()+" established");
		return remoteActor;
	}
}
