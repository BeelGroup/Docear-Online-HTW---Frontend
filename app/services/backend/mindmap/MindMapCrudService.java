package services.backend.mindmap;

import java.io.IOException;
import java.util.Map;

import models.backend.exceptions.DocearServiceException;
import play.libs.F.Promise;

public interface MindMapCrudService {

	/** 
	 * Obtains a mind map as JSONString  with a specific id.
	 * @param mapId id of the map
	 * @param nodeCount soft limit of maxmium nodes to receive, -1 for unlimited
	 * @return mind map as json string
	 */
	Promise<String> mindMapAsJsonString(String source, String username, String mapId,Integer nodeCount) throws DocearServiceException, IOException;
	Promise<String> mindMapAsXmlString(String source, String username, String mapId) throws DocearServiceException, IOException;
	

	Promise<Boolean> requestLock(String source, String username, String mapId, String nodeId);
	Promise<Boolean> releaseLock(String source, String username, String mapId, String nodeId);
	Promise<String> fetchUpdatesSinceRevision(String source, String username, String mapId, Integer revision);

	/**
	 * Creates and adds a node to a map on the given parent 
	 * @param mapId
	 * @param parentNodeId
	 * @return the generated node
	 */
	Promise<String> createNode(String source, String username, String mapId, String parentNodeId);

	/**
	 * Get Node
	 * @param mapId
	 * @param nodeId
	 * @return the node
	 */
	Promise<String> getNode(String source, String username, String mapId, String nodeId, Integer nodeCount);


	Promise<Boolean> listenForUpdates(String source, String username, String mapId);

	/**
	 * 
	 * @return List with updates done as json
	 */
	Promise<String> changeNode(String source, String username, String mapId, String nodeId, Map<String,Object> attributeValueMap);

	Promise<Boolean> moveNodeTo(String source, String username, String mapId, String newParentNodeId, String nodetoMoveId, Integer newIndex);
	/**
	 * 
	 * Sends request to remove a node in a map
	 * @return true on success
	 */
	Promise<Boolean> removeNode(String source, String username, String mapId, String nodeId);

	Promise<Boolean> changeEdge(String source, String username, String mapId, String nodeId, Map<String,Object> attributeValueMap);

}
