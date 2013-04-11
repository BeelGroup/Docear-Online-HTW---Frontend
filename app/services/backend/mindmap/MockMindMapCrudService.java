package services.backend.mindmap;

import static util.Input.resourceToString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

import models.backend.exceptions.DocearServiceException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.libs.Akka;
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
		try {
			final ObjectMapper om = new ObjectMapper();
			final List<String> updates = new ArrayList<String>();
			for(Map.Entry<String, Object> entry : attributeValueMap.entrySet()) {
				final String update = "{\"type\":\"ChangeNodeAttribute\",\"nodeId\":\""+nodeId+"\",\"attribute\":\""+entry.getKey()+"\",\"value\":"+om.writeValueAsString(entry.getValue())+"}";
				updates.add(update);
			}
			return Promise.pure(om.writeValueAsString(updates));
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Promise<Boolean> removeNode(String mapId, String nodeId, String username) {
		return Promise.pure(true);
	}

	@Override
	public Promise<Boolean> requestLock(String mapId, String nodeId, String userName) {
		return Promise.pure(true);
	}

	@Override
	public Promise<String> fetchUpdatesSinceRevision(String mapId, Integer revision, String username) {
		final String updates = "{\"currentRevision\":"+revision+4+",\"orderedUpdates\":[\"{\"type\":\"ChangeNodeAttribute\",\"nodeId\":\"ID_1\",\"attribute\":\"locked\",\"value\":\"online-demo\"}\",\"{\"type\":\"ChangeNodeAttribute\",\"nodeId\":\"ID_1\",\"attribute\":\"folded\",\"value\":true}\",\"{\"type\":\"ChangeNodeAttribute\",\"nodeId\":\"ID_1\",\"attribute\":\"nodeText\",\"value\":\"New Text\"}\",\"{\"type\":\"ChangeNodeAttribute\",\"nodeId\":\"ID_1\",\"attribute\":\"locked\",\"value\":null}\"]}";
		return Promise.pure(updates);
	}

	@Override
	public Promise<Boolean> releaseLock(String mapId, String nodeId, String userName) {
		return Promise.pure(true);
	}

	@Override
	public Promise<Boolean> listenForUpdates(String mapId) {
		Promise<Boolean> promise = Akka.future(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Thread.sleep((long)(Math.random() * 30000));
				return true;
			}
		});

		return promise;
	}

	@Override
	public Promise<String> addNode(JsonNode addNodeRequestJson) {
		throw new RuntimeException("Deprecated method! Use changeNode(String,String,String) instead.");
	}


}
