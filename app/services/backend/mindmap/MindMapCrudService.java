package services.backend.mindmap;

import java.io.IOException;

import models.backend.exceptions.DocearServiceException;

import org.codehaus.jackson.JsonNode;
import org.docear.messages.Messages.ChangeNodeRequest;

import play.libs.F.Promise;

public interface MindMapCrudService {
     
    /** Obtains a mind map as JSONString  with a specific id. */
    Promise<String> mindMapAsJsonString(String id) throws DocearServiceException, IOException;
   ;
    
    /**
     * Adds a node to a map on the given parent 
     * @param addNodeRequestJson {"mapId":"THE_ID","parentNodeId":"PARENT_ID"}
     * @return the generated node
     */
    Promise<JsonNode> addNode(JsonNode addNodeRequestJson);
    
    /**
     * Creates and adds a node to a map on the given parent 
     * @param mapId
     * @param parentNodeId
     * @return the generated node
     */
    Promise<String> createNode(String mapId, String parentNodeId);
    
    /**
     * 
     * @param changeNodeRequestJson {"mapId":"THE_ID","node":{...node object with atributes to change...}}
     */
    void ChangeNode(String mapId, String nodeJson);
    
    /**
     * 
     * @param removeNodeRequestJson {"mapId":"THE_ID","nodeId":"NODE_ID"}
     */
    void removeNode(JsonNode removeNodeRequestJson);
}
