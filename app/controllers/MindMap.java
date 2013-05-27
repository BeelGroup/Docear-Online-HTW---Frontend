package controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.sendResult.UnauthorizedException;
import models.frontend.formdata.ChangeEdgeData;
import models.frontend.formdata.ChangeNodeData;
import models.frontend.formdata.CreateNodeData;
import models.frontend.formdata.MoveNodeData;
import models.frontend.formdata.ReleaseLockData;
import models.frontend.formdata.RemoveNodeData;
import models.frontend.formdata.RequestLockData;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.docear.messages.models.MapIdentifier;
import org.docear.messages.models.UserIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import play.Logger;
import play.data.Form;
import play.libs.F;
import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import services.backend.mindmap.MindMapCrudService;
import services.backend.project.ProjectService;
import services.backend.user.UserService;

@Component
public class MindMap extends Controller {
	private final static Form<CreateNodeData> createNodeForm = Form.form(CreateNodeData.class);
	private final static Form<RemoveNodeData> removeNodeForm = Form.form(RemoveNodeData.class);
	private final static Form<ChangeNodeData> changeNodeForm = Form.form(ChangeNodeData.class);
	private final static Form<MoveNodeData> moveNodeForm = Form.form(MoveNodeData.class);
	private final static Form<RequestLockData> requestLockForm = Form.form(RequestLockData.class);
	private final static Form<ReleaseLockData> releaseLockForm = Form.form(ReleaseLockData.class);
	private final static Form<ChangeEdgeData> changeEdgeForm = Form.form(ChangeEdgeData.class);

	public final static String COMPATIBILITY_DOCEAR_SERVER_PROJECT_ID = "-1";

	@Autowired
	private MindMapCrudService mindMapCrudService;

	@Autowired
	private UserService userService;

	@Autowired
	private ProjectService projectService;

	// cannot be secured, because we load the welcome map
	public Result mapAsJson(final String projectId, final String mapId, final Integer nodeCount) throws DocearServiceException, IOException {
		Logger.debug("MindMap.map <- projectId= " + projectId + "; mapId=" + mapId + "; nodeCount: " + nodeCount);

		// for welcome map allways take cached json
		if (projectId.equals(COMPATIBILITY_DOCEAR_SERVER_PROJECT_ID) && mapId.equals("welcome")) {
			return ok(util.Input.resourceToString("rest/v1/map/welcome.json"));
		}

		// short version, throws NotLoggedInException
		final UserIdentifier userIdentifier = userIdentifier();

		final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
		final F.Promise<String> mindMapPromise = mindMapCrudService.mindMapAsJsonString(userIdentifier, mapIdentifier, nodeCount);
		return async(mindMapPromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String mindMap) throws Throwable {
				return ok(mindMap);
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result mapAsXml(final String projectId, final String mapId) throws DocearServiceException, IOException {
		Logger.debug("MindMap.map <- projectId= " + projectId + "; mapId=" + mapId);

		final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
		final F.Promise<String> mindMapPromise = mindMapCrudService.mindMapAsXmlString(userIdentifier(), mapIdentifier);

		return async(mindMapPromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String mindMap) throws Throwable {
				return ok(mindMap);
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result requestLock(final String projectId, final String mapId) {
		final Form<RequestLockData> filledForm = requestLockForm.bindFromRequest();
		Logger.debug("MindMap.requestLock => projectId= " + projectId + "; mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final RequestLockData data = filledForm.get();
			final String nodeId = data.getNodeId();

			final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
			final F.Promise<Boolean> promise = mindMapCrudService.requestLock(userIdentifier(), mapIdentifier, nodeId);
			return async(promise.map(new Function<Boolean, Result>() {

				@Override
				public Result apply(Boolean success) throws Throwable {
					if (success) {
						return ok();
					} else {
						return status(PRECONDITION_FAILED);
					}
				}
			}));
		}
	}

	@Security.Authenticated(Secured.class)
	public Result releaseLock(final String projectId, final String mapId) {
		final Form<ReleaseLockData> filledForm = releaseLockForm.bindFromRequest();
		Logger.debug("MindMap.requestLock => projectId= " + projectId + "; mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final ReleaseLockData data = filledForm.get();
			final String nodeId = data.getNodeId();

			final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
			final F.Promise<Boolean> promise = mindMapCrudService.releaseLock(userIdentifier(), mapIdentifier, nodeId);
			return async(promise.map(new Function<Boolean, Result>() {

				@Override
				public Result apply(Boolean success) throws Throwable {
					if (success) {
						return ok();
					} else {
						return status(PRECONDITION_FAILED);
					}
				}
			}));
		}
	}

	@Security.Authenticated(Secured.class)
	public Result fetchUpdatesSinceRevision(final String projectId, String mapId, Integer revision) {
		Logger.debug("MindMap.fetchUpdatesSinceRevision <- projectId= " + projectId + "; mapId=" + mapId + "; revision: " + revision);

		final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
		final F.Promise<String> updatePromise = mindMapCrudService.fetchUpdatesSinceRevision(userIdentifier(), mapIdentifier, revision);
		return async(updatePromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String updates) throws Throwable {
				return ok(updates.replaceAll("\"\\{", "\\{").replaceAll("\\}\"", "\\}").replace("\\\"", "\""));
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result createNode(final String projectId, final String mapId) {
		final Form<CreateNodeData> filledForm = createNodeForm.bindFromRequest();
		Logger.debug("MindMap.createNode <- projectId= " + projectId + "; mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final String parentNodeId = filledForm.get().getParentNodeId();

			final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
			final F.Promise<String> addNodePromise = mindMapCrudService.createNode(userIdentifier(), mapIdentifier, parentNodeId);
			return async(addNodePromise.map(new F.Function<String, Result>() {
				@Override
				public Result apply(String node) throws Throwable {
					return ok(node);
				}
			}));
		}
	}

	public Result getNode(final String projectId, final String mapId, final String nodeId, final Integer nodeCount) {
		Logger.debug("MindMap.getNode <- projectId= " + projectId + "; mapId=" + mapId + ", nodeId=" + nodeId + ", nodeCount= " + nodeCount);
		if (!mapId.equals("welcome") && !userService.isAuthenticated())
			throw new UnauthorizedException("No user logged in");

		final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
		final F.Promise<String> addNodePromise = mindMapCrudService.getNode(userIdentifier(), mapIdentifier, nodeId, nodeCount);
		return async(addNodePromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String node) throws Throwable {
				return ok(node);
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result changeNode(final String projectId, final String mapId) {
		final Form<ChangeNodeData> filledForm = changeNodeForm.bindFromRequest();

		Logger.debug("MindMap.changeNode => projectId= " + projectId + "; mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final ChangeNodeData data = filledForm.get();
			final String nodeId = data.getNodeId();
			final Map<String, Object> attributeValueMap = new HashMap<String, Object>();

			final Map<String, String[]> formUrlEncoded = request().body().asFormUrlEncoded();
			Logger.debug((formUrlEncoded != null) + "");
			for (Map.Entry<String, String[]> entry : formUrlEncoded.entrySet()) {
				if (entry.getKey().equals("nodeId"))
					continue;

				final String value = entry.getValue()[0];

				attributeValueMap.put(entry.getKey(), value.isEmpty() ? null : value);
			}

			final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
			final F.Promise<String> promise = mindMapCrudService.changeNode(userIdentifier(), mapIdentifier, nodeId, attributeValueMap);
			return async(promise.map(new Function<String, Result>() {
				@Override
				public Result apply(String json) throws Throwable {
					return ok(json);
				}
			}));
		}
	}

	@Security.Authenticated(Secured.class)
	public Result moveNode(final String projectId, final String mapId) throws JsonParseException, JsonMappingException, IOException {
		final Form<MoveNodeData> filledForm = moveNodeForm.bindFromRequest();
		Logger.debug("MindMap.moveNode => projectId= " + projectId + "; mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final MoveNodeData data = filledForm.get();
			final String newParentNodeId = data.getNewParentNodeId();
			final String nodeToMoveId = data.getNodetoMoveId();
			final Integer newIndex = data.getNewIndex();

			final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
			final F.Promise<Boolean> promise = mindMapCrudService.moveNodeTo(userIdentifier(), mapIdentifier, newParentNodeId, nodeToMoveId, newIndex);
			return async(promise.map(new Function<Boolean, Result>() {
				@Override
				public Result apply(Boolean success) throws Throwable {
					if (success)
						return ok();
					else
						return status(PRECONDITION_FAILED);

				}
			}));
		}
	}

	@Security.Authenticated(Secured.class)
	public Result deleteNode(final String projectId, final String mapId) {
		final Form<RemoveNodeData> filledForm = removeNodeForm.bindFromRequest();
		Logger.debug("MindMap.deleteNode => projectId= " + projectId + "; mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final RemoveNodeData data = filledForm.get();
			final String nodeId = data.getNodeId();

			final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
			final F.Promise<Boolean> promise = mindMapCrudService.removeNode(userIdentifier(), mapIdentifier, nodeId);
			return async(promise.map(new Function<Boolean, Result>() {
				@Override
				public Result apply(Boolean success) throws Throwable {
					if (success) {
						return ok();
					} else {
						return status(PRECONDITION_FAILED);
					}
				}
			}));
		}
	}

	@Security.Authenticated(Secured.class)
	public Result changeEdge(final String projectId, final String mapId) throws JsonParseException, JsonMappingException, IOException {
		final Form<ChangeEdgeData> filledForm = changeEdgeForm.bindFromRequest();
		Logger.debug("MindMap.changeEdge => projectId= " + projectId + "; mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final ChangeEdgeData data = filledForm.get();
			final String nodeId = data.getNodeId();
			final Map<String, Object> attributeValueMap = new HashMap<String, Object>();

			final Map<String, String[]> formUrlEncoded = request().body().asFormUrlEncoded();
			Logger.debug((formUrlEncoded != null) + "");
			for (Map.Entry<String, String[]> entry : formUrlEncoded.entrySet()) {
				if (entry.getKey().equals("nodeId"))
					continue;

				final String value = entry.getValue()[0];
				Logger.debug("value: " + value + "; empty :" + value.isEmpty());
				attributeValueMap.put(entry.getKey(), value.isEmpty() ? null : value);
			}
			Logger.debug(attributeValueMap.toString());

			final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
			final F.Promise<Boolean> promise = mindMapCrudService.changeEdge(userIdentifier(), mapIdentifier, nodeId, attributeValueMap);
			return async(promise.map(new Function<Boolean, Result>() {
				@Override
				public Result apply(Boolean success) throws Throwable {
					if (success) {
						return ok();
					} else {
						return status(PRECONDITION_FAILED);
					}
				}
			}));
		}
	}

	public Result listenForUpdates(final String projectId, final String mapId) {
		Logger.debug("MindMap.listenForUpdates => projectId= " + projectId + "; mapId=" + mapId);
		final MapIdentifier mapIdentifier = new MapIdentifier(projectId, mapId);
		return async(mindMapCrudService.listenForUpdates(userIdentifier(), mapIdentifier).map(new Function<Boolean, Result>() {

			@Override
			public Result apply(Boolean hasChanged) throws Throwable {
				if (hasChanged)
					return ok();
				else
					return status(NOT_MODIFIED);
			}
		}));
	}

	private UserIdentifier userIdentifier() {
		final models.backend.User user = userService.getCurrentUser(source());
		final UserIdentifier userIdentifier = new UserIdentifier(user.getSource(), user.getUsername());
		return userIdentifier;
	}

	private String source() {
		final String source = request().getQueryString("source");
		if (source != null)
			return source;
		else
			return "unknown";
	}
}