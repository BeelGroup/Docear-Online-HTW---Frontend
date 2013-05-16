package controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.backend.exceptions.DocearServiceException;
import models.frontend.formdata.ChangeEdgeData;
import models.frontend.formdata.ChangeNodeData;
import models.frontend.formdata.CreateNodeData;
import models.frontend.formdata.MoveNodeData;
import models.frontend.formdata.ReleaseLockData;
import models.frontend.formdata.RemoveNodeData;
import models.frontend.formdata.RequestLockData;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
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
import services.backend.user.UserService;
import views.html.helper.form;

@Component
public class MindMap extends Controller {
	private final static Form<CreateNodeData> createNodeForm = Form.form(CreateNodeData.class);
	private final static Form<RemoveNodeData> removeNodeForm = Form.form(RemoveNodeData.class);
	private final static Form<ChangeNodeData> changeNodeForm = Form.form(ChangeNodeData.class);
	private final static Form<MoveNodeData> moveNodeForm = Form.form(MoveNodeData.class);
	private final static Form<RequestLockData> requestLockForm = Form.form(RequestLockData.class);
	private final static Form<ReleaseLockData> releaseLockForm = Form.form(ReleaseLockData.class);
	private final static Form<ChangeEdgeData> changeEdgeForm = Form.form(ChangeEdgeData.class);

	@Autowired
	private MindMapCrudService mindMapCrudService;
	@Autowired
	private UserService userService;

	// cannot be secured, because we load the welcome map
	public Result mapAsJson(final String mapId, final Integer nodeCount) throws DocearServiceException, IOException {
		Logger.debug("MindMap.map <- mapId=" + mapId + "; nodeCount: " + nodeCount);
		// check if welcome map or user authenticated
		if (!mapId.equals("welcome") && !userService.isAuthenticated())
			return redirect(routes.Application.index());

		final F.Promise<String> mindMapPromise = mindMapCrudService.mindMapAsJsonString(source(), username(), mapId, nodeCount);
		return async(mindMapPromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String mindMap) throws Throwable {
				return ok(mindMap);
			}
		}));
	}

	public Result mapAsXml(final String mapId) throws DocearServiceException, IOException {
		Logger.debug("MindMap.map <- mapId=" + mapId);
		// check if welcome map or user authenticated
		if (!mapId.equals("welcome") && !userService.isAuthenticated())
			return redirect(routes.Application.index());

		final F.Promise<String> mindMapPromise = mindMapCrudService.mindMapAsXmlString(source(), username(), mapId);

		return async(mindMapPromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String mindMap) throws Throwable {
				return ok(mindMap);
			}
		}));
	}

	public Result requestLock(final String mapId) {
		final Form<RequestLockData> filledForm = requestLockForm.bindFromRequest();
		Logger.debug("MindMap.requestLock => mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final RequestLockData data = filledForm.get();
			final String nodeId = data.getNodeId();
			final F.Promise<Boolean> promise = mindMapCrudService.requestLock(source(), username(), mapId, nodeId);
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

	public Result releaseLock(final String mapId) {
		final Form<ReleaseLockData> filledForm = releaseLockForm.bindFromRequest();
		Logger.debug("MindMap.requestLock => mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final ReleaseLockData data = filledForm.get();
			final String nodeId = data.getNodeId();
			final F.Promise<Boolean> promise = mindMapCrudService.releaseLock(source(), username(), mapId, nodeId);
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
	public Result fetchUpdatesSinceRevision(String mapId, Integer revision) {
		Logger.debug("MindMap.fetchUpdatesSinceRevision <- mapId=" + mapId + "; revision: " + revision);

		final F.Promise<String> updatePromise = mindMapCrudService.fetchUpdatesSinceRevision(source(), username(), mapId, revision);
		return async(updatePromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String updates) throws Throwable {
				return ok(updates.replaceAll("\"\\{", "\\{").replaceAll("\\}\"", "\\}").replace("\\\"", "\""));
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result createNode(final String mapId) {
		final Form<CreateNodeData> filledForm = createNodeForm.bindFromRequest();
		Logger.debug("MindMap.createNode <- mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors()) {
			return badRequest(filledForm.errorsAsJson());
		} else {
			final String parentNodeId = filledForm.get().getParentNodeId();
			final F.Promise<String> addNodePromise = mindMapCrudService.createNode(source(), username(), mapId, parentNodeId);
			return async(addNodePromise.map(new F.Function<String, Result>() {
				@Override
				public Result apply(String node) throws Throwable {
					return ok(node);
				}
			}));
		}
	}

	public Result getNode(final String mapId, final String nodeId, final Integer nodeCount) {
		Logger.debug("MindMap.getNode <- mapId=" + mapId + ", nodeId=" + nodeId + ", nodeCount= " + nodeCount);
		if (!mapId.equals("welcome") && !userService.isAuthenticated())
			return unauthorized();
		final F.Promise<String> addNodePromise = mindMapCrudService.getNode(source(), username(), mapId, nodeId, nodeCount);
		return async(addNodePromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String node) throws Throwable {
				return ok(node);
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result changeNode(final String mapId) {
		final Form<ChangeNodeData> filledForm = changeNodeForm.bindFromRequest();
		
		Logger.debug("MindMap.changeNode => mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final ChangeNodeData data = filledForm.get();
			final String nodeId = data.getNodeId();
			final Map<String, Object> attributeValueMap = new HashMap<String, Object>();
			
			final Map<String, String[]> formUrlEncoded = request().body().asFormUrlEncoded();
			Logger.debug((formUrlEncoded != null) + "");
			for(Map.Entry<String, String[]> entry : formUrlEncoded.entrySet()) {
				if(entry.getKey().equals("nodeId"))
					continue;
				
				final String value = entry.getValue()[0];
				
				attributeValueMap.put(entry.getKey(), value.isEmpty() ? null : value);
			}

			final F.Promise<String> promise = mindMapCrudService.changeNode(source(), username(), mapId, nodeId, attributeValueMap);
			return async(promise.map(new Function<String, Result>() {
				@Override
				public Result apply(String json) throws Throwable {
					return ok(json);
				}
			}));
		}
	}

	@Security.Authenticated(Secured.class)
	public Result moveNode(final String mapId) throws JsonParseException, JsonMappingException, IOException {
		final Form<MoveNodeData> filledForm = moveNodeForm.bindFromRequest();
		Logger.debug("MindMap.moveNode => mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final MoveNodeData data = filledForm.get();
			final String newParentNodeId = data.getNewParentNodeId();
			final String nodeToMoveId = data.getNodetoMoveId();
			final Integer newIndex = data.getNewIndex();

			final F.Promise<Boolean> promise = mindMapCrudService.moveNodeTo(source(), username(), mapId, newParentNodeId, nodeToMoveId, newIndex);
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
	public Result deleteNode(final String mapId) {
		final Form<RemoveNodeData> filledForm = removeNodeForm.bindFromRequest();
		Logger.debug("MindMap.deleteNode => mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final RemoveNodeData data = filledForm.get();
			final String nodeId = data.getNodeId();
			final F.Promise<Boolean> promise = mindMapCrudService.removeNode(source(), username(), mapId, nodeId);
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
	public Result changeEdge(final String mapId) throws JsonParseException, JsonMappingException, IOException {
		final Form<ChangeEdgeData> filledForm = changeEdgeForm.bindFromRequest();
		Logger.debug("MindMap.changeEdge => mapId=" + mapId + ", form=" + filledForm.toString());

		if (filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final ChangeEdgeData data = filledForm.get();
			final String nodeId = data.getNodeId();
			final Map<String, Object> attributeValueMap = new HashMap<String, Object>();
			
			final Map<String, String[]> formUrlEncoded = request().body().asFormUrlEncoded();
			Logger.debug((formUrlEncoded != null) + "");
			for(Map.Entry<String, String[]> entry : formUrlEncoded.entrySet()) {
				if(entry.getKey().equals("nodeId"))
					continue;
				
				final String value = entry.getValue()[0];
				Logger.debug("value: "+value+"; empty :"+value.isEmpty());
				attributeValueMap.put(entry.getKey(), value.isEmpty() ? null : value);
			}
			Logger.debug(attributeValueMap.toString());
			final F.Promise<Boolean> promise = mindMapCrudService.changeEdge(source(), username(), mapId, nodeId, attributeValueMap);
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

	public Result listenForUpdates(final String mapId) {
		return async(mindMapCrudService.listenForUpdates(source(), username(), mapId).map(new Function<Boolean, Result>() {

			@Override
			public Result apply(Boolean hasChanged) throws Throwable {
				if (hasChanged)
					return ok();
				else
					return status(NOT_MODIFIED);
			}
		}));
	}

	/**
	 * @deprecated use {@link #createNode(String)} instead
	 */
	@Security.Authenticated(Secured.class)
	@Deprecated
	public Result addNode() {
		return badRequest("Deprecated! Use " + routes.MindMap.createNode("NODE_ID").toString() + " instead");
	}

	/**
	 * 
	 * @return name of currently logged in user
	 */
	private String username() {
		final models.backend.User user = userService.getCurrentUser();
		if (user != null)
			return user.getUsername();
		else
			return null;
	}

	private String source() {
		final String source = request().getQueryString("source");
		if (source != null)
			return source;
		else
			return "unknown";
	}
}