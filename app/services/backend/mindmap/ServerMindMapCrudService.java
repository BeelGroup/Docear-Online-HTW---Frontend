package services.backend.mindmap;

import static akka.pattern.Patterns.ask;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.NoUserLoggedInException;
import models.backend.exceptions.UnauthorizedException;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.docear.messages.Messages.AddNodeRequest;
import org.docear.messages.Messages.AddNodeResponse;
import org.docear.messages.Messages.ChangeNodeRequest;
import org.docear.messages.Messages.ChangeNodeResponse;
import org.docear.messages.Messages.FetchMindmapUpdatesRequest;
import org.docear.messages.Messages.FetchMindmapUpdatesResponse;
import org.docear.messages.Messages.GetNodeRequest;
import org.docear.messages.Messages.GetNodeResponse;
import org.docear.messages.Messages.ListenToUpdateOccurrenceRequest;
import org.docear.messages.Messages.ListenToUpdateOccurrenceRespone;
import org.docear.messages.Messages.MindmapAsJsonReponse;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.Messages.OpenMindMapResponse;
import org.docear.messages.Messages.ReleaseLockRequest;
import org.docear.messages.Messages.ReleaseLockResponse;
import org.docear.messages.Messages.RequestLockRequest;
import org.docear.messages.Messages.RequestLockResponse;
import org.docear.messages.exceptions.MapNotFoundException;
import org.docear.messages.exceptions.NodeAlreadyLockedException;
import org.docear.messages.exceptions.NodeNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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
import play.mvc.Controller;
import scala.concurrent.Future;
import services.backend.user.UserService;
import util.backend.ZipUtils;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import com.typesafe.config.ConfigFactory;

import controllers.Secured;

@Profile("backendProd")
@Component
public class ServerMindMapCrudService implements MindMapCrudService {
	private Map<String, String> serverIdToMapIdMap = new HashMap<String, String>();
	private final String freeplaneActorUrl = Play.application().configuration().getString("backend.singleInstance.host");
	private ActorSystem system;
	private ActorRef remoteActor;
	private final long defaultTimeoutInMillis = 20000;// Play.application().configuration().getLong("services.backend.mindmap.MindMapCrudService.timeoutInMillis");

	@Autowired
	private UserService userService;

	@Override
	public Promise<String> mindMapAsJsonString(final String mapId, final Integer nodeCount) throws DocearServiceException, IOException {
		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => mapId: " + mapId);
		//check that user has right to access map, throws UnauthorizedEception on failure
		hasCurrentUserMapAccessRights(mapId);

		// hack, because we use 'wrong' ids at the moment because of docear
		// server ids
		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => converting serverId to mindmapId");
		final String mindmapId = getMindMapIdInFreeplane(mapId);
		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => mindmapId: " + mindmapId);

		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => getting remote actor");
		final ActorRef remoteActor = getRemoteActor();

		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => sending request to freeplane");

		try {
			final Promise<Object> promise = Akka.asPromise(ask(remoteActor, new MindmapAsJsonRequest(mindmapId, nodeCount), defaultTimeoutInMillis));

			final MindmapAsJsonReponse response = (MindmapAsJsonReponse) promise.get();
			Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => response received");
			final String jsonString = response.getJsonString();
			Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => returning map as json string : " + jsonString.substring(0, 10));
			return Promise.pure(jsonString);
		} catch (Throwable e) {
			if (e instanceof MapNotFoundException) {
				Logger.warn("ServerMindMapCrudService.mindMapAsJsonString => Map expected on server, but was not present. Reopening...", e);
				serverIdToMapIdMap.remove(mapId);
				return mindMapAsJsonString(mapId, nodeCount);
			} else {
				throw new RuntimeException(e);
			}
		}

	}

	@Override
	public Promise<Boolean> listenForUpdates(String mapId) {
		// refuse if map is not open
		if (!serverIdToMapIdMap.containsKey(mapId)) {
			return Promise.pure(false);
		}

		//check that user has right to access map, throws UnauthorizedEception on failure
		hasCurrentUserMapAccessRights(mapId);

		final String mindmapId = getMindMapIdInFreeplane(mapId);
		final ListenToUpdateOccurrenceRequest request = new ListenToUpdateOccurrenceRequest(mindmapId);

		Future<Object> future = ask(getRemoteActor(), request, 120000); // two
		// minutes
		// for
		// longpolling
		Promise<Boolean> promise = Akka.asPromise(future).map(new Function<Object, Boolean>() {

			@Override
			public Boolean apply(Object arg0) throws Throwable {
				final ListenToUpdateOccurrenceRespone response = (ListenToUpdateOccurrenceRespone) arg0;
				return response.getResult();
			}
		});

		return promise;
	}

	/**
	 * returns docear mapid In case the map is not loaded on a server, it gets
	 * automatically pushed to freeplane
	 * 
	 * @param mapId
	 * @return
	 */
	private String getMindMapIdInFreeplane(final String mapId) {
		Logger.info("ServerMindMapCrudService.getMindMapIdInFreeplane => idOnServer: " + mapId);
		String mindmapId = serverIdToMapIdMap.get(mapId);
		if (mindmapId == null) { // if not hosted, send to a server
			Logger.info("ServerMindMapCrudService.getMindMapIdInFreeplane => Map for server id " + mapId + " not open. Sending to freeplane...");
			try {
				final boolean success = sendMapToDocearInstance(mapId);
				if (!success) {
					throw new RuntimeException("problem sending mindmap to Docear");
				}
			} catch (NoUserLoggedInException e) {
				Logger.error("ServerMindMapCrudService.getMindMapIdInFreeplane => No user logged in! ", e);
				throw new RuntimeException("No user logged in", e);
			}
			mindmapId = serverIdToMapIdMap.get(mapId);
		}

		Logger.error("ServerMindMapCrudService.getMindMapIdInFreeplane => ServerId: " + mapId + "; MapId: " + mindmapId);
		return mindmapId;
	}

	@Override
	public Promise<String> createNode(final String mapId, final String parentNodeId, String username) {
		Logger.debug("mapId: " + mapId + "; parentNodeId: " + parentNodeId);

		//check that user has right to access map, throws UnauthorizedEception on failure
		hasCurrentUserMapAccessRights(mapId);

		final AddNodeRequest request = new AddNodeRequest(mapId, parentNodeId, username);

		final ActorRef remoteActor = getRemoteActor();
		final Future<Object> future = ask(remoteActor, request, defaultTimeoutInMillis);

		final Promise<String> promise = Akka.asPromise(future).map(new Function<Object, String>() {
			@Override
			public String apply(Object responseMessage) throws Throwable {
				AddNodeResponse response = (AddNodeResponse) responseMessage;
				return response.getMapUpdate();
			}
		});
		return promise;
	}

	@Override
	public Promise<String> getNode(final String mapId, final String nodeId, final Integer nodeCount) {
		Logger.debug("getNode => mapId: " + mapId + "; nodeId: " + nodeId + ", nodeCount: " + nodeCount);

		//check that user has right to access map, throws UnauthorizedEception on failure
		hasCurrentUserMapAccessRights(mapId);

		final GetNodeRequest request = new GetNodeRequest(mapId, nodeId, nodeCount);
		ActorRef remoteActor = getRemoteActor();
		Future<Object> future = ask(remoteActor, request, defaultTimeoutInMillis);

		Promise<String> promise = Akka.asPromise(future).map(new Function<Object, String>() {
			@Override
			public String apply(Object responseMessage) throws Throwable {
				GetNodeResponse response = (GetNodeResponse) responseMessage;
				return response.getNode().toString();
			}
		});
		return promise;
	}

	@Override
	public Promise<String> addNode(JsonNode addNodeRequestJson) {

		// final String mapId =
		// getMindMapIdInFreeplane(addNodeRequestJson.get("mapId").asText());
		// final String parentNodeId =
		// addNodeRequestJson.get("parentNodeId").asText();
		// Logger.debug("mapId: "+mapId+"; parentNodeId: "+parentNodeId);
		// AddNodeRequest request = new
		// AddNodeRequest(mapId,parentNodeId,username());
		//
		// ActorRef remoteActor = getRemoteActor();
		// Future<Object> future = ask(remoteActor, request,
		// defaultTimeoutInMillis);
		//
		// Promise<String> promise = Akka.asPromise(future).map(new
		// Function<Object, String>() {
		// @Override
		// public String apply(Object responseMessage) throws Throwable {
		// AddNodeResponse response = (AddNodeResponse)responseMessage;
		// return response.getMapUpdate();
		// }
		// });
		return null;// promise;
	}

	@Override
	public Promise<String> changeNode(String mapId, String nodeId, Map<String, Object> attributeValueMap, String username) {
		Logger.debug("ServerMindMapCrudService.changeNode => mapId: " + mapId + "; nodeId: " + nodeId + "; attributeMap: " + attributeValueMap.toString());

		//check that user has right to access map, throws UnauthorizedEception on failure
		hasCurrentUserMapAccessRights(mapId);

		try {		
			final ChangeNodeRequest request = new ChangeNodeRequest(mapId, nodeId, attributeValueMap, username);
			final ActorRef remoteActor = getRemoteActor();

			final Promise<Object> promise = Akka.asPromise(ask(remoteActor, request, defaultTimeoutInMillis));
			final ChangeNodeResponse response = (ChangeNodeResponse) promise.get();

			final String updatesListJson = new ObjectMapper().writeValueAsString(response.getMapUpdates());

			return Promise.pure(updatesListJson);
		} catch (JsonGenerationException e) {
			throw new RuntimeException("Problem reading updates from response", e);
		} catch (JsonMappingException e) {
			throw new RuntimeException("Problem reading updates from response", e);
		} catch (IOException e) {
			throw new RuntimeException("Problem reading updates from response", e);
		}
	}

	@Override
	public void removeNode(String mapId, String nodeId, String username) {
		// final String mapId =
		// getMindMapIdInFreeplane(removeNodeRequestJson.get("mapId").asText());
		// final String nodeId = removeNodeRequestJson.get("nodeId").asText();
		// Logger.debug("mapId: "+mapId+"; nodeId: "+nodeId);
		// RemoveNodeRequest request = new
		// RemoveNodeRequest(mapId,nodeId,username());
		//
		// ActorRef remoteActor = getRemoteActor();
		// remoteActor.tell(request, remoteActor);
	}

	@Override
	public Promise<String> fetchUpdatesSinceRevision(String mapId, Integer revision, String username) {
		Logger.debug("ServerMindMapCrudService.fetchUpdatesSinceRevision " +
				"=> mapId: " + mapId + "; revision: " + revision + "; username: " + username);

		//check that user has right to access map, throws UnauthorizedEception on failure
		hasCurrentUserMapAccessRights(mapId);
		
		try {
			//TODO use username for security
			final Promise<Object> promise = Akka.asPromise(ask(remoteActor, new FetchMindmapUpdatesRequest(mapId, revision), defaultTimeoutInMillis));
			final FetchMindmapUpdatesResponse response = (FetchMindmapUpdatesResponse)promise.get();
			final String json = new ObjectMapper().writeValueAsString(response);
			return Promise.pure(json);
		} catch (Throwable t) {
			if(t instanceof MapNotFoundException) {
				return Promise.pure("{}");
			} else {
				throw new RuntimeException(t);
			}
		}
	}

	@Override
	public Promise<Boolean> requestLock(String mapId, String nodeId, String username) {
		Logger.debug("ServerMindMapCrudService.requestLock => mapId: " + mapId + "; nodeId: " + nodeId + "; username: " + username);

		//check that user has right to access map, throws UnauthorizedEception on failure
		hasCurrentUserMapAccessRights(mapId);
		
		try {
			final Promise<Object> promise = Akka.asPromise(ask(remoteActor, new RequestLockRequest(mapId, nodeId, username), defaultTimeoutInMillis));
			final RequestLockResponse response = (RequestLockResponse) promise.get();
			return Promise.pure(response.getLockGained());

		} catch (Throwable t) {
			if (t instanceof MapNotFoundException || t instanceof NodeAlreadyLockedException || t instanceof NodeNotFoundException) {
				return Promise.pure(false);
			} else {
				throw new RuntimeException(t);
			}
		}
	}

	@Override
	public Promise<Boolean> releaseLock(String mapId, String nodeId, String username) {
		Logger.debug("ServerMindMapCrudService.releaseLock => mapId: " + mapId + "; nodeId: " + nodeId + "; username: " + username);

		//check that user has right to access map, throws UnauthorizedEception on failure
		hasCurrentUserMapAccessRights(mapId);
		
		try {
			final Promise<Object> promise = Akka.asPromise(ask(remoteActor, new ReleaseLockRequest(mapId, nodeId, username), defaultTimeoutInMillis));
			final ReleaseLockResponse response = (ReleaseLockResponse) promise.get();
			return Promise.pure(response.getLockReleased());

		} catch (Throwable t) {
			if (t instanceof MapNotFoundException || t instanceof NodeNotFoundException) {
				return Promise.pure(false);
			} else {
				throw new RuntimeException(t);
			}
		}
	}

	private Boolean sendMapToDocearInstance(String mapId) throws NoUserLoggedInException {
		Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => mapId: " + mapId);
		InputStream in = null;
		String fileName = null;

		//check that user has right to access map, throws UnauthorizedEception on failure
		hasCurrentUserMapAccessRights(mapId);
		
		try {
			if (mapId.length() == 1 || mapId.equals("welcome")) { // test/welcome
				// map
				Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map is demo/welcome map, loading from resources");
				in = Play.application().resourceAsStream("mindmaps/" + mapId + ".mm");
				fileName = mapId + ".mm";
			} else { // map from user account
				User user = controllers.User.getCurrentUser();
				if (user == null)
					throw new NoUserLoggedInException();

				final StringBuilder outfileName = new StringBuilder();
				Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map is real map, loading from docear server");
				in = getMindMapInputStreamFromDocearServer(user, mapId, outfileName);

				fileName = outfileName.toString();
				if (in == null) {
					Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map with serverId: " + mapId + " not found on docear server.");
					throw new FileNotFoundException("Map not found");
				}
				// Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map file: "+file.getAbsolutePath());
			}

			StringWriter writer = new StringWriter();
			IOUtils.copy(in, writer);
			final String fileContentAsString = writer.toString();

			// just a hack, because we are currently using different ids for
			// retrieval then supposed
			final String mmId = getMapIdFromMindmapXmlString(fileContentAsString);
			Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => real mindmapId: " + mmId);

			// send file to server and put in map
			serverIdToMapIdMap.put(mapId, mmId);
			final ActorRef remoteActor = getRemoteActor();
			try {
				final Promise<Object> promise = Akka.asPromise(ask(remoteActor, new OpenMindMapRequest(fileContentAsString, fileName), defaultTimeoutInMillis));
				final OpenMindMapResponse response = (OpenMindMapResponse) promise.get();
				return response.getSuccess();
			} catch (Throwable t) {
				return false;
			}

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
		Logger.debug("getMindMapFileFromDocearServer => calling URL: '" + resource + "'");
		Logger.debug(user.getAccessToken());
		WS.Response response = WS.url(resource).setHeader("accessToken", user.getAccessToken()).get().get();

		if (response.getStatus() == 200) {
			return ZipUtils.getMindMapInputStream(response.getBodyAsStream(), outFileName);
		} else {
			return null;
		}
	}

	private static String getMapIdFromMindmapXmlString(String xmlString) {
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(xmlString));
			Document doc = dBuilder.parse(is);
			doc.getDocumentElement().normalize();

			return doc.getDocumentElement().getAttribute("dcr_id");

		} catch (Exception e) {
			Logger.error("getMapIdFromFile failed", e);
			// throw new RuntimeException(e);
		}

		return null;
	}

	private ActorRef getRemoteActor() {
		if (system == null) {
			system = ActorSystem.create("freeplaneSystem", ConfigFactory.load().getConfig("local"));
		}
		// if(remoteActor == null || remoteActor.isTerminated()) {
		remoteActor = system.actorFor(freeplaneActorUrl);
		Logger.debug("Connection to " + remoteActor.path() + " established");
		// }

		return remoteActor;
	}

	/**
	 * @throws UnauthorizedException 
	 * @param mapId
	 * @return true or throws {@link UnauthorizedException}
	 */
	private boolean hasCurrentUserMapAccessRights(String mapId) {
		//check for demo and welcome map
		if(mapId.length() == 1 || mapId.equals("welcome"))
			return true;

		try {
			Logger.debug("ServerMindMapCrudService.hasCurrentUserMapAccessRights => mapId:"+mapId);
			List<UserMindmapInfo> infos = userService.getListOfMindMapsFromUser(controllers.User.getCurrentUser()).get();

			Logger.debug("ServerMindMapCrudService.hasCurrentUserMapAccessRights => loaded mapInfos. Count: "+infos.size());
			boolean canAccess = false;
			for(UserMindmapInfo info : infos) {
				if(info.mmIdOnServer.equals(mapId)) {
					canAccess = true;
					break;
				}
			}

			if(!canAccess) {
				Logger.warn("User '"+Controller.session(Secured.SESSION_KEY_USERNAME)+"' tried to access a map he/she does not own!");
				throw new UnauthorizedException("User tried to access not owned map");
			}
			return canAccess;

		} catch (IOException e) {
			throw new RuntimeException("Cannot access Docear server!", e);
		}
	}

	// /**
	// *
	// * @return name of currently logged in user
	// */
	// private String username() {
	// return controllers.User.getCurrentUser().getUsername();
	// }

}
