package services.backend.mindmap;

import java.io.IOException;
import java.util.List;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;

import org.codehaus.jackson.JsonNode;
import play.libs.F.Promise;

public interface MindMapCrudService {
   
    /** Obtains a mind map as JSON String with a specific id. */
    Promise<String> mindMapAsJsonString(String id) throws DocearServiceException, IOException;
    
    Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException;
    
    /**
     * Adds a node to a map on the given parent 
     * @param addNodeRequestJson {"mapId":"THE_ID","parentNodeId":"PARENT_ID"}
     * @return the generated node
     */
    Promise<JsonNode> addNode(JsonNode addNodeRequestJson);
    
    /**
     * 
     * @param changeNodeRequestJson {"mapId":"THE_ID","node":{...node object with atributes to change...}}
     */
    void ChangeNode(JsonNode changeNodeRequestJson);
    
    /**
     * 
     * @param removeNodeRequestJson {"mapId":"THE_ID","nodeId":"NODE_ID"}
     */
    void removeNode(JsonNode removeNodeRequestJson);
}
