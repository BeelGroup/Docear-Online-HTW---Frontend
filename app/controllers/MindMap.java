package controllers;

import java.io.IOException;
import java.util.Map;

import models.backend.exceptions.DocearServiceException;

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
	
	public Result map(final String mapId) throws DocearServiceException, IOException {
		return map(mapId, -1);
	}
	
	public Result map(final String mapId, final Integer nodeCount) throws DocearServiceException, IOException {
        final F.Promise<String> mindMapPromise = mindMapCrudService.mindMapAsJsonString(mapId, nodeCount);
		return async(mindMapPromise.map(new F.Function<String, Result>() {
            @Override
            public Result apply(String mindMap) throws Throwable {
                return ok(mindMap);
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
    
    public Result changeNode(final String mapId) {
    	Map<String, String[]> bodyEntries = request().body().asFormUrlEncoded();
    	
    	final String nodeJson = bodyEntries.get("nodeJson")[0];
    	Logger.debug("changeNode => mapId: '"+mapId+"', nodeJson:'"+nodeJson+"'");
    	
    	mindMapCrudService.ChangeNode(mapId, nodeJson);
    	return ok();
    }
    
    public Result deleteNode(final String mapId) {
    	return TODO;
    }
}