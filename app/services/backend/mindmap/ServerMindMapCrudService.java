package services.backend.mindmap;

import static akka.pattern.Patterns.ask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipInputStream;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.NoUserLoggedInException;
import models.backend.exceptions.sendResult.PreconditionFailedException;
import models.backend.exceptions.sendResult.SendResultException;
import models.backend.exceptions.sendResult.UnauthorizedException;
import models.project.exceptions.InvalidFileNameException;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.docear.messages.Messages.AddNodeRequest;
import org.docear.messages.Messages.AddNodeResponse;
import org.docear.messages.Messages.ChangeEdgeRequest;
import org.docear.messages.Messages.ChangeEdgeResponse;
import org.docear.messages.Messages.ChangeNodeRequest;
import org.docear.messages.Messages.ChangeNodeResponse;
import org.docear.messages.Messages.CreateMindmapRequest;
import org.docear.messages.Messages.CreateMindmapResponse;
import org.docear.messages.Messages.FetchMindmapUpdatesRequest;
import org.docear.messages.Messages.FetchMindmapUpdatesResponse;
import org.docear.messages.Messages.GetNodeRequest;
import org.docear.messages.Messages.GetNodeResponse;
import org.docear.messages.Messages.ListenToUpdateOccurrenceRequest;
import org.docear.messages.Messages.ListenToUpdateOccurrenceResponse;
import org.docear.messages.Messages.MindMapChangeResponse;
import org.docear.messages.Messages.MindMapRequest;
import org.docear.messages.Messages.MindmapAsJsonReponse;
import org.docear.messages.Messages.MindmapAsJsonRequest;
import org.docear.messages.Messages.MindmapAsXmlRequest;
import org.docear.messages.Messages.MindmapAsXmlResponse;
import org.docear.messages.Messages.MoveNodeToRequest;
import org.docear.messages.Messages.MoveNodeToResponse;
import org.docear.messages.Messages.OpenMindMapRequest;
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
import play.libs.F;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;
import play.mvc.Controller;
import scala.concurrent.duration.Duration;
import services.backend.project.ProjectService;
import services.backend.project.persistance.EntityCursor;
import services.backend.user.UserService;
import util.backend.ZipUtils;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

import com.typesafe.config.ConfigFactory;

import controllers.MindMap;
import controllers.secured.SecuredBase;

@Profile("backendProd")
@Component
/**
 *
 * production implementation of MindmapCrudService interface.
 * Handles communication with Freeplane
 *
 */
public class ServerMindMapCrudService implements MindMapCrudService {

	private final ActorSystem system;
	private final ActorRef remoteActor;
	private ActorRef processClosedMindMapActor;
	@SuppressWarnings("unused")
	private final Cancellable saveMindmapsJob;
	private final UserIdentifier serverUserIdentifier = new UserIdentifier("SERVER", "SERVER");
	private final long defaultTimeoutInMillis = Play.application().configuration().getLong("services.backend.mindmap.MindMapCrudService.timeoutInMillis");
	@Autowired
	private UserService userService;
	@Autowired
	private ProjectService projectService;
	@Autowired
	private MetaDataCrudService metaDataCrudService;

	public ServerMindMapCrudService() {
		final String freeplaneActorUrl = Play.application().configuration().getStringList("services.backend.mindmap.MindMapCrudService.instances").get(0);
		system = ActorSystem.create("freeplaneSystem", ConfigFactory.load().getConfig("local"));
		remoteActor = system.actorFor(freeplaneActorUrl);

		// job to auto save maps that haven't been saved for 5 minutes but are
		// open
		saveMindmapsJob = system.scheduler().schedule(Duration.Zero(), Duration.apply(1, TimeUnit.MINUTES), new Runnable() {
			@Override
			public void run() {
				try {
					// 1000 * 1 ms = 1s * 60 = 1m * 5 = 5m = 1000 * 60 * 5 =
					// 300000 ms
					final long notSavedSinceTimeInMillis = System.currentTimeMillis() - 300000;
					final EntityCursor<MetaData> byNotSavedSince = metaDataCrudService.findByNotSavedSince(notSavedSinceTimeInMillis);
					try {
						Iterator<MetaData> metaIt = byNotSavedSince.iterator();
						while (metaIt.hasNext()) {
							final MetaData metaData = metaIt.next();
							final String projectId = metaData.getProjectId();
							final String path = metaData.getMindMapResource();
							Logger.debug("Mindmap '" + path + "' in project '" + projectId + "' will be saved.");
							// check if mindmap is open
							try {
								performActionOnMindMap(new MindmapAsJsonRequest(serverUserIdentifier, new MapIdentifier(projectId, path), 1), defaultTimeoutInMillis, false,
										new ActionOnMindMap<Object>() {

											@Override
											public Promise<Object> perform(Promise<Object> promise) throws Exception {
												return promise;
											}
										}).get();
							} catch (MapNotFoundException e) {
								Logger.error("Map not found on server, deleting meta data");
								metaDataCrudService.delete(projectId, path);
							} catch (Exception e) {
								if (e instanceof TimeoutException) {
									Logger.error("Server is not reachable, deleting meta data");
									metaDataCrudService.delete(projectId, path);
								} else {
									throw new RuntimeException(e);
								}
							}

							try {
								saveMindMapInProjectService(new MapIdentifier(metaData.getProjectId(), metaData.getMindMapResource()));
							} catch (InvalidFileNameException e) {
								Logger.error("Problem saving file!", e);
							}
						}
					} finally {
						byNotSavedSince.close();
					}
				} catch (IOException e) {
					Logger.error("Problem with metadataservice. Retrying in 5 Minutes! ", e);
				}
			}
		}, system.dispatcher());
	}

	@SuppressWarnings("serial")
	private ActorRef getProcessClosedMindMapsActor() {
		if (processClosedMindMapActor == null) {
			processClosedMindMapActor = system.actorOf(new Props(new UntypedActorFactory() {
				@Override
				public Actor create() throws Exception {
					return new ProcessClosedMindMapsActor(metaDataCrudService, projectService);
				}
			}));
		}
		return processClosedMindMapActor;
	}

	@Override
	public Promise<Boolean> createMindmap(UserIdentifier userIdentifier, final MapIdentifier mapIdentifier) {
		Logger.debug("ServerMindMapCrudService.createMindmap => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier);

		final CreateMindmapRequest request = new CreateMindmapRequest(userIdentifier, mapIdentifier, getProcessClosedMindMapsActor());

		return performActionOnMindMap(request, new ActionOnMindMap<Boolean>() {
			@Override
			public Promise<Boolean> perform(Promise<Object> promise) throws Exception {
				final CreateMindmapResponse response = (CreateMindmapResponse) promise.get();
				metaDataCrudService.upsert(new MetaData(mapIdentifier.getProjectId(), mapIdentifier.getMapId(), 0L, System.currentTimeMillis()));
				Logger.debug("ServerMindMapCrudService.mindMapAsJsonString => response received");
				return Promise.pure(response.isSuccess());
			}
		});

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
	public Promise<MindmapAsXmlResponse> mindMapAsXmlString(UserIdentifier userIdentifier, MapIdentifier mapIdentifier) throws DocearServiceException, IOException {
		Logger.debug("ServerMindMapCrudService.mindMapAsXmlString => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier);

		final MindmapAsXmlRequest request = new MindmapAsXmlRequest(userIdentifier, mapIdentifier);

		return performActionOnMindMap(request, new ActionOnMindMap<MindmapAsXmlResponse>() {
			@Override
			public Promise<MindmapAsXmlResponse> perform(Promise<Object> promise) throws Exception {
				final MindmapAsXmlResponse response = (MindmapAsXmlResponse) promise.get();
				return Promise.pure(response);
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
		Logger.debug("ServerMindMapCrudService.createNode => mapIdentifier: " + mapIdentifier + "; parentNodeId: " + parentNodeId + "; side: " + side);
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
				return Promise.pure(response.isSuccess());
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
				return Promise.pure(response.isSuccess());
			}
		});
	}

	@Override
	public Promise<JsonNode> fetchUpdatesSinceRevision(UserIdentifier userIdentifier, MapIdentifier mapIdentifier, Integer revision) {
		Logger.debug("ServerMindMapCrudService.fetchUpdatesSinceRevision => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier + "; revision: " + revision
				+ "; user.getUsername(): " + userIdentifier.getUsername());
		final FetchMindmapUpdatesRequest request = new FetchMindmapUpdatesRequest(userIdentifier, mapIdentifier, revision);

		return performActionOnMindMap(request, new ActionOnMindMap<JsonNode>() {
			@Override
			public Promise<JsonNode> perform(Promise<Object> promise) throws Exception {
				final FetchMindmapUpdatesResponse response = (FetchMindmapUpdatesResponse) promise.get();
				final ObjectMapper objectMapper = new ObjectMapper();
				final ObjectNode jsonNode = objectMapper.createObjectNode();
				final ArrayNode updates = objectMapper.createArrayNode();
				for (String json : response.getOrderedUpdates()) {
					updates.add(objectMapper.readTree(json));
				}

				jsonNode.put("currentRevision", response.getCurrentRevision());
				jsonNode.put("orderedUpdates", updates);

				return Promise.pure((JsonNode) jsonNode);
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
				return Promise.pure(response.isSuccess());
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
				return Promise.pure(response.isSuccess());
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
	 * 
	 * @throws Exception
	 */
	private <A> Promise<A> performActionOnMindMap(MindMapRequest message, ActionOnMindMap<A> actionOnMindMap) {
		return performActionOnMindMap(message, defaultTimeoutInMillis, actionOnMindMap);
	}

	private <A> Promise<A> performActionOnMindMap(final MindMapRequest message, final long timeoutInMillis, final ActionOnMindMap<A> actionOnMindMap) {
		return performActionOnMindMap(message, timeoutInMillis, true, actionOnMindMap);
	}

	/**
	 * Central point to perform an action on a mindmap. Helps to centralise
	 * error handling and reduce code duplication
	 */
	private <A> Promise<A> performActionOnMindMap(final MindMapRequest message, final long timeoutInMillis, final boolean secondAttempt, final ActionOnMindMap<A> actionOnMindMap) {
		Logger.debug("ServerMindMapCrudService.performActionOnMindMap => message type: " + message.getClass().getSimpleName());
		final MapIdentifier mapIdentifier = message.getMapIdentifier();

		// Save the user for the current request
		final UserIdentifier user = message.getUserIdentifier();
		// check that user has right to access map
		// throws UnauthorizedException on failure
		if (!hasUserMapAccessRights(user, mapIdentifier)) {
			throw new UnauthorizedException("User has no right on Map!");
		}

		Promise<A> result = null;
		try {

			Logger.debug("ServerMindMapCrudService.performActionOnMindMap => sending request to freeplane");
			final Promise<Object> promise = sendMessageToServer(message, timeoutInMillis);

			// link into promise for saving mechanism, save every 25 revisions
			promise.onRedeem(new SavingCallback(mapIdentifier, metaDataCrudService, this));
			result = actionOnMindMap.perform(promise);
		} catch (Exception e) {
			Logger.debug("ServerMindMapCrudService.performActionOnMindMap => Exception catched! Type: " + e.getClass().getSimpleName());
			// check if exception is handled by action
			result = actionOnMindMap.handleException(e);
			Logger.debug("ServerMindMapCrudService.performActionOnMindMap => Exception handled by action: " + (result != null));

			if (!secondAttempt) {
				if (e instanceof MapNotFoundException) {
					throw (MapNotFoundException) e;
				} else {
					throw new RuntimeException(e);
				}
			}
			// default handling when second attempt is allowed
			if (result == null) { // exception was not handled by action
				if (e instanceof MapNotFoundException) {
					final MapNotFoundException exception = (MapNotFoundException) e;
					// Map was closed on server, reopen and perform action again
					Logger.info("ServerMindMapCrudService.performActionOnMindMap => mind map was not present in freeplane. Reopening...");
					final MapIdentifier mapIdentifierNotFound = exception.getMapIdentifier();
					// remove from meta db
					try {
						metaDataCrudService.delete(mapIdentifier.getProjectId(), mapIdentifier.getMapId());
					} catch (IOException e1) {
						Logger.error("unexpected exception! ", e1);
					}

					try {
						sendMindMapToServer(user, mapIdentifierNotFound);
					} catch (InvalidFileNameException e1) {
						throw new SendResultException(e1.getMessage(), 400);
					}
					Logger.debug("ServerMindMapCrudService.performActionOnMindMap => re-sending request to freeplane");
					final Promise<Object> promise = sendMessageToServer(message, timeoutInMillis);
					promise.onRedeem(new SavingCallback(mapIdentifierNotFound, metaDataCrudService, this));
					try {
						result = actionOnMindMap.perform(promise);
					} catch (Exception e2) {
						throw new RuntimeException("ServerMindMapCrudService.performActionOnMindMap => Second attempt failed. ", e2);
					}
				} else if (e instanceof NodeNotLockedByUserException) {
					throw new PreconditionFailedException("No lock on node", e);
				} else if (e instanceof NodeAlreadyLockedException) {
					throw new PreconditionFailedException("Node already locked by another user", e);
				} else if (e instanceof NodeNotFoundException) {
					throw new SendResultException("Node does not exist", 400);
				} else {
					throw new RuntimeException(e);
				}
			}
		}

		return result;
	}

	private static class SavingCallback implements F.Callback<Object> {
		private final MapIdentifier mapIdentifier;
		private final MetaDataCrudService metaDataCrudService;
		private final ServerMindMapCrudService mindMapCrudService;

		public SavingCallback(MapIdentifier mapIdentifier, MetaDataCrudService metaDataCrudService, ServerMindMapCrudService mindMapCrudService) {
			super();
			this.mapIdentifier = mapIdentifier;
			this.metaDataCrudService = metaDataCrudService;
			this.mindMapCrudService = mindMapCrudService;
		}

		@Override
		public void invoke(Object o) throws Throwable {

			if (o instanceof MindMapChangeResponse) {
				final MindMapChangeResponse response = (MindMapChangeResponse) o;
				try {
					final MetaData metaData = metaDataCrudService.find(mapIdentifier.getProjectId(), mapIdentifier.getMapId());
					Logger.debug(metaData.getCurrentRevision() + "; " + response.getCurrentRevision());
					Logger.debug("" + (metaData.getCurrentRevision() % 3));
					Logger.debug("" + (response.getCurrentRevision() % 3));
					if (metaData.getCurrentRevision() % 25 >= response.getCurrentRevision() % 25) {
						mindMapCrudService.saveMindMapInProjectService(mapIdentifier);
					}
				} catch (Throwable t) {
					Logger.error("Problem with metaDataService", t);
					throw t;
				}

			}
		}
	}

	private Promise<Object> sendMessageToServer(Object message, long timeoutInMillis) {
		return Akka.asPromise(ask(remoteActor, message, timeoutInMillis));
	}

	private Boolean sendMindMapToServer(final UserIdentifier userIdentifier, final MapIdentifier mapIdentifier) throws NoUserLoggedInException, InvalidFileNameException {
		Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => userIdentifier: " + userIdentifier + "; mapIdentifier: " + mapIdentifier);
		InputStream in = null;
		ByteArrayOutputStream out = null;
		byte[] bytes = null;
		try {
			// test & welcome maps
			if (mapIdentifier.getMapId().length() == 1 || mapIdentifier.getMapId().equals("welcome")) {
				Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => map is demo/welcome map, loading from resources");
				in = Play.application().resourceAsStream("mindmaps/" + mapIdentifier.getMapId() + ".mm");
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

				in = new ByteArrayInputStream(filebytes);
			}
			// map from project
			else {
				Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => Is map in a project");
				final String mapId = mapIdentifier.getMapId();

				in = new ZipInputStream(projectService.getFile(mapIdentifier.getProjectId(), mapId, true));
				((ZipInputStream) in).getNextEntry();
			}

			// copy map data to a byte array
			out = new ByteArrayOutputStream();
			IOUtils.copy(in, out);
			bytes = out.toByteArray();

		} catch (IOException e) {
			Logger.error("ServerMindMapCrudService.sendMapToDocearInstance => can't open mindmap file", e);
			throw new RuntimeException("ServerMindMapCrudService.sendMapToDocearInstance => can't find mindmap file", e);
		} finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(out);
		}

		try {
			Logger.debug("ServerMindMapCrudService.sendMapToDocearInstance => sending map to docear");
			// send file to server and put in database
			metaDataCrudService.upsert(new MetaData(mapIdentifier.getProjectId(), mapIdentifier.getMapId(), 0L, System.currentTimeMillis()));

			final OpenMindMapRequest request = new OpenMindMapRequest(userIdentifier, mapIdentifier, bytes, getProcessClosedMindMapsActor());
			remoteActor.tell(request, null);
			return true;
			// sending map to freeplane
			// return performActionOnMindMap(request, new
			// ActionOnMindMap<Boolean>() {
			// @Override
			// public Promise<Boolean> perform(Promise<Object> promise) throws
			// Exception {
			//
			// final OpenMindMapResponse response = (OpenMindMapResponse)
			// promise.get();
			// final boolean success = response.isSuccess();
			// if (success) {
			// // put in meta database
			// final MetaData metaData = new
			// MetaData(mapIdentifier.getProjectId(), mapIdentifier.getMapId(),
			// 0L, System.currentTimeMillis());
			// metaDataCrudService.upsert(metaData);
			// }
			//
			// return Promise.pure(success);
			// }
			//
			// }).get();
		} catch (IOException e) {
			throw new RuntimeException("Cannot upsert meta data to database! ", e);
		}
	}

	@Override
	public Boolean isMindMapOpened(MapIdentifier mapIdentifier) {
		try {
			return metaDataCrudService.find(mapIdentifier.getProjectId(), mapIdentifier.getMapId()) != null;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public void saveMindMapInProjectService(final MapIdentifier mapIdentifier) throws IOException, InvalidFileNameException {
		final String projectId = mapIdentifier.getProjectId();
		final String path = mapIdentifier.getMapId();

		// compatibility! Don't try to save projectId -1
		if (mapIdentifier.getProjectId().equals(MindMap.COMPATIBILITY_DOCEAR_SERVER_PROJECT_ID))
			return;

		Logger.debug("ServerMindMapCrudService.saveMindMapInProjectService => saving map in project: " + mapIdentifier);
		// get map xml
		final MindmapAsXmlResponse mindmapAsXmlResponse = performActionOnMindMap(new MindmapAsXmlRequest(serverUserIdentifier, mapIdentifier), new ActionOnMindMap<MindmapAsXmlResponse>() {
			@Override
			public Promise<MindmapAsXmlResponse> perform(Promise<Object> promise) throws Exception {
				return Promise.pure((MindmapAsXmlResponse) promise.get());
			}
		}).get();

		Logger.debug(mindmapAsXmlResponse.getFileBytes().toString());

		// convert to bytes for put
		final byte[] fileBytes = mindmapAsXmlResponse.getFileBytes();
		final long currentRevision = mindmapAsXmlResponse.getCurrentRevision();

		// upload to server
		projectService.putFile(mapIdentifier.getProjectId(), mapIdentifier.getMapId(), fileBytes, false, 0L, true);

		// update metadata
		final MetaData newMetaData = new MetaData(projectId, path, currentRevision, System.currentTimeMillis());
		metaDataCrudService.upsert(newMetaData);
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
	 * @param mapIdentifier
	 * @return true or throws {@link UnauthorizedException}
	 * @throws UnauthorizedException
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
					Logger.warn("UserIdentifier '" + Controller.session(SecuredBase.SESSION_KEY_USERNAME) + "' tried to access a map he/she does not own!");
					throw new UnauthorizedException("You are not allowed to access that map!");
				}
				return canAccess;

			} catch (IOException e) {
				throw new RuntimeException("Cannot access Docear server!", e);
			}

		}
		// check for project rights
		else {
			//check if server ordered this action
			if(user.getSource().equals("SERVER") && user.getUsername().equals("SERVER"))
				return true;
			return projectService.userBelongsToProject(user.getUsername(), mapIdentifier.getProjectId());
		}
	}

	private User user() {
		return userService.getCurrentUser();
	}

}
