package services.backend.mindmap;

import models.backend.exceptions.DocearServiceException;
import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.docear.messages.Messages.MindmapAsXmlResponse;
import org.docear.messages.models.MapIdentifier;
import org.docear.messages.models.UserIdentifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import play.libs.Akka;
import play.libs.F.Promise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

import static util.Input.resourceToString;

@Profile("backendMock")
@Component
public class MockMindMapCrudService implements MindMapCrudService {

    @Override
    public Promise<Boolean> createMindmap(UserIdentifier user, MapIdentifier mapIdentifier) {
        throw new NotImplementedException("not implemented");
    }

    @Override
	public Promise<String> mindMapAsJsonString(UserIdentifier user, MapIdentifier mapIdentifier, Integer nodeCount) throws DocearServiceException, IOException {
		return Promise.pure(resourceToString("rest/v1/map/" + mapIdentifier.getMapId() + ".json"));
	}

	@Override
	public Promise<MindmapAsXmlResponse> mindMapAsXmlString(UserIdentifier user, MapIdentifier mapIdentifier) throws DocearServiceException, IOException {
		throw new NotImplementedException();
	}

	@Override
	public Promise<String> createNode(UserIdentifier user, MapIdentifier mapIdentifier, String parentNodeId) {
		try {
			Random ran = new Random();
			int id = ran.nextInt() * ran.nextInt();
			String result = "{\"id\":\"ID_" + id + "\",\"nodeText\":\"New Mock Node\"}";

			return Promise.pure(result);
		} catch (Exception e) {
			return null;
		}
	}
	
	public Promise<String> createNode(UserIdentifier user, final MapIdentifier mapIdentifier, final String parentNodeId, final String side) {
		try {
			Random ran = new Random();
			int id = ran.nextInt() * ran.nextInt();
			String result = "{\"id\":\"ID_" + id + "\",\"nodeText\":\"New Mock Node\",\"side\":\"Left\"}";

			return Promise.pure(result);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Promise<String> getNode(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId, Integer nodeCount) {
		try {
			String result = "{\"id\":\"" + nodeId + "\",\"nodeText\":\"Mock Node\"}";

			return Promise.pure(result);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Promise<String> changeNode(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId, Map<String, Object> attributeValueMap) {
		try {
			final ObjectMapper om = new ObjectMapper();
			final List<String> updates = new ArrayList<String>();
			for (Map.Entry<String, Object> entry : attributeValueMap.entrySet()) {
				final String update = "{\"type\":\"ChangeNodeAttribute\",\"nodeId\":\"" + nodeId + "\",\"attribute\":\"" + entry.getKey() + "\",\"value\":" + om.writeValueAsString(entry.getValue())
						+ "}";
				updates.add(update);
			}
			return Promise.pure(om.writeValueAsString(updates));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Promise<Boolean> moveNodeTo(UserIdentifier user, MapIdentifier mapIdentifier, String newParentNodeId, String nodeId, Integer newIndex) {
		return Promise.pure(true);
	}

	@Override
	public Promise<Boolean> removeNode(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId) {
		return Promise.pure(true);
	}

	@Override
	public Promise<Boolean> requestLock(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId) {
		return Promise.pure(true);
	}

	@Override
	public Promise<JsonNode> fetchUpdatesSinceRevision(UserIdentifier user, MapIdentifier mapIdentifier, Integer revision) {
//		final String updates = "{\"currentRevision\":"
//				+ revision
//				+ 4
//				+ ",\"orderedUpdates\":[{\"type\":\"ChangeNodeAttribute\",\"nodeId\":\"ID_1\",\"attribute\":\"locked\",\"value\":\"online-demo\"},{\"type\":\"ChangeNodeAttribute\",\"nodeId\":\"ID_1\",\"attribute\":\"folded\",\"value\":true},{\"type\":\"ChangeNodeAttribute\",\"nodeId\":\"ID_1\",\"attribute\":\"nodeText\",\"value\":\"New Text\"},{\"type\":\"ChangeNodeAttribute\",\"nodeId\":\"ID_1\",\"attribute\":\"locked\",\"value\":null}]}";
		return Promise.pure((JsonNode)new ObjectMapper().createObjectNode());
	}

	@Override
	public Promise<Boolean> releaseLock(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId) {
		return Promise.pure(true);
	}

	@Override
	public Boolean listenForUpdates(UserIdentifier user, MapIdentifier mapIdentifier) {
		Promise<Boolean> promise = Akka.future(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				Thread.sleep((long) (Math.random() * 30000));
				return true;
			}
		});

		return promise.get();
	}

	@Override
	public Promise<Boolean> changeEdge(UserIdentifier user, MapIdentifier mapIdentifier, String nodeId, Map<String, Object> attributeValueMap) {
		throw new NotImplementedException();
	}
}
