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
import services.backend.mindmap.MindMapCrudService;

@Component
public class MindMap extends Controller {
	
	@Autowired
	private MindMapCrudService mindMapCrudService;

	public Result map(final String id) throws DocearServiceException, IOException {
		
        final F.Promise<String> mindMapPromise = mindMapCrudService.mindMapAsJsonString(id);
		return async(mindMapPromise.map(new F.Function<String, Result>() {
            @Override
            public Result apply(String mindMap) throws Throwable {
                return ok(mindMap);
            }
        }));
	}
    
    public Result addNode() {
    	JsonNode addNodeJSON = request().body().asJson();
        final F.Promise<JsonNode> addNodePromise = mindMapCrudService.addNode(addNodeJSON);
        return async(addNodePromise.map(new F.Function<JsonNode, Result>() {
            @Override
            public Result apply(JsonNode node) throws Throwable {
                return ok(node);
            }
        }));
    }
    
    public Result createNode(final String mapId) {
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
    
    public Result getNode(final String mapId, final String nodeId) {
        final F.Promise<String> addNodePromise = mindMapCrudService.getNode(mapId, nodeId);
        return async(addNodePromise.map(new F.Function<String, Result>() {
            @Override
            public Result apply(String node) throws Throwable {
                return ok(node);
            }
        }));
    }
    
    public Result changeNode(final String mapId) {
    	Map<String, String[]> bodyEntries = request().body().asFormUrlEncoded();
    	
    	final String nodeJson = bodyEntries.get("nodeJson")[0];
    	Logger.debug("changeNode => mapId: '"+mapId+"', nodeJson:'"+nodeJson+"'");
    	
    	mindMapCrudService.ChangeNode(mapId, nodeJson);
    	return ok();
    }
    
    public Result deleteNode(String mapId) {
    	return TODO;
    }
}