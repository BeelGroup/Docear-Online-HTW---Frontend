package services.backend.mindmap;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import models.backend.User;
import models.backend.UserMindmapInfo;
import models.backend.exceptions.DocearServiceException;
import models.backend.exceptions.NoUserLoggedInException;

import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import play.libs.F.Promise;

import play.Play;

@Profile("backendMock")
@Component
public class MockMindMapCrudService extends MindMapCrudServiceBase implements MindMapCrudService {
    @Override
    public Promise<JsonNode> mindMapAsJson(String id) throws NoUserLoggedInException, IOException, DocearServiceException {
        InputStream stream = null;
        JsonNode jsonNode;
        try {
            stream = Play.application().resourceAsStream("rest/v1/map/" + id + ".json");
            if (stream == null) {
                throw new IOException("there is no map with id" + id);
            }
            ObjectMapper mapper = new ObjectMapper();
            jsonNode = mapper.readTree(stream);
        } finally {
            closeQuietly(stream);
        }
        return Promise.pure(jsonNode);
    }

    @Override
    public Promise<List<UserMindmapInfo>> getListOfMindMapsFromUser(User user) throws IOException {
        return Promise.pure(Arrays.asList(
            new UserMindmapInfo("1", "1", "Dec 01, 2012 6:38:31 PM", "/not/available", "1.mm"),
            new UserMindmapInfo("2", "2", "Dec 02, 2012 6:38:31 PM", "/not/available", "2.mm"),
            new UserMindmapInfo("3", "3", "Dec 03, 2012 6:38:31 PM", "/not/available", "3.mm"),
            //new UserMindmapInfo("4", "4", "Dec 19, 2012 6:38:31 PM", "/not/available", "4.mm"),
            new UserMindmapInfo("5", "5", "Dec 05, 2012 6:38:31 PM", "/not/available", "5.mm")
        ));
    }
}
