package services.backend.mindmap;

import static akka.pattern.Patterns.ask;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.NoUserLoggedInException;
import models.backend.exceptions.UnauthorizedException;

import org.apache.commons.io.IOUtils;
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
import org.docear.messages.Messages.MindMapRequest;
import org.docear.messages.Messages.MindmapAsJsonReponse;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.MindmapAsXmlRequest;
import org.docear.messages.Messages.MindmapAsXmlResponse;
import org.docear.messages.Messages.MoveNodeToRequest;
import org.docear.messages.Messages.MoveNodeToResponse;
import org.docear.messages.Messages.OpenMindMapRequest;
import org.docear.messages.Messages.OpenMindMapResponse;
import org.docear.messages.Messages.ReleaseLockRequest;
import org.docear.messages.Messages.ReleaseLockResponse;
import org.docear.messages.Messages.RemoveNodeRequest;
import org.docear.messages.Messages.RemoveNodeResponse;
import org.docear.messages.Messages.RequestLockRequest;
import org.docear.messages.Messages.RequestLockResponse;
import org.docear.messages.exceptions.MapNotFoundException;
import org.docear.messages.exceptions.NodeAlreadyLockedException;
import org.docear.messages.exceptions.NodeNotFoundException;
import org.docear.messages.exceptions.NodeNotLockedByUserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;
import play.mvc.Controller;
import services.backend.user.UserService;
import util.backend.ZipUtils;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import com.typesafe.config.ConfigFactory;

import controllers.Secured;

@Profile("backendProd")
@Component
/**
 * 
 * production implementation of MindmapCrudService interface.
 * Handles communication with Freeplane
 *
 */
public class ServerMindMapCrudService implements MindMapCrudService {
	private Set<String> openMapIds = new HashSet<String>();
	private final String freeplaneActorUrl = Play.application().configuration().getString("backend.singleInstance.host");
	private final ActorSystem system;
	private final ActorRef remoteActor;
	private final long defaultTimeoutInMillis = Play.application().configuration().getLong("services.backend.mindmap.MindMapCrudService.timeoutInMillis");

	@Autowired
	private UserService userService;

	public ServerMindMapCrudService() {
		system = ActorSystem.create("freeplaneSystem", ConfigFactory.load().getConfig("local"));
		remoteActor = system.actorFor(freeplaneActorUrl);
	}

	@Override
	public Promise<String> mindMapAsJsonString(String source, String username, final String mapId, final Integer nodeCount) throws DocearServiceException, IOException {
		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => mapId: " + mapId);

		final MindmapAsJsonRequest request = new MindmapAsJsonRequest(source, username, mapId, nodeCount);

		return performActionOnMindMap(request, new ActionOnMindMap<String>() {
			@Override
			public Promise<String> perform(Promise<Object> promise) throws Exception {

				final MindmapAsJsonReponse response = (MindmapAsJsonReponse) promise.get();
				Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => response received");
				final String jsonString = response.getJsonString();
				Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => returning map as json string : " + jsonString.substring(0, 10));
				return Promise.pure(jsonString);

			}
		});
	}

	@Override
	public Promise<String> mindMapAsXmlString(String source, String username, String mapId) throws DocearServiceException, IOException {
		Logger.debug("ServerMindMapCrudService.mindMapAsXmlString => mapId: " + mapId);

		final MindmapAsXmlRequest request = new MindmapAsXmlRequest(source, username, mapId);

		return performActionOnMindMap(request, new ActionOnMindMap<String>() {
			@Override
			public Promise<String> perform(Promise<Object> promise) throws Exception {
				final MindmapAsXmlResponse response = (MindmapAsXmlResponse) promise.get();
				Logger.debug("ServerMindMapCrudService.mindMapAsXmlString => response received");
				String xmlString = new ObjectMapper().writeValueAsString(response);

				Logger.debug("ServerMindMapCrudService.mindMapAsXmlString => returning map as xml string : " + xmlString.substring(0, 10));
				return Promise.pure(xmlString);
			}
		});
	}

	@Override
	public Promise<Boolean> listenForUpdates(String source, String username, final String mapId) {
		final ListenToUpdateOccurrenceRequest request = new ListenToUpdateOccurrenceRequest(source, username, mapId);

		// two minutes for longpolling
		final long twoMinutesInMillis = 120000;
		return performActionOnMindMap(request, twoMinutesInMillis, new ActionOnMindMap<Boolean>() {

			@Override
			public Promise<Boolean> perform(Promise<Object> promise) throws Exception {
				return promise.map(new Function<Object, Boolean>() {

					@Override
					public Boolean apply(Object arg0) throws Throwable {
						final ListenToUpdateOccurrenceRespone response = (ListenToUpdateOccurrenceRespone) arg0;
						return response.getResult();
					}
				}).recover(new Function<Throwable, Boolean>() {

					@Override
					public Boolean apply(Throwable t) throws Throwable {
						/*
						 * When map was not found, something must have happened
						 * since the last interaction Probably the laptop was in
						 * standby and tries now to reconnect, which should
						 * result in a reload. When the frontend tries to load
						 * updates, the map will be send to a server
						 */
						if (t instanceof MapNotFoundException) {
							return true;
						}
						return false;
					}
				});

			}
		});
	}

	@Override
	public Promise<String> createNode(String source, String username, final String mapId, final String parentNodeId) {
		Logger.debug("mapId: " + mapId + "; parentNodeId: " + parentNodeId);
		final AddNodeRequest request = new AddNodeRequest(source, username, mapId, parentNodeId);

		final Promise<String> promise = performActionOnMindMap(request, new ActionOnMindMap<String>() {

			@Override
			public Promise<String> perform(Promise<Object> promise) throws Exception {
				final AddNodeResponse response = (AddNodeResponse) promise.get();
				return Promise.pure(response.getMapUpdate());
			}
		});

		return promise;
	}

	@Override
	public Promise<String> getNode(String source, String username, final String mapId, final String nodeId, final Integer nodeCount) {
		Logger.debug("getNode => mapId: " + mapId + "; nodeId: " + nodeId + ", nodeCount: " + nodeCount);
		final GetNodeRequest request = new GetNodeRequest(source, username, mapId, nodeId, nodeCount);

		final Promise<String> promise = performActionOnMindMap(request, new ActionOnMindMap<String>() {
			@Override
			public Promise<String> perform(Promise<Object> promise) throws Exception {
				return Promise.pure(((GetNodeResponse) promise.get()).getNode());
			}
		});
		return promise;
	}

	@Override
	public Promise<String> changeNode(String source, String username, String mapId, String nodeId, Map<String, Object> attributeValueMap) {
		Logger.debug("ServerMindMapCrudService.changeNode => mapId: " + mapId + "; nodeId: " + nodeId + "; attributeMap: " + attributeValueMap.toString());

		final ChangeNodeRequest request = new ChangeNodeRequest(source, username, mapId, nodeId, attributeValueMap);

		return performActionOnMindMap(request, new ActionOnMindMap<String>() {

			@Override
			public Promise<String> perform(Promise<Object> promise) throws Exception {
				final ChangeNodeResponse response = (ChangeNodeResponse) promise.get();
				return Promise.pure(new ObjectMapper().writeValueAsString(response.getMapUpdates()));
			}
		});
	}

	@Override
	public Promise<Boolean> moveNodeTo(String source, String username, String mapId, String newParentNodeId, String nodetoMoveId, Integer newIndex) {
		Logger.debug("ServerMindMapCrudService.moveNodeTo => mapId: " + mapId + "; newParentNodeId: " + newParentNodeId + "; nodeId: " + nodetoMoveId + "; newIndex: " + newIndex);

		final MoveNodeToRequest request = new MoveNodeToRequest(source, username, mapId, newParentNodeId, nodetoMoveId, newIndex);

		return performActionOnMindMap(request, new ActionOnMindMap<Boolean>() {

			@Override
			public Promise<Boolean> perform(Promise<Object> promise) throws Exception {
				final MoveNodeToResponse response = (MoveNodeToResponse) promise.get();
				return Promise.pure(response.getSuccess());
			}
		});
	}

	@Override
	public Promise<Boolean> removeNode(String source, String username, String mapId, String nodeId) {
		Logger.debug("ServerMindMapCrudService.removeNode => mapId: " + mapId + "; nodeId: " + nodeId + "; username: " + username);
		final RemoveNodeRequest request = new RemoveNodeRequest(source, username, mapId, nodeId);

		return performActionOnMindMap(request, new ActionOnMindMap<Boolean>() {

			@Override
			public Promise<Boolean> perform(Promise<Object> promise) throws Exception {
				final RemoveNodeResponse response = (RemoveNodeResponse) promise.get();
				return Promise.pure(response.getDeleted());
			}
		});
	}

	@Override
	public Promise<String> fetchUpdatesSinceRevision(String source, String username, String mapId, Integer revision) {
		Logger.debug("ServerMindMapCrudService.fetchUpdatesSinceRevision " + "=> mapId: " + mapId + "; revision: " + revision + "; username: " + username);
		final FetchMindmapUpdatesRequest request = new FetchMindmapUpdatesRequest(source, username, mapId, revision);

		return performActionOnMindMap(request, new ActionOnMindMap<String>() {
			@Override
			public Promise<String> perform(Promise<Object> promise) throws Exception {
				final FetchMindmapUpdatesResponse response = (FetchMindmapUpdatesResponse) promise.get();
				final String json = new ObjectMapper().writeValueAsString(response);
				return Promise.pure(json);
			}
		});
	}

	@Override
	public Promise<Boolean> requestLock(String source, String username, String mapId, String nodeId) {
		Logger.debug("ServerMindMapCrudService.requestLock => mapId: " + mapId + "; nodeId: " + nodeId + "; username: " + username);
		final RequestLockRequest request = new RequestLockRequest(source, username, mapId, nodeId);
		Logger.debug("username: " + request.getUsername());
		return performActionOnMindMap(request, new ActionOnMindMap<Boolean>() {
			@Override
			public Promise<Boolean> perform(Promise<Object> promise) throws Exception {
				final RequestLockResponse response = (RequestLockResponse) promise.get();
				return Promise.pure(response.getLockGained());
			}

			@Override
			public Promise<Boolean> handleException(Throwable t) {
				if (t instanceof NodeAlreadyLockedException || t instanceof NodeNotFoundException) {
					return Promise.pure(false);
				}
				return super.handleException(t);
			}
		});
	}

	@Override
	public Promise<Boolean> releaseLock(String source, String username, String mapId, String nodeId) {
		Logger.debug("ServerMindMapCrudService.releaseLock => mapId: " + mapId + "; nodeId: " + nodeId + "; username: " + username);

		final ReleaseLockRequest request = new ReleaseLockRequest(source, username, mapId, nodeId);

		return performActionOnMindMap(request, new ActionOnMindMap<Boolean>() {
			@Override
			public Promise<Boolean> perform(Promise<Object> promise) throws Exception {
				final ReleaseLockResponse response = (ReleaseLockResponse) promise.get();
				return Promise.pure(response.getLockReleased());
			}

			@Override
			public Promise<Boolean> handleException(Throwable t) {
				if (t instanceof NodeNotFoundException) {
					return Promise.pure(false);
				}
				return super.handleException(t);
			}
		});
	}

	/**
	 * Central point to perform an action on a mindmap. Helps to centralise
	 * error handling
	 */
	private <A> Promise<A> performActionOnMindMap(MindMapRequest message, ActionOnMindMap<A> actionOnMindMap) {
		return performActionOnMindMap(message, defaultTimeoutInMillis, actionOnMindMap);
	}

	/**
	 * Central point to perform an action on a mindmap. Helps to centralise
	 * error handling and reduce code duplication
	 */
	private <A> Promise<A> performActionOnMindMap(final MindMapRequest message, final long timeoutInMillis, final ActionOnMindMap<A> actionOnMindMap) {
		Logger.debug("ServerMindMapCrudService.performActionOnMindMap => message type: " + message.getClass().getSimpleName());
		final String mapId = message.getMapId();

		// Save the user for the current request
		final User user = user();
		// check that user has right to access map
		// throws UnauthorizedException on failure
		hasUserMapAccessRights(user, mapId);

		Promise<A> result = null;
		try {
			Logger.debug("ServerMindMapCrudService.performActionOnMindMap => sending request to freeplane");
			final Promise<Object> promise = sendMessageToServer(message, timeoutInMillis);
			result = actionOnMindMap.perform(promise);
		} catch (Exception e) {
			Logger.debug("ServerMindMapCrudService.performActionOnMindMap => Exception catched! Type: " + e.getClass().getSimpleName());
			// check if exception is handled by action
			result = actionOnMindMap.handleException(e);
			Logger.debug("ServerMindMapCrudService.performActionOnMindMap => Exception handled by action: " + (result != null));

			if (result == null) { // exception was not handled by action
				if (e instanceof MapNotFoundException) {
					final MapNotFoundException exception = (MapNotFoundException) e;
					// Map was closed on server, reopen and perform action again
					Logger.info("ServerMindMapCrudService.performActionOnMindMap => mind map was not present in freeplane. Reopening...");
					final String mapIdNotFound = exception.getMapId();
					sendMindMapToServer(message.getSource(), message.getUsername(), mapIdNotFound);
					Logger.debug("ServerMindMapCrudService.performActionOnMindMap => re-sending request to freeplane");
					final Promise<Object> promise = sendMessageToServer(message, timeoutInMillis);
					try {
						result = actionOnMindMap.perform(promise);
					} catch (Exception e2) {
						throw new RuntimeException("erverMindMapCrudService.performActionOnMindMap => Second attempt failed. ", e2);
					}
				} else if (e instanceof NodeNotLockedByUserException) {
					// TODO correct handling
					throw new RuntimeException(e);
				} else {
					throw new RuntimeException(e);
				}
			}
		}

		return result;
	}

	private Promise<Object> sendMessageToServer(Object message, long timeoutInMillis) {
		return Akka.asPromise(ask(remoteActor, message, timeoutInMillis));
	}

	private Boolean sendMindMapToServer(String source, String username, String mapId) throws NoUserLoggedInException {
		Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => mapId: " + mapId);
		InputStream in = null;
		String fileName = null;

		try {
			// test & welcome maps
			if (mapId.length() == 1 || mapId.equals("welcome")) {
				Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map is demo/welcome map, loading from resources");
				in = Play.application().resourceAsStream("mindmaps/" + mapId + ".mm");
				fileName = mapId + ".mm";
			}
			// map from user account
			else {
				final User user = user();
				if (user == null)
					throw new NoUserLoggedInException();

				final StringBuilder outfileName = new StringBuilder();
				Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map is real map, loading from docear server");
				final byte[] filebytes = getMindMapInputStreamFromDocearServer(user, mapId, outfileName);
				
				if (filebytes == null) {
					Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map with serverId: " + mapId + " was not in zip file.");
					throw new FileNotFoundException("Map not found");
				}
				
				fileName = outfileName.toString();
				in = new ByteArrayInputStream(filebytes);
			}

			// copy map data to a string
			StringWriter writer = new StringWriter();
			IOUtils.copy(in, writer);
			final String fileContentAsString = writer.toString();

			// send file to server and put in open maps set
			openMapIds.add(mapId);

			final OpenMindMapRequest request = new OpenMindMapRequest(source, username, mapId, fileContentAsString, fileName);

			return performActionOnMindMap(request, new ActionOnMindMap<Boolean>() {
				@Override
				public Promise<Boolean> perform(Promise<Object> promise) throws Exception {
					;
					final OpenMindMapResponse response = (OpenMindMapResponse) promise.get();
					return Promise.pure(response.getSuccess());
				}

			}).get();

		} catch (IOException e) {
			Logger.error("ServerMindMapCrudService.sendMapToDocearInstance => can't open mindmap file", e);
			throw new RuntimeException("ServerMindMapCrudService.sendMapToDocearInstance => can't find mindmap file", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private static byte[] getMindMapInputStreamFromDocearServer(final User user, final String mmIdOnServer, final StringBuilder outFileName) throws IOException {

		final String docearServerAPIURL = "https://api.docear.org/user";
		final String resource = docearServerAPIURL + "/" + user.getUsername() + "/mindmaps/" + mmIdOnServer;
		Logger.debug("getMindMapFileFromDocearServer => calling URL: '" + resource + "'");
		WS.Response response = WS.url(resource).setHeader("accessToken", user.getAccessToken()).get().get();

		if (response.getStatus() == 200) {
			return ZipUtils.getMindMapInputStream(response.getBodyAsStream(), outFileName);
		} else if (response.getStatus() == 403) {
			throw new UnauthorizedException("User tried to access not owned map");
		} else {
			throw new RuntimeException("Problem retrieving map from docear server. Status: " + response.getStatus() + " - " + response.getStatusText());
		}
	}

	/**
	 * @throws UnauthorizedException
	 * @param mapId
	 * @return true or throws {@link UnauthorizedException}
	 */
	private boolean hasUserMapAccessRights(User user, String mapId) {
		// check for demo and welcome map
		if (mapId.length() == 1 || mapId.equals("welcome"))
			return true;

		try {
			Logger.debug("ServerMindMapCrudService.hasCurrentUserMapAccessRights => mapId:" + mapId);
			List<UserMindmapInfo> infos = userService.getListOfMindMapsFromUser(user).get();

			Logger.debug("ServerMindMapCrudService.hasCurrentUserMapAccessRights => loaded mapInfos. Count: " + infos.size());
			boolean canAccess = false;
			for (UserMindmapInfo info : infos) {
				if (info.mmIdOnServer.equals(mapId)) {
					canAccess = true;
					break;
				}
			}

			if (!canAccess) {
				Logger.warn("User '" + Controller.session(Secured.SESSION_KEY_USERNAME) + "' tried to access a map he/she does not own!");
				throw new UnauthorizedException("User tried to access not owned map");
			}
			return canAccess;

		} catch (IOException e) {
			throw new RuntimeException("Cannot access Docear server!", e);
		}
	}

	private User user() {
		return userService.getCurrentUser();
	}

}
