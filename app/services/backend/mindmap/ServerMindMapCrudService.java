package services.backend.mindmap;

import static akka.pattern.Patterns.ask;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import models.backend.User;
import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.NoUserLoggedInException;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.docear.messages.Messages.AddNodeRequest;
import org.docear.messages.Messages.AddNodeResponse;
import org.docear.messages.Messages.ChangeNodeRequest;
import org.docear.messages.Messages.GetNodeRequest;
import org.docear.messages.Messages.GetNodeResponse;
import org.docear.messages.Messages.ListenToUpdateOccurrenceRequest;
import org.docear.messages.Messages.ListenToUpdateOccurrenceRespone;
import org.docear.messages.Messages.MindmapAsJsonReponse;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.Messages.RemoveNodeRequest;
import org.docear.messages.exceptions.MapNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

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
public class ServerMindMapCrudService implements MindMapCrudService {
	private Map<String, String> serverIdToMapIdMap = new HashMap<String, String>();
	private final String freeplaneActorUrl = Play.application().configuration().getString("backend.singleInstance.host");
	private final ObjectMapper objectMapper = new ObjectMapper();
	private ActorSystem system;
	private ActorRef remoteActor;
	private final long defaultTimeoutInMillis = 20000;//Play.application().configuration().getLong("services.backend.mindmap.MindMapCrudService.timeoutInMillis");

	@Override
	public Promise<String> mindMapAsJsonString(final String id,final Integer nodeCount)
			throws DocearServiceException, IOException {
		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => mapId: " + id);

		//hack, because we use 'wrong' ids at the moment because of docear server ids
		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => converting serverId to mindmapId");
		final String mindmapId = getMindMapIdInFreeplane(id);
		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => mindmapId: "+mindmapId);

		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => getting remote actor");
		final ActorRef remoteActor = getRemoteActor();

		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => sending request to freeplane");

		try {
			final Future<Object> future = ask(remoteActor, new MindmapAsJsonRequest(mindmapId,nodeCount), defaultTimeoutInMillis);
			final Promise<Object> prom = Akka.asPromise(future);

			
			final MindmapAsJsonReponse response = (MindmapAsJsonReponse)prom.get();
			Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => response received");
			final String jsonString = response.getJsonString();
			Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => returning map as json string : "+jsonString.substring(0, 10));
			return Promise.pure(jsonString);
		} catch (Throwable e) {
			if(e instanceof MapNotFoundException) {
				Logger.warn("ServerMindMapCrudService.mindMapAsJsonString => Map expected on server, but was not present. Reopening...",e);
				serverIdToMapIdMap.remove(id);
				return mindMapAsJsonString(id,nodeCount);
			} else {
				throw new RuntimeException(e);
			}
		}

		//		Promise<String> promise = Akka.asPromise(future).map(
		//				new Function<Object, String>() {
		//					@Override
		//					public String apply(Object message) throws Throwable {
		//						Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => response received");
		//						final MindmapAsJsonReponse response = (MindmapAsJsonReponse)message;
		//						final String jsonString = response.getJsonString();
		//						Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => returning map as json string : "+jsonString.substring(0, 10));
		//						return jsonString;
		//					}
		//				}).recover(new Function<Throwable, String>() {
		//					@Override
		//					public String apply(Throwable t) throws Throwable {
		//						if(t instanceof MapNotFoundException) {
		//							Logger.warn("ServerMindMapCrudService.mindMapAsJsonString => Map expected on server, but was not present. Reopening...",t);
		//							serverIdToMapIdMap.remove(id);
		//							return mindMapAsJsonString(id,nodeCount).get();
		//						} else {
		//							Logger.error("ServerMindMapCrudService.mindMapAsJsonString => unexpected Exception! ",t);
		//							throw t;
		//						}
		//					}
		//				});
		//
		//		return promise;
	}

	@Override
	public Promise<Boolean> listenForUpdates(String serverMapId) {
		//refuse if map is not open
		if(!serverIdToMapIdMap.containsKey(serverMapId)) {
			return Promise.pure(false);
		}

		final String mindmapId = getMindMapIdInFreeplane(serverMapId);
		final ListenToUpdateOccurrenceRequest request = new ListenToUpdateOccurrenceRequest(mindmapId);

		Future<Object> future =  ask(getRemoteActor(), request, 120000); //two minutes for longpolling
		Promise<Boolean> promise = Akka.asPromise(future).map(new Function<Object, Boolean>() {

			@Override
			public Boolean apply(Object arg0) throws Throwable {
				final ListenToUpdateOccurrenceRespone response = (ListenToUpdateOccurrenceRespone)arg0;
				return response.getResult();
			}
		});

		return promise;
	}

	/**
	 * returns docear mapid
	 * In case the map is not loaded on a server, it gets automatically pushed to freeplane
	 * @param id
	 * @return
	 */
	private String getMindMapIdInFreeplane(final String id) {
		Logger.info("ServerMindMapCrudService.getMindMapIdInFreeplane => idOnServer: "+id);
		String mindmapId = serverIdToMapIdMap.get(id);
		if(mindmapId == null) { //if not hosted, send to a server
			Logger.info("ServerMindMapCrudService.getMindMapIdInFreeplane => Map for server id " + id + " not open. Sending to freeplane...");
			try {
				final boolean success = sendMapToDocearInstance(id);
				if(!success) {
					throw new RuntimeException("problem sending mindmap to Docear");
				}
			} catch (NoUserLoggedInException e) {
				Logger.error("ServerMindMapCrudService.getMindMapIdInFreeplane => No user logged in! ",e);
				throw new RuntimeException("No user logged in", e);
			}
			mindmapId = serverIdToMapIdMap.get(id);
		}

		Logger.error("ServerMindMapCrudService.getMindMapIdInFreeplane => ServerId: " + id + "; MapId: " + mindmapId);
		return mindmapId;
	}

	@Override
	public Promise<String> createNode(final String mapId, final String parentNodeId) {
		Logger.debug("mapId: "+mapId+"; parentNodeId: "+parentNodeId);
		AddNodeRequest request = new AddNodeRequest(mapId,parentNodeId,username());

		ActorRef remoteActor = getRemoteActor();
		Future<Object> future = ask(remoteActor, request, defaultTimeoutInMillis);

		Promise<String> promise = Akka.asPromise(future).map(new Function<Object, String>() {
			@Override
			public String apply(Object responseMessage) throws Throwable {
				AddNodeResponse response = (AddNodeResponse)responseMessage;
				return response.getNode().toString();
			}
		});
		return promise;
	}

	@Override
	public Promise<String> getNode(final String mapId, final String nodeId, final Integer nodeCount) {
		Logger.debug("getNode => mapId: "+mapId+"; nodeId: "+nodeId+ ", nodeCount: "+nodeCount);
		final GetNodeRequest request = new GetNodeRequest(mapId,nodeId, nodeCount);

		ActorRef remoteActor = getRemoteActor();
		Future<Object> future = ask(remoteActor, request, defaultTimeoutInMillis);

		Promise<String> promise = Akka.asPromise(future).map(new Function<Object, String>() {
			@Override
			public String apply(Object responseMessage) throws Throwable {
				GetNodeResponse response = (GetNodeResponse)responseMessage;
				return response.getNode().toString();
			}
		});
		return promise;
	}


	@Override
	public Promise<JsonNode> addNode(JsonNode addNodeRequestJson) {

		final String mapId = getMindMapIdInFreeplane(addNodeRequestJson.get("mapId").asText());
		final String parentNodeId = addNodeRequestJson.get("parentNodeId").asText();
		Logger.debug("mapId: "+mapId+"; parentNodeId: "+parentNodeId);
		AddNodeRequest request = new AddNodeRequest(mapId,parentNodeId,username());

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
	public void changeNode(String mapId, String nodeJson) {


		Logger.debug("mapId: "+mapId+"; nodeAsJsonString: "+nodeJson);
		ChangeNodeRequest request = new ChangeNodeRequest(mapId,nodeJson,username());

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
		RemoveNodeRequest request = new RemoveNodeRequest(mapId,nodeId,username());

		ActorRef remoteActor = getRemoteActor();
		remoteActor.tell(request, remoteActor);
	}

	private Boolean sendMapToDocearInstance(String mapId) throws NoUserLoggedInException {
		Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => mapId: "+mapId);
		InputStream in = null;		
		String fileName = null; 

		try {
			if(mapId.length() == 1 || mapId.equals("welcome")) { //test/welcome map
				Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map is demo/welcome map, loading from resources");
				in = Play.application().resourceAsStream("mindmaps/"+mapId+".mm");
				fileName = mapId+".mm";
			} else { //map from user account
				User user = controllers.User.getCurrentUser();
				if(user == null)
					throw new NoUserLoggedInException();

				final StringBuilder outfileName = new StringBuilder();
				Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map is real map, loading from docear server");
				in = getMindMapInputStreamFromDocearServer(user, mapId,outfileName);

				fileName = outfileName.toString();
				if(in == null) {
					Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map with serverId: "+mapId+" not found on docear server.");
					throw new FileNotFoundException("Map not found");
				}
				//Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map file: "+file.getAbsolutePath());
			}

			StringWriter writer = new StringWriter();
			IOUtils.copy(in, writer);
			final String fileContentAsString = writer.toString();

			//just a hack, because we are currently using different ids for retrieval then supposed
			final String mmId = getMapIdFromMindmapXmlString(fileContentAsString);
			Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => real mindmapId: "+mmId);

			//send file to server and put in map
			serverIdToMapIdMap.put(mapId, mmId);
			final ActorRef remoteActor = getRemoteActor();
			remoteActor.tell(new OpenMindMapRequest(fileContentAsString,fileName), null);
			return true;
			//			final Future<Object> future = 
			//					ask(remoteActor, new OpenMindMapRequest(fileContentAsString,fileName), defaultTimeoutInMillis);
			//					.map(new Mapper<Object, Boolean>() {
			//						@Override
			//						public Boolean apply(Object parameter) {
			//							return true;
			//						}
			//					}, system.dispatcher())
			//					.recover(new Recover<Boolean>() {
			//
			//						@Override
			//						public Boolean recover(Throwable arg0) throws Throwable {
			//							return false;
			//						}
			//
			//					}, system.dispatcher());


			//			return Akka.asPromise(future).map(new Function<Object, Boolean>() {
			//
			//				@Override
			//				public Boolean apply(Object response) throws Throwable {
			//
			//					return ((OpenMindMapResponse)response).getSuccess();
			//				}
			//			})
			//			.recover(new Function<Throwable, Boolean>() {
			//
			//				@Override
			//				public Boolean apply(Throwable arg0) throws Throwable {
			//					return false;
			//				}
			//			})
			//			.get();

		} catch (FileNotFoundException e) {
			Logger.error("ServerMindMapCrudService.sendMapToDocearInstance => can't find mindmap file", e);
			throw new RuntimeException("ServerMindMapCrudService.sendMapToDocearInstance => can't find mindmap file");
		} catch (IOException e) {
			Logger.error("ServerMindMapCrudService.sendMapToDocearInstance => can't open mindmap file", e);
			throw new RuntimeException("ServerMindMapCrudService.sendMapToDocearInstance => can't find mindmap file");
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private static InputStream getMindMapInputStreamFromDocearServer(final User user, final String mmIdOnServer, final StringBuilder outFileName) throws IOException {

		final String docearServerAPIURL = "https://api.docear.org/user";
		final String resource = docearServerAPIURL + "/" + user.getUsername() + "/mindmaps/" + mmIdOnServer;
		Logger.debug("getMindMapFileFromDocearServer => calling URL: '"+resource+"'");
		Logger.debug(user.getAccessToken());
		WS.Response response =  WS.url(resource)
				.setHeader("accessToken", user.getAccessToken())
				.get().get();

		if(response.getStatus() == 200) {
			return ZipUtils.getMindMapInputStream(response.getBodyAsStream(),outFileName);
		} else {
			return null;
		}
	}

	private static String getMapIdFromMindmapXmlString(String xmlString) {
		try {
			DocumentBuilderFactory dbFactory =  DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(xmlString));
			Document doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();

			return doc.getDocumentElement().getAttribute("dcr_id");

		} catch (Exception e) {
			Logger.error("getMapIdFromFile failed", e);
			//throw new RuntimeException(e);
		}


		return null;
	}

	private ActorRef getRemoteActor() {
		if(system == null) {
			system = ActorSystem.create("freeplaneSystem",ConfigFactory.load().getConfig("local"));
		}
		//if(remoteActor == null || remoteActor.isTerminated()) {
		remoteActor = system.actorFor(freeplaneActorUrl);
		Logger.debug("Connection to "+remoteActor.path()+" established");
		//}

		return remoteActor;
	}

	/**
	 * 
	 * @return name of currently logged in user
	 */
	private String username() {
		return controllers.User.getCurrentUser().getUsername();
	}

}
