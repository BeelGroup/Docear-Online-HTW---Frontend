package services.backend.mindmap;

import java.io.IOException;
import java.util.Map;

import models.backend.exceptions.DocearServiceException;

import org.docear.messages.Messages;
import org.docear.messages.models.MapIdentifier;
import org.docear.messages.models.UserIdentifier;

import play.libs.F.Promise;

public interface MindMapCrudService {


    Promise<Boolean> createMindmap(UserIdentifier user, MapIdentifier mapIdentifier);

	/** 
	 * Obtains a mind map as JSONString  with a specific id.
	 * @param nodeCount soft limit of maxmium nodes to receive, -1 for unlimited
	 * @return mind map as json string
	 */
	Promise<String> mindMapAsJsonString(UserIdentifier user, MapIdentifier mapIdentifier,Integer nodeCount) throws DocearServiceException, IOException;
	Promise<Messages.MindmapAsXmlResponse> mindMapAsXmlString(UserIdentifier user, MapIdentifier mapIdentifier) throws DocearServiceException, IOException;
	

	Promise<Boolean> requestLock(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId);
	Promise<Boolean> releaseLock(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId);
	Promise<String> fetchUpdatesSinceRevision(UserIdentifier user, MapIdentifier mapIdentifier, Integer revision);

	/**
	 * Creates and adds a node to a map on the given parent 
	 * @param mapId
	 * @param parentNodeId
	 * @return the generated node
	 */
	Promise<String> createNode(UserIdentifier userIdentifier, MapIdentifier mapIdentifier, String parentNodeId);
	Promise<String> createNode(UserIdentifier userIdentifier, MapIdentifier mapIdentifier, String parentNodeId, String side);

	/**
	 * Get Node
	 * @param mapId
	 * @param nodeId
	 * @return the node
	 */
	Promise<String> getNode(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId, Integer nodeCount);


	Boolean listenForUpdates(UserIdentifier user, MapIdentifier mapIdentifier);

	/**
	 * 
	 * @return List with updates done as json
	 */
	Promise<String> changeNode(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId, Map<String,Object> attributeValueMap);

	Promise<Boolean> moveNodeTo(UserIdentifier user, MapIdentifier mapIdentifier, String newParentNodeId, String nodetoMoveId, Integer newIndex);
	/**
	 * 
	 * Sends request to remove a node in a map
	 * @return true on success
	 */
	Promise<Boolean> removeNode(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId);

	Promise<Boolean> changeEdge(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId, Map<String,Object> attributeValueMap);

}
