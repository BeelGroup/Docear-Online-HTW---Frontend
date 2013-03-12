package controllers;

import java.io.IOException;
import java.util.Map;

import models.backend.exceptions.DocearServiceException;

import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import play.Logger;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import services.backend.mindmap.MindMapCrudService;

@Component
public class MindMap extends Controller {
	
	@Autowired
	private MindMapCrudService mindMapCrudService;

	@Security.Authenticated(Secured.class)
	public Result map(final String mapId) throws DocearServiceException, IOException {
		Logger.debug("MindMap.map <- mapId="+mapId);
        final F.Promise<String> mindMapPromise = mindMapCrudService.mindMapAsJsonString(mapId);
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
    
    @Security.Authenticated(Secured.class)
    public Result getNode(final String mapId, final String nodeId) {
    	Logger.debug("MindMap.createNode <- mapId="+mapId+", nodeId="+nodeId);
        final F.Promise<String> addNodePromise = mindMapCrudService.getNode(mapId, nodeId);
        return async(addNodePromise.map(new F.Function<String, Result>() {
            @Override
            public Result apply(String node) throws Throwable {
                return ok(node);
            }
        }));
    }
    
    @Security.Authenticated(Secured.class)
    public Result changeNode(final String mapId) {
    	Logger.debug("MindMap.changeNode <- mapId="+mapId+", body="+request().body().asText());
    	Map<String, String[]> bodyEntries = request().body().asFormUrlEncoded();
    	
    	final String nodeJson = bodyEntries.get("nodeJson")[0];
    	
    	mindMapCrudService.ChangeNode(mapId, nodeJson);
    	return ok();
    }
    
    @Security.Authenticated(Secured.class)
    public Result deleteNode(final String mapId, final String nodeId) {
    	Logger.debug("MindMap.deleteNode <- mapId="+mapId+", nodeId="+nodeId);
    	return TODO;
    }
}