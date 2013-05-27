package services.backend.mindmap;

import java.io.IOException;
import java.util.Map;

import models.backend.User;
import models.backend.exceptions.DocearServiceException;
import play.libs.F.Promise;

public interface MindMapCrudService {

	/** 
	 * Obtains a mind map as JSONString  with a specific id.
	 * @param mapId id of the map
	 * @param nodeCount soft limit of maxmium nodes to receive, -1 for unlimited
	 * @return mind map as json string
	 */
	Promise<String> mindMapAsJsonString(User user, String mapId,Integer nodeCount) throws DocearServiceException, IOException;
	Promise<String> mindMapAsXmlString(User user, String mapId) throws DocearServiceException, IOException;
	

	Promise<Boolean> requestLock(User user, String mapId, String nodeId);
	Promise<Boolean> releaseLock(User user, String mapId, String nodeId);
	Promise<String> fetchUpdatesSinceRevision(User user, String mapId, Integer revision);

	/**
	 * Creates and adds a node to a map on the given parent 
	 * @param mapId
	 * @param parentNodeId
	 * @return the generated node
	 */
	Promise<String> createNode(User user, String mapId, String parentNodeId);

	/**
	 * Get Node
	 * @param mapId
	 * @param nodeId
	 * @return the node
	 */
	Promise<String> getNode(User user, String mapId, String nodeId, Integer nodeCount);


	Promise<Boolean> listenForUpdates(User user, String mapId);

	/**
	 * 
	 * @return List with updates done as json
	 */
	Promise<String> changeNode(User user, String mapId, String nodeId, Map<String,Object> attributeValueMap);

	Promise<Boolean> moveNodeTo(User user, String mapId, String newParentNodeId, String nodetoMoveId, Integer newIndex);
	/**
	 * 
	 * Sends request to remove a node in a map
	 * @return true on success
	 */
	Promise<Boolean> removeNode(User user, String mapId, String nodeId);

	Promise<Boolean> changeEdge(User user, String mapId, String nodeId, Map<String,Object> attributeValueMap);

}
