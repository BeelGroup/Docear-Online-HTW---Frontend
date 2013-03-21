package controllers;

import java.io.IOException;
import java.util.Map;

import models.backend.exceptions.DocearServiceException;
import models.frontend.ChangeNodeData;

import org.codehaus.jackson.JsonNode;
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

	@Autowired
	private MindMapCrudService mindMapCrudService;

	//cannot be secured, because we load the welcome map
	public Result map(final String mapId, final Integer nodeCount) throws DocearServiceException, IOException {
		Logger.debug("MindMap.map <- mapId="+mapId+ "; nodeCount: "+nodeCount);
		if(!mapId.equals("welcome") && !User.isAuthenticated())
			return unauthorized();
		
		
		final F.Promise<String> mindMapPromise = mindMapCrudService.mindMapAsJsonString(mapId,nodeCount);
		return async(mindMapPromise.map(new F.Function<String, Result>() {
			@Override
			public Result apply(String mindMap) throws Throwable {
				return ok(mindMap);
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result addNode() {
		Logger.debug("MindMap.addNode <- body="+request().body());
		JsonNode addNodeJSON = request().body().asJson();
		final F.Promise<JsonNode> addNodePromise = mindMapCrudService.addNode(addNodeJSON);
		return async(addNodePromise.map(new F.Function<JsonNode, Result>() {
			@Override
			public Result apply(JsonNode node) throws Throwable {
				return ok(node);
			}
		}));
	}

	@Security.Authenticated(Secured.class)
	public Result createNode(final String mapId) {
		Logger.debug("MindMap.createNode <- mapId="+mapId+", body="+request().body().asText());
		Map<String, String[]> bodyEntries = request().body().asFormUrlEncoded();

		final String parentNodeId = bodyEntries.get("parentNodeId")[0];
		final F.Promise<String> addNodePromise = mindMapCrudService.createNode(mapId, parentNodeId);
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
	public Result changeNode(final String mapId) {
		final Form<ChangeNodeData> filledForm = changeNodeForm.bindFromRequest();
		Logger.debug("MindMap.changeNode => mapId="+mapId+", form="+filledForm.toString());

		if(filledForm.hasErrors())
			return badRequest(filledForm.errorsAsJson());
		else {
			final String nodeJson = filledForm.get().getNodeJson();
			mindMapCrudService.changeNode(mapId, nodeJson);
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
}