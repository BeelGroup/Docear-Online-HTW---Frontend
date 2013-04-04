package services.backend.mindmap;

import java.io.IOException;
import java.util.Map;

import models.backend.exceptions.DocearServiceException;

import org.codehaus.jackson.JsonNode;
import org.docear.messages.exceptions.MapNotFoundException;
import org.docear.messages.exceptions.NodeNotFoundException;
import org.docear.messages.exceptions.NodeNotLockedByUserException;

import play.libs.F.Promise;

public interface MindMapCrudService {
     
    /** Obtains a mind map as JSONString  with a specific id. */
    Promise<String> mindMapAsJsonString(String id,Integer nodeCount) throws DocearServiceException, IOException;
   ;
    
    /**
     * Adds a node to a map on the given parent 
     * @param addNodeRequestJson {"mapId":"THE_ID","parentNodeId":"PARENT_ID"}
     * @return the generated node
     */
   	@Deprecated
    Promise<String> addNode(JsonNode addNodeRequestJson);
    
   	Promise<Boolean> requestLock(String mapId, String nodeId, String userName);
   	Promise<Boolean> releaseLock(String mapId, String nodeId, String userName);
   	
    /**
     * Creates and adds a node to a map on the given parent 
     * @param mapId
     * @param parentNodeId
     * @return the generated node
     */
    Promise<String> createNode(String mapId, String parentNodeId, String username);
    
    /**
     * Get Node
     * @param mapId
     * @param nodeId
     * @return the node
     */
    Promise<String> getNode(String mapId, String nodeId, Integer nodeCount);
    
    
    Promise<Boolean> listenForUpdates(String mapId);
    
    /**
     * 
     * @return List with updates done as json
     */
    Promise<String> changeNode(String mapId, String nodeId, Map<String,Object> attributeValueMap, String username)
    	throws MapNotFoundException, NodeNotLockedByUserException, NodeNotFoundException;
    
    /**
     * 
     * @param removeNodeRequestJson {"mapId":"THE_ID","nodeId":"NODE_ID"}
     */
    void removeNode(String mapId, String nodeId, String username);
    
    
}
