package services.backend.mindmap;

import static util.Input.resourceToString;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import models.backend.exceptions.DocearServiceException;

import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jackson.JsonNode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.libs.F.Promise;

@Profile("backendMock")
@Component
public class MockMindMapCrudService implements MindMapCrudService {

	@Override
	public Promise<String> mindMapAsJsonString(String id, Integer nodeCount) throws DocearServiceException, IOException {
        return Promise.pure(resourceToString("rest/v1/map/" + id + ".json"));
	}

	@Override
	public Promise<String> createNode(String mapId, String parentNodeId, String username) {
		try {
			Random ran = new Random();
			int id = ran.nextInt() * ran.nextInt();
			String result = "{\"id\":\"ID_" + id + "\",\"nodeText\":\"New Mock Node\"}";

			return Promise.pure(result);
		} catch (Exception e) {
			return null;
		}
	}


	@Override
	public Promise<String> getNode(String mapId, String nodeId, Integer nodeCount) {
		try {
			String result = "{\"id\":\"" + nodeId + "\",\"nodeText\":\"Mock Node\"}";

			return Promise.pure(result);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Promise<String> changeNode(String mapId, String nodeId, Map<String, Object> attributeValueMap, String username) {
		throw new NotImplementedException("https://github.com/Docear/HTW-Frontend/issues/256");
	}

	@Override
	public Promise<Boolean> removeNode(String mapId, String nodeId, String username) {
		throw new NotImplementedException("https://github.com/Docear/HTW-Frontend/issues/256");
	}

	@Override
	public Promise<Boolean> requestLock(String mapId, String nodeId, String userName) {
		throw new NotImplementedException("https://github.com/Docear/HTW-Frontend/issues/256");
	}

	@Override
	public Promise<String> fetchUpdatesSinceRevision(String mapId, Integer revision, String username) {
		throw new NotImplementedException("https://github.com/Docear/HTW-Frontend/issues/256");
	}

	@Override
	public Promise<Boolean> releaseLock(String mapId, String nodeId, String userName) {
		throw new NotImplementedException("https://github.com/Docear/HTW-Frontend/issues/256");
	}

	@Override
	public Promise<Boolean> listenForUpdates(String mapId) {
		throw new NotImplementedException("https://github.com/Docear/HTW-Frontend/issues/256");
	}
	
	@Override
	public Promise<String> addNode(JsonNode addNodeRequestJson) {
		throw new RuntimeException("Deprecated method! Use changeNode(String,String,String) instead.");
	}


}
