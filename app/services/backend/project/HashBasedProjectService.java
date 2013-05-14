package services.backend.project;

import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import play.libs.F;

import java.io.InputStream;

@Profile("projectHashImpl")
@Component
public class HashBasedProjectService implements ProjectService {

    @Autowired
    private ProjectService projectService;

    @Override
    public F.Promise<InputStream> getFile(Long projectId, String path) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }

    @Override
    public F.Promise<JsonNode> metadata(Long projectId, String path) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }

    @Override
    public F.Promise<JsonNode> createFolder(Long projectId, String path) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }

    @Override
    public F.Promise<JsonNode> putFile(Long projectId, String path, byte[] content) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }

    @Override
    public F.Promise<Boolean> listenIfUpdateOccurs(Long projectId) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }

    @Override
    public F.Promise<String> versionDelta(Long projectId, String cursor) {
        throw new NotImplementedException("see https://github.com/Docear/HTW-Frontend/issues?labels=workspace-sync&milestone=&page=1&state=open");
    }
}
