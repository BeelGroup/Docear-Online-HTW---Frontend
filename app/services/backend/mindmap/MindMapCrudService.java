package services.backend.mindmap;

import java.io.IOException;
import java.util.List;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;

import org.codehaus.jackson.JsonNode;
import play.libs.F.Promise;

public interface MindMapCrudService {

    /** Obtains a mind map as JSON with a specific id. */
    Promise<JsonNode> mindMapAsJson(String id) throws DocearServiceException, IOException;

    Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException;
}
