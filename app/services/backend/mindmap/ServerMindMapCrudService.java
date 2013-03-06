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

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.docear.messages.Messages.AddNodeRequest;
import org.docear.messages.Messages.AddNodeResponse;
import org.docear.messages.Messages.ChangeNodeRequest;
import org.docear.messages.Messages.MindmapAsJsonReponse;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.Messages.RemoveNodeRequest;
import org.docear.messages.exceptions.MapNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F;
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
	private Map<String, String> serverIdToMapIdMap = new HashMap<String, String>();
	private final String freeplaneActorUrl = Play.application().configuration().getString("backend.singleInstance.host");
	private final ObjectMapper objectMapper = new ObjectMapper();
	private ActorSystem system;
    private final long defaultTimeoutInMillis = Play.application().configuration().getLong("services.backend.mindmap.MindMapCrudService.timeoutInMillis");

	@Override
	public Promise<JsonNode> mindMapAsJson(final String id) throws DocearServiceException, IOException {
		//hack, because we use 'wrong' ids at the moment because of docear server ids
		String mindmapId = getMindMapIdInFreeplane(id);

		ActorRef remoteActor = getRemoteActor();
		Future<Object> future = ask(remoteActor, new MindmapAsJsonRequest(mindmapId), defaultTimeoutInMillis);

		Promise<JsonNode> promise = Akka.asPromise(future).map(
				new Function<Object, JsonNode>() {
					@Override
					public JsonNode apply(Object message) throws Throwable {
						final MindmapAsJsonReponse response = (MindmapAsJsonReponse)message;
						final String jsonString = response.getJsonString();
						return objectMapper.readTree(jsonString);
					}
				}).recover(new Function<Throwable, JsonNode>() {
					@Override
					public JsonNode apply(Throwable t) throws Throwable {
						if(t instanceof MapNotFoundException) {
							Logger.warn("Map expected on server, but was not present. Reopening...");
							serverIdToMapIdMap.remove(id);
							return mindMapAsJson(id).get();
						} else {
							throw t;
						}
					}
				});

		return promise;
	}
	
	private String getMindMapIdInFreeplane(String id) {
		String mindmapId = serverIdToMapIdMap.get(id);
		if(mindmapId == null) { //if not hosted, send to a server
			Logger.debug("Map for server id " + id + " not open. Sending to freeplane...");
			try {
			sendMapToDocearInstance(id);
			} catch (NoUserLoggedInException e) {
				throw new RuntimeException("No user logged in", e);
			}
			mindmapId = serverIdToMapIdMap.get(id);
		} else {
			Logger.debug("ServerId: " + id + "; MapId: " + mindmapId);
		}
		
		return mindmapId;
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
                    Logger.debug(line);
                    UserMindmapInfo info = new UserMindmapInfo(strings[0], strings[1], strings[2], strings[3], strings[4]);
                    infos.add(info);
                }
                return Arrays.asList(infos.toArray(new UserMindmapInfo[0]));
            }
        });
	}
	
	@Override
	public Promise<JsonNode> addNode(JsonNode addNodeRequestJson) {
		
		final String mapId = getMindMapIdInFreeplane(addNodeRequestJson.get("mapId").asText());
		final String parentNodeId = addNodeRequestJson.get("parentNodeId").asText();
		Logger.debug("mapId: "+mapId+"; parentNodeId: "+parentNodeId);
		AddNodeRequest request = new AddNodeRequest(mapId,parentNodeId);
		
		ActorRef remoteActor = getRemoteActor();
		Future<Object> future = ask(remoteActor, request, defaultTimeoutInMillis);
		
		Promise<JsonNode> promise = Akka.asPromise(future).map(new Function<Object, JsonNode>() {
			@Override
			public JsonNode apply(Object responseMessage) throws Throwable {
				AddNodeResponse response = (AddNodeResponse)responseMessage;
				JsonNode node = objectMapper.readTree(response.getNode());
				return node;
			}
		});
		return promise;
	}


	@Override
	public void ChangeNode(JsonNode changeNodeRequestJson) {
		final String mapId = getMindMapIdInFreeplane(changeNodeRequestJson.get("mapId").asText());
		final String node = changeNodeRequestJson.get("nodeAsJsonString").toString();
		Logger.debug("mapId: "+mapId+"; nodeAsJsonString: "+node);
		ChangeNodeRequest request = new ChangeNodeRequest(mapId,node);
		
		ActorRef remoteActor = getRemoteActor();
		remoteActor.tell(request, remoteActor);
//		Future<Object> future = ask(remoteActor, request, defaultTimeoutInMillis);
//		
//		Promise<JsonNode> promise = Akka.asPromise(future).map(new Function<Object, JsonNode>() {
//			@Override
//			public JsonNode apply(Object responseMessage) throws Throwable {
//				ChangeNodeResponse response = (AddNodeResponse)responseMessage;
//				JsonNode node = objectMapper.readTree(response.getNode());
//				return node;
//			}
//		});
		//return promise;
	}

	@Override
	public void removeNode(JsonNode removeNodeRequestJson) {
		final String mapId = getMindMapIdInFreeplane(removeNodeRequestJson.get("mapId").asText());
		final String nodeId = removeNodeRequestJson.get("nodeId").asText();
		Logger.debug("mapId: "+mapId+"; nodeId: "+nodeId);
		RemoveNodeRequest request = new RemoveNodeRequest(mapId,nodeId);
		
		ActorRef remoteActor = getRemoteActor();
		remoteActor.tell(request, remoteActor);
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

			}
			
			//just a hack, because we are currently using different ids for retrieval then supposed
			mmId = getMapIdFromFile(file);

			serverIdToMapIdMap.put(mapId, mmId);

			//send file to server and put in map
			ActorRef remoteActor = getRemoteActor();
			remoteActor.tell(new OpenMindMapRequest(FileUtils.readFileToString(file)),remoteActor);
		} catch (FileNotFoundException e) {
			Logger.error("can't find mindmap file", e);
		} catch (IOException e) {
			Logger.error("can't open mindmap file", e);
		} catch (URISyntaxException e) {
			Logger.error("can't open mindmap file", e);
		}
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
