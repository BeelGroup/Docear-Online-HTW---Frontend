package controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import models.backend.exceptions.DocearServiceException;
import models.frontend.formdata.ChangeNodeData;
import models.frontend.formdata.ReleaseLockData;
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

@Component
public class MindMap extends Controller {
	private final static Form<ChangeNodeData> changeNodeForm = Form.form(ChangeNodeData.class);
	private final static Form<RequestLockData> requestLockForm = Form.form(RequestLockData.class);
	private final static Form<ReleaseLockData> releaseLockForm = Form.form(ReleaseLockData.class);

	@Autowired
	private MindMapCrudService mindMapCrudService;

	//cannot be secured, because we load the welcome map
	public Result map(final String mapId, final Integer nodeCount) throws DocearServiceException, IOException {
		Logger.debug("MindMap.map <- mapId="+mapId+ "; nodeCount: "+nodeCount);
		//check if welcome map or user authenticated
		if(!mapId.equals("welcome") && !User.isAuthenticated())
			return redirect(routes.Application.index());


		final F.Promise<String> mindMapPromise = mindMapCrudService.mindMapAsJsonString(mapId,nodeCount);
		return async(mindMapPromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String mindMap) throws Throwable {
				return ok(mindMap);
			}
		}));
	}

	public Result requestLock(final String mapId) {
		final Form<RequestLockData> filledForm = requestLockForm.bindFromRequest();
		Logger.debug("MindMap.requestLock => mapId="+mapId+", form="+filledForm.toString());

		if(filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final RequestLockData data = filledForm.get();
			final String nodeId = data.getNodeId();
			final F.Promise<Boolean> promise = mindMapCrudService.requestLock(mapId, nodeId, username());
			return async(promise.map(new Function<Boolean, Result>() {

				@Override
				public Result apply(Boolean success) throws Throwable {
					if(success) {
						return ok();
					} else {
						return forbidden();
					}
				}
			}));	
		}
	}

	public Result releaseLock(final String mapId) {
		final Form<ReleaseLockData> filledForm = releaseLockForm.bindFromRequest();
		Logger.debug("MindMap.requestLock => mapId="+mapId+", form="+filledForm.toString());

		if(filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final ReleaseLockData data = filledForm.get();
			final String nodeId = data.getNodeId();
			final F.Promise<Boolean> promise = mindMapCrudService.releaseLock(mapId, nodeId, username());
			return async(promise.map(new Function<Boolean, Result>() {

				@Override
				public Result apply(Boolean success) throws Throwable {
					if(success) {
						return ok();
					} else {
						return forbidden();
					}
				}
			}));	
		}
	}
	
	@Security.Authenticated(Secured.class)
	public Result fetchUpdatesSinceRevision(String mapId, Integer revision) {
		Logger.debug("MindMap.fetchUpdatesSinceRevision <- mapId="+mapId+ "; revision: "+revision);

		final F.Promise<String> updatePromise = mindMapCrudService.fetchUpdatesSinceRevision(mapId, revision, username());
		return async(updatePromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String updates) throws Throwable {
				return ok(updates);
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result createNode(final String mapId) {
		Logger.debug("MindMap.createNode <- mapId="+mapId+", body="+request().body().asText());
		Map<String, String[]> bodyEntries = request().body().asFormUrlEncoded();

		final String parentNodeId = bodyEntries.get("parentNodeId")[0];
		final F.Promise<String> addNodePromise = mindMapCrudService.createNode(mapId, parentNodeId, username());
		return async(addNodePromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String node) throws Throwable {
				return ok(node);
			}
		}));
	}

	public Result getNode(final String mapId, final String nodeId, final Integer nodeCount) {
		Logger.debug("MindMap.createNode <- mapId="+mapId+", nodeId="+nodeId+", nodeCount= "+nodeCount);
		if(!mapId.equals("welcome") && !User.isAuthenticated())
			return unauthorized();
		final F.Promise<String> addNodePromise = mindMapCrudService.getNode(mapId, nodeId,nodeCount);
		return async(addNodePromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String node) throws Throwable {
				return ok(node);
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result changeNode(final String mapId) throws JsonParseException, JsonMappingException, IOException {
		final Form<ChangeNodeData> filledForm = changeNodeForm.bindFromRequest();
		Logger.debug("MindMap.changeNode => mapId="+mapId+", form="+filledForm.toString());

		if(filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final ChangeNodeData data = filledForm.get();
			final String nodeId = data.getNodeId();
			final TypeReference<HashMap<String,Object>> typeRef 
	          = new TypeReference<HashMap<String,Object>>() {}; 
			final Map<String, Object> map = new ObjectMapper().readValue(data.getAttributeValueMapJson(), typeRef);
			mindMapCrudService.changeNode(mapId, nodeId,map, username());
			return ok();	
		}
	}

	@Security.Authenticated(Secured.class)
	public Result deleteNode(final String mapId, final String nodeId) {
		Logger.debug("MindMap.deleteNode <- mapId="+mapId+", nodeId="+nodeId);
		return TODO;
	}

	public Result listenForUpdates(final String mapId) {
		return async(mindMapCrudService.listenForUpdates(mapId).map(new Function<Boolean, Result>() {

			@Override
			public Result apply(Boolean arg0) throws Throwable {
				if(arg0)
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
		return badRequest("Deprecated! Use "+routes.MindMap.createNode("NODE_ID").toString()+" instead");
	}
	
	/**
	 * 
	 * @return name of currently logged in user
	 */
	private String username() {
		return controllers.User.getCurrentUser().getUsername();
	}
}