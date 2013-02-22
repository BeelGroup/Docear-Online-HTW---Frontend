package controllers;

import static controllers.User.getCurrentUser;

import java.io.IOException;
import java.util.List;

import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;

import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.MinMaxPriorityQueue;

import play.Logger;
import play.libs.F;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import services.backend.mindmap.MindMapCrudService;

@Component
public class MindMap extends Controller {
	
	@Autowired
	private MindMapCrudService mindMapCrudService;

	public Result map(final String id) throws DocearServiceException, IOException {
		
        final F.Promise<JsonNode> mindMapPromise = mindMapCrudService.mindMapAsJson(id);
        return async(mindMapPromise.map(new F.Function<JsonNode, Result>() {
            @Override
            public Result apply(JsonNode mindMap) throws Throwable {
                return ok(mindMap);
            }
        }));
	}

    @Security.Authenticated(Secured.class)
	public Result mapListFromDB() throws IOException, DocearServiceException {
        final Promise<List<UserMindmapInfo>> listOfMindMapsFromUser = mindMapCrudService.getListOfMindMapsFromUser(getCurrentUser());
        return async(listOfMindMapsFromUser.map(new F.Function<List<UserMindmapInfo>, Result>() {
            @Override
            public Result apply(List<UserMindmapInfo> maps) throws Throwable {
                return ok(Json.toJson(maps));
            }
        }));
    }
    
    public Result createNode() {
    	JsonNode addNodeRequestJson = request().body().asJson();
    	Logger.debug("body content: "+addNodeRequestJson);
    	final Promise<JsonNode> newNode = mindMapCrudService.addNode(addNodeRequestJson);
    	return async(newNode.map(new Function<JsonNode, Result>() {
			@Override
			public Result apply(JsonNode nodeJson) throws Throwable {
				return ok(nodeJson);
			}
		}));
    }
    
    public Result changeNode() {
    	JsonNode changeNodeRequestJson = request().body().asJson();
    	mindMapCrudService.ChangeNode(changeNodeRequestJson);
    	return ok();
    }
    
    public Result deleteNode() {
    	JsonNode deleteNodeRequestJson = request().body().asJson();
    	mindMapCrudService.removeNode(deleteNodeRequestJson);
    	return ok();
    }   

}