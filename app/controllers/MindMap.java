package controllers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import controllers.featuretoggle.ImplementedFeature;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;

import models.backend.exceptions.NoUserLoggedInException;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import akka.actor.Cancellable;
import play.libs.F;
import scala.concurrent.duration.Duration;

import play.Logger;
import play.libs.Akka;
import play.libs.Json;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import services.backend.mindmap.MindMapCrudService;

import static controllers.featuretoggle.Feature.*;

import static controllers.User.getCurrentUser;

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
    	return TODO;
    }
    
    public Result changeNode() {
    	return TODO;
    }
    
    public Result deleteNode() {
    	return TODO;
    }
}