package controllers;

import java.io.IOException;

import models.backend.exceptions.DocearServiceException;

import org.codehaus.jackson.JsonNode;
import org.docear.messages.Messages.ChangeNodeRequest;
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

	public Result map(final String mapId, Integer nodeCount) throws DocearServiceException, IOException {
        final F.Promise<String> mindMapPromise = mindMapCrudService.mindMapAsJsonString(mapId, nodeCount);
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
    
    public Result createNode(String mapId) {
    	return TODO;
    }
    
    public Result changeNode(String mapId) {
    	final String nodeJson = request().body().asJson().toString();
    	Logger.debug("changeNode => mapId: '"+mapId+"', nodeJson:'"+nodeJson+"'");
    	
    	mindMapCrudService.ChangeNode(mapId, nodeJson);
    	return ok();
    }
    
    public Result deleteNode(String mapId) {
    	return TODO;
    }
}