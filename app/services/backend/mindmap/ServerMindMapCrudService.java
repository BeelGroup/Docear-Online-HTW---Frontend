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
import java.util.zip.ZipInputStream;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.NoUserLoggedInException;
import models.backend.exceptions.sendResult.PreconditionFailedException;
import models.backend.exceptions.sendResult.UnauthorizedException;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.docear.messages.Messages.AddNodeRequest;
import org.docear.messages.Messages.AddNodeResponse;
import org.docear.messages.Messages.ChangeEdgeRequest;
import org.docear.messages.Messages.ChangeEdgeResponse;
import org.docear.messages.Messages.ChangeNodeRequest;
import org.docear.messages.Messages.ChangeNodeResponse;
import org.docear.messages.Messages.FetchMindmapUpdatesRequest;
import org.docear.messages.Messages.FetchMindmapUpdatesResponse;
import org.docear.messages.Messages.GetNodeRequest;
import org.docear.messages.Messages.GetNodeResponse;
import org.docear.messages.Messages.ListenToUpdateOccurrenceRequest;
import org.docear.messages.Messages.ListenToUpdateOccurrenceResponse;
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
import org.docear.messages.models.MapIdentifier;
import org.docear.messages.models.UserIdentifier;
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
import services.backend.project.ProjectService;
import services.backend.user.UserService;
import util.backend.ZipUtils;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import com.typesafe.config.ConfigFactory;

import controllers.MindMap;
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
	private Set<MapIdentifier> openMapIds = new HashSet<MapIdentifier>();
	private final ActorSystem system;
	private final ActorRef remoteActor;
	private final long defaultTimeoutInMillis = Play.application().configuration().getLong("services.backend.mindmap.MindMapCrudService.timeoutInMillis");

	@Autowired
	private UserService userService;

	@Autowired
	private ProjectService projectService;

	public ServerMindMapCrudService() {
		final String freeplaneActorUrl = Play.application().configuration().getString("backend.singleInstance.host");
		system = ActorSystem.create("freeplaneSystem", ConfigFactory.load().getConfig("local"));
		remoteActor = system.actorFor(freeplaneActorUrl);
	}

	@Override
	public Promise<String> mindMapAsJsonString(final UserIdentifier userIdentifier, final MapIdentifier mapIdentifier, final Integer nodeCount) throws DocearServiceException, IOException {
		Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier);

		final MindmapAsJsonRequest request = new MindmapAsJsonRequest(userIdentifier, mapIdentifier, nodeCount);

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
	public Promise<String> mindMapAsXmlString(UserIdentifier userIdentifier, MapIdentifier mapIdentifier) throws DocearServiceException, IOException {
		Logger.debug("ServerMindMapCrudService.mindMapAsXmlString => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier);

		final MindmapAsXmlRequest request = new MindmapAsXmlRequest(userIdentifier, mapIdentifier);

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
	public Promise<Boolean> listenForUpdates(UserIdentifier userIdentifier, final MapIdentifier mapIdentifier) {
		final ListenToUpdateOccurrenceRequest request = new ListenToUpdateOccurrenceRequest(userIdentifier, mapIdentifier);

		// two minutes for longpolling
		final long twoMinutesInMillis = 120000;
		return performActionOnMindMap(request, twoMinutesInMillis, new ActionOnMindMap<Boolean>() {

			@Override
			public Promise<Boolean> perform(Promise<Object> promise) throws Exception {
				return promise.map(new Function<Object, Boolean>() {

					@Override
					public Boolean apply(Object listenResponse) throws Throwable {
						final ListenToUpdateOccurrenceResponse response = (ListenToUpdateOccurrenceResponse) listenResponse;
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
	public Promise<String> createNode(UserIdentifier userIdentifier, final MapIdentifier mapIdentifier, final String parentNodeId) {
		return createNode(userIdentifier, mapIdentifier, parentNodeId, null);
	}
	
	@Override
	public Promise<String> createNode(UserIdentifier userIdentifier, final MapIdentifier mapIdentifier, final String parentNodeId, final String side) {
		Logger.debug("mapIdentifier: " + mapIdentifier + "; parentNodeId: " + parentNodeId + "; side: " + side);
		final AddNodeRequest request = new AddNodeRequest(userIdentifier, mapIdentifier, parentNodeId, side);
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
	public Promise<String> getNode(UserIdentifier userIdentifier, final MapIdentifier mapIdentifier, final String nodeId, final Integer nodeCount) {
		Logger.debug("ServerMindMapCrudService.getNode => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier + "; nodeId: " + nodeId + ", nodeCount: " + nodeCount);
		final GetNodeRequest request = new GetNodeRequest(userIdentifier, mapIdentifier, nodeId, nodeCount);

		final Promise<String> promise = performActionOnMindMap(request, new ActionOnMindMap<String>() {
			@Override
			public Promise<String> perform(Promise<Object> promise) throws Exception {
				return Promise.pure(((GetNodeResponse) promise.get()).getNode());
			}
		});
		return promise;
	}

	@Override
	public Promise<String> changeNode(UserIdentifier userIdentifier, MapIdentifier mapIdentifier, String nodeId, Map<String, Object> attributeValueMap) {
		Logger.debug("ServerMindMapCrudService.changeNode => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier + "; nodeId: " + nodeId + "; attributeMap: "
				+ attributeValueMap.toString());

		final ChangeNodeRequest request = new ChangeNodeRequest(userIdentifier, mapIdentifier, nodeId, attributeValueMap);

		return performActionOnMindMap(request, new ActionOnMindMap<String>() {

			@Override
			public Promise<String> perform(Promise<Object> promise) throws Exception {
				final ChangeNodeResponse response = (ChangeNodeResponse) promise.get();
				return Promise.pure(new ObjectMapper().writeValueAsString(response.getMapUpdates()));
			}
		});
	}

	@Override
	public Promise<Boolean> moveNodeTo(UserIdentifier userIdentifier, MapIdentifier mapIdentifier, String newParentNodeId, String nodetoMoveId, Integer newIndex) {
		Logger.debug("ServerMindMapCrudService.moveNodeTo => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier + "; newParentNodeId: " + newParentNodeId + "; nodeId: "
				+ nodetoMoveId + "; newIndex: " + newIndex);

		final MoveNodeToRequest request = new MoveNodeToRequest(userIdentifier, mapIdentifier, newParentNodeId, nodetoMoveId, newIndex);

		return performActionOnMindMap(request, new ActionOnMindMap<Boolean>() {

			@Override
			public Promise<Boolean> perform(Promise<Object> promise) throws Exception {
				final MoveNodeToResponse response = (MoveNodeToResponse) promise.get();
				return Promise.pure(response.getSuccess());
			}
		});
	}

	@Override
	public Promise<Boolean> removeNode(UserIdentifier userIdentifier, MapIdentifier mapIdentifier, String nodeId) {
		Logger.debug("ServerMindMapCrudService.removeNode => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier + "; nodeId: " + nodeId + "; user.getUsername(): "
				+ userIdentifier.getUsername());
		final RemoveNodeRequest request = new RemoveNodeRequest(userIdentifier, mapIdentifier, nodeId);

		return performActionOnMindMap(request, new ActionOnMindMap<Boolean>() {

			@Override
			public Promise<Boolean> perform(Promise<Object> promise) throws Exception {
				final RemoveNodeResponse response = (RemoveNodeResponse) promise.get();
				return Promise.pure(response.getDeleted());
			}
		});
	}

	@Override
	public Promise<String> fetchUpdatesSinceRevision(UserIdentifier userIdentifier, MapIdentifier mapIdentifier, Integer revision) {
		Logger.debug("ServerMindMapCrudService.fetchUpdatesSinceRevision => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier + "; revision: " + revision
				+ "; user.getUsername(): " + userIdentifier.getUsername());
		final FetchMindmapUpdatesRequest request = new FetchMindmapUpdatesRequest(userIdentifier, mapIdentifier, revision);

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
	public Promise<Boolean> requestLock(UserIdentifier userIdentifier, MapIdentifier mapIdentifier, String nodeId) {
		Logger.debug("ServerMindMapCrudService.requestLock => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier + "; nodeId: " + nodeId + "; user.getUsername(): "
				+ userIdentifier.getUsername());
		final RequestLockRequest request = new RequestLockRequest(userIdentifier, mapIdentifier, nodeId);
		Logger.debug("user.getUsername(): " + request.getUsername());
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
	public Promise<Boolean> releaseLock(UserIdentifier userIdentifier, MapIdentifier mapIdentifier, String nodeId) {
		Logger.debug("ServerMindMapCrudService.releaseLock => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier + "; nodeId: " + nodeId + "; user.getUsername(): "
				+ userIdentifier.getUsername());

		final ReleaseLockRequest request = new ReleaseLockRequest(userIdentifier, mapIdentifier, nodeId);

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

	@Override
	public Promise<Boolean> changeEdge(UserIdentifier userIdentifier, MapIdentifier mapIdentifier, String nodeId, Map<String, Object> attributeValueMap) {
		Logger.debug("ServerMindMapCrudService.changeEdge => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier + "; nodeId: " + nodeId + "; attributeMap: "
				+ attributeValueMap.toString());

		final ChangeEdgeRequest request = new ChangeEdgeRequest(userIdentifier, mapIdentifier, nodeId, attributeValueMap);

		return performActionOnMindMap(request, new ActionOnMindMap<Boolean>() {

			@Override
			public Promise<Boolean> perform(Promise<Object> promise) throws Exception {
				final ChangeEdgeResponse response = (ChangeEdgeResponse) promise.get();
				return Promise.pure(response.isSuccess());
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
		final MapIdentifier mapIdentifier = message.getMapIdentifier();

		// Save the user for the current request
		final UserIdentifier user = message.getUserIdentifier();
		// check that user has right to access map
		// throws UnauthorizedException on failure
		hasUserMapAccessRights(user, mapIdentifier);

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
					final MapIdentifier mapIdentifierNotFound = exception.getMapIdentifier();
					sendMindMapToServer(user, mapIdentifierNotFound);
					Logger.debug("ServerMindMapCrudService.performActionOnMindMap => re-sending request to freeplane");
					final Promise<Object> promise = sendMessageToServer(message, timeoutInMillis);
					try {
						result = actionOnMindMap.perform(promise);
					} catch (Exception e2) {
						throw new RuntimeException("ServerMindMapCrudService.performActionOnMindMap => Second attempt failed. ", e2);
					}
				} else if (e instanceof NodeNotLockedByUserException) {
					throw new PreconditionFailedException("No lock on node", e);
				} else if (e instanceof NodeAlreadyLockedException) {
					throw new PreconditionFailedException("Node already locked by another user", e);
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

	private Boolean sendMindMapToServer(final UserIdentifier userIdentifier, MapIdentifier mapIdentifier) throws NoUserLoggedInException {
		Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier);
		InputStream in = null;
		String fileName = null;

		try {
			// test & welcome maps
			if (mapIdentifier.getMapId().length() == 1 || mapIdentifier.getMapId().equals("welcome")) {
				Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map is demo/welcome map, loading from resources");
				in = Play.application().resourceAsStream("mindmaps/" + mapIdentifier.getMapId() + ".mm");
				fileName = mapIdentifier + ".mm";
			}
			// map from user account
			else if (mapIdentifier.getProjectId().equals(MindMap.COMPATIBILITY_DOCEAR_SERVER_PROJECT_ID)) {
				final StringBuilder outfileName = new StringBuilder();
				Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map is real map, loading from docear server");
				final byte[] filebytes = getMindMapInputStreamFromDocearServer(userIdentifier, mapIdentifier.getMapId(), outfileName);

				if (filebytes == null) {
					Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map with serverId: " + mapIdentifier + " was not in zip file.");
					throw new FileNotFoundException("Map not found");
				}

				fileName = outfileName.toString();
				in = new ByteArrayInputStream(filebytes);
			}
			// map from project
			else {
				final String mapId = mapIdentifier.getMapId();

				in = new ZipInputStream(projectService.getFile(mapIdentifier.getProjectId(), mapIdentifier.getMapId()).get());
				((ZipInputStream) in).getNextEntry();
				fileName = mapId.substring(mapId.lastIndexOf("/"));
			}

			// copy map data to a string
			StringWriter writer = new StringWriter();
			IOUtils.copy(in, writer);
			final String fileContentAsString = writer.toString();

			// send file to server and put in open maps set
			openMapIds.add(mapIdentifier);

			final OpenMindMapRequest request = new OpenMindMapRequest(userIdentifier, mapIdentifier, fileContentAsString, fileName);

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

	private byte[] getMindMapInputStreamFromDocearServer(final UserIdentifier userIdentifier, final String mmIdOnServer, final StringBuilder outFileName) throws IOException {

		final String accessToken = userService.getCurrentUser().getAccessToken();
		final String docearServerAPIURL = "https://api.docear.org/user";
		final String resource = docearServerAPIURL + "/" + userIdentifier.getUsername() + "/mindmaps/" + mmIdOnServer;
		Logger.debug("getMindMapFileFromDocearServer => calling URL: '" + resource + "'");
		WS.Response response = WS.url(resource).setHeader("accessToken", accessToken).get().get();

		if (response.getStatus() == 200) {
			return ZipUtils.getMindMapInputStream(response.getBodyAsStream(), outFileName);
		} else if (response.getStatus() == 403) {
			throw new UnauthorizedException("UserIdentifier tried to access not owned map");
		} else {
			throw new RuntimeException("Problem retrieving map from docear server. Status: " + response.getStatus() + " - " + response.getStatusText());
		}
	}

	/**
	 * @throws UnauthorizedException
	 * @param mapIdentifier
	 * @return true or throws {@link UnauthorizedException}
	 */
	private boolean hasUserMapAccessRights(UserIdentifier user, MapIdentifier mapIdentifier) {
		Logger.debug("ServerMindMapCrudService.hasCurrentUserMapAccessRights => userIdentifier: " + user + "; mapIdentifier: " + mapIdentifier);
		// check for demo and welcome map
		if (mapIdentifier.getProjectId().equals(MindMap.COMPATIBILITY_DOCEAR_SERVER_PROJECT_ID) && (mapIdentifier.getMapId().length() == 1 || mapIdentifier.getMapId().equals("welcome")))
			return true;

		// check for docear server map
		if (mapIdentifier.getProjectId().equals(MindMap.COMPATIBILITY_DOCEAR_SERVER_PROJECT_ID)) {
			try {
				List<UserMindmapInfo> infos = userService.getListOfMindMapsFromUser(user()).get();

				Logger.debug("ServerMindMapCrudService.hasCurrentUserMapAccessRights => loaded mapInfos. Count: " + infos.size());
				boolean canAccess = false;
				for (UserMindmapInfo info : infos) {
					if (info.mmIdOnServer.equals(mapIdentifier.getMapId())) {
						canAccess = true;
						break;
					}
				}

				if (!canAccess) {
					Logger.warn("UserIdentifier '" + Controller.session(Secured.SESSION_KEY_USERNAME) + "' tried to access a map he/she does not own!");
					throw new UnauthorizedException("You are not allowed to access that map!");
				}
				return canAccess;

			} catch (IOException e) {
				throw new RuntimeException("Cannot access Docear server!", e);
			}

		}
		//check for project rights
		else {
			return projectService.userBelongsToProject(user.getUsername(), mapIdentifier.getProjectId());
		}
	}

	private User user() {
		return userService.getCurrentUser();
	}

}
