package services.backend.mindmap;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

import models.backend.exceptions.DocearServiceException;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.docear.messages.exceptions.MapNotFoundException;
import org.docear.messages.exceptions.NodeNotFoundException;
import org.docear.messages.exceptions.NodeNotLockedByUserException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.Play;
import play.libs.F.Promise;

@Profile("backendMock")
@Component
public class MockMindMapCrudService implements MindMapCrudService {

	@Override
	public Promise<String> mindMapAsJsonString(String id, Integer nodeCount)
			throws DocearServiceException, IOException {
		String result = null;

		try {
			result = FileUtils.readFileToString(new File(Play.application().path()+"/conf/rest/v1/map/" + id + ".json"));
			if (result == null) {
				throw new IOException("there is no map with id" + id);
			}

		} finally {
		}
		return Promise.pure(result);
	}

	@Override
	public Promise<String> createNode(String mapId, String parentNodeId, String username) {
		try {
			Random ran = new Random();
			int id = ran.nextInt() * ran.nextInt();
			String result = "{\"id\":\"ID_"+id+"\",\"nodeText\":\"New Mock Node\"}";

			return Promise.pure(result);
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unused")
	@Override
	public Promise<String> addNode(JsonNode addNodeRequestJson) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Random ran = new Random();
			int id = ran.nextInt() * ran.nextInt();
			JsonNode node = mapper.readTree("{\"id\":\"ID_"+id+"\",\"nodeText\":\"New Mock Node\"}");

			//TODO mock
			return Promise.pure("");// Promise.pure(node);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Promise<String> getNode(String mapId, String nodeId, Integer nodeCount) {
		try {
			String result = "{\"id\":\""+nodeId+"\",\"nodeText\":\"Mock Node\"}";

			return Promise.pure(result);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Promise<String> changeNode(String mapId, String nodeId,
			Map<String, Object> attributeValueMap, String username)
					throws MapNotFoundException, NodeNotLockedByUserException,
					NodeNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeNode(String mapId, String nodeId, String username) {
		// TODO Auto-generated method stub

	}

	@Override
	public Promise<Boolean> requestLock(String mapId, String nodeId,
			String userName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Promise<Boolean> releaseLock(String mapId, String nodeId,
			String userName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Promise<Boolean> listenForUpdates(String mapId) {
		// TODO Auto-generated method stub
		return null;
	}

}
