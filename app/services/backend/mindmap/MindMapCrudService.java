package services.backend.mindmap;

import java.io.IOException;
import java.util.Map;

import models.backend.exceptions.DocearServiceException;

import org.codehaus.jackson.JsonNode;

import play.libs.F.Promise;

public interface MindMapCrudService {

	/** 
	 * Obtains a mind map as JSONString  with a specific id.
	 * @param mapId id of the map
	 * @param nodeCount soft limit of maxmium nodes to receive, -1 for unlimited
	 * @return mind map as json string
	 */
	Promise<String> mindMapAsJsonString(String mapId,Integer nodeCount) throws DocearServiceException, IOException;


	/**
	 * @deprecated redundant, use {@link #createNode(String, String, String)}
	 */
	@Deprecated
	Promise<String> addNode(JsonNode addNodeRequestJson);

	Promise<Boolean> requestLock(String mapId, String nodeId, String username);
	Promise<Boolean> releaseLock(String mapId, String nodeId, String username);
	Promise<String> fetchUpdatesSinceRevision(String mapId, Integer revision, String username);

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
	Promise<String> changeNode(String mapId, String nodeId, Map<String,Object> attributeValueMap, String username);

	Promise<Boolean> moveNodeTo(String mapId, String newParentNodeId, String nodetoMoveId, Integer newIndex);
	/**
	 * 
	 * Sends request to remove a node in a map
	 * @return true on success
	 */
	Promise<Boolean> removeNode(String mapId, String nodeId, String username);


}
