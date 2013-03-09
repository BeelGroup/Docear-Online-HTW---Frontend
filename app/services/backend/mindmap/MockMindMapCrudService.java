package services.backend.mindmap;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import play.Play;
import play.libs.F.Promise;

@Profile("backendMock")
@Component
public class MockMindMapCrudService extends MindMapCrudServiceBase implements MindMapCrudService {
	
	@Override
	public Promise<String> mindMapAsJsonString(String id)
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
	public Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException {
		return Promise.pure(Arrays.asList(
				new UserMindmapInfo("1", "1", "Dec 01, 2012 6:38:31 PM", "/not/available", "1.mm"),
				new UserMindmapInfo("1", "1", "Dec 01, 2012 7:38:31 PM", "/not/available", "1.mm"),
				new UserMindmapInfo("1", "1", "Dec 01, 2012 8:38:31 PM", "/not/available", "1.mm"),
				new UserMindmapInfo("2", "2", "Dec 02, 2012 6:38:31 PM", "/not/available", "2.mm"),
				new UserMindmapInfo("3", "3", "Dec 03, 2012 6:38:31 PM", "/not/available", "3.mm"),
				//new UserMindmapInfo("4", "4", "Dec 19, 2012 6:38:31 PM", "/not/available", "4.mm"),
				new UserMindmapInfo("5", "5", "Dec 05, 2012 6:38:31 PM", "/not/available", "5.mm")
				));
	}

	@Override
	public Promise<JsonNode> addNode(JsonNode addNodeRequestJson) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			Random ran = new Random();
			int id = ran.nextInt() * ran.nextInt();
			JsonNode node = mapper.readTree("{\"id\":\"ID_"+id+"\",\"nodeText\":\"New Mock Node\"}");

			return Promise.pure(node);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void ChangeNode(JsonNode changeNodeRequestJson) {
		//nothing to do here
	}

	@Override
	public void removeNode(JsonNode removeNodeRequestJson) {
		//nothing to do here

	}

}
